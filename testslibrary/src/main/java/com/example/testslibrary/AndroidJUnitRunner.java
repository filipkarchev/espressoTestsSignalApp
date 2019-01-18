package com.example.testslibrary;

import static android.support.test.internal.util.ReflectionUtil.reflectivelyInvokeRemoteMethod;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.support.annotation.VisibleForTesting;
import android.support.test.filters.LargeTest;
import android.support.test.filters.MediumTest;
import android.support.test.filters.SmallTest;
import android.support.test.internal.runner.RunnerArgs;
import android.support.test.internal.runner.TestExecutor;
import android.support.test.internal.runner.TestRequestBuilder;
import android.support.test.internal.runner.listener.ActivityFinisherRunListener;
import android.support.test.internal.runner.listener.CoverageListener;
import android.support.test.internal.runner.listener.DelayInjector;
import android.support.test.internal.runner.listener.InstrumentationResultPrinter;
import android.support.test.internal.runner.listener.LogRunListener;
import android.support.test.internal.runner.listener.SuiteAssignmentPrinter;
import android.support.test.internal.runner.tracker.AnalyticsBasedUsageTracker;
import android.support.test.internal.runner.tracker.UsageTrackerRegistry.AtslVersions;
import android.support.test.orchestrator.instrumentationlistener.OrchestratedInstrumentationListener;
import android.support.test.orchestrator.instrumentationlistener.OrchestratedInstrumentationListener.OnConnectListener;
import android.support.test.runner.MonitoringInstrumentation;
import android.support.test.runner.UsageTrackerFacilitator;
import android.support.test.runner.lifecycle.ApplicationLifecycleCallback;
import android.support.test.runner.lifecycle.ApplicationLifecycleMonitorRegistry;
import android.support.test.runner.screenshot.ScreenCaptureProcessor;
import android.support.test.runner.screenshot.Screenshot;
import android.util.Log;
import java.util.HashSet;
import org.junit.runner.Request;
import dalvik.system.DexClassLoader;

/**
 *                                      IMPORTANT !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 *  This class contains the source code of the standard AndroidJUnitRunner obtained from 'com.android.support.test.espresso:espresso-core:3.0.2'
 *  Two Main Changes are inserted inside in order to make it run Instrumentation tests dynamically on the users Phone:
 *  1. In the method buildRequest, we are creating new Class loader with the provided path, which is set as a class loader for the created request
 *  2. In the addListenersFromArg method, we are setting our custom lister TesterLister, which receives Context parameter that will be later used to relaunch the application
 */

public class AndroidJUnitRunner extends MonitoringInstrumentation implements OnConnectListener {

    private static final String LOG_TAG = "AndroidJUnitRunner";

    private Bundle mArguments;
    private InstrumentationResultPrinter mInstrumentationResultPrinter =
            new InstrumentationResultPrinter();
    private RunnerArgs mRunnerArgs;
    private UsageTrackerFacilitator mUsageTrackerFacilitator;
    private OrchestratedInstrumentationListener mOrchestratorListener;

    @Override
    public void onCreate(Bundle arguments) {
        mArguments = arguments;
        parseRunnerArgs(mArguments);

        if (mRunnerArgs.debug) {
            Log.i(LOG_TAG, "Waiting for debugger to connect...");
            Debug.waitForDebugger();
            Log.i(LOG_TAG, "Debugger connected.");
        }

        // We are only interested in tracking usage of the primary process.
        if (isPrimaryInstrProcess(mRunnerArgs.targetProcess)) {
            mUsageTrackerFacilitator = new UsageTrackerFacilitator(mRunnerArgs);
        } else {
            mUsageTrackerFacilitator = new UsageTrackerFacilitator(false);
        }

        super.onCreate(arguments);

        for (ApplicationLifecycleCallback listener : mRunnerArgs.appListeners) {
            ApplicationLifecycleMonitorRegistry.getInstance().addLifecycleCallback(listener);
        }

        addScreenCaptureProcessors(mRunnerArgs);

        if (mRunnerArgs.orchestratorService != null
                && isPrimaryInstrProcess(mRunnerArgs.targetProcess)) {
            // If orchestratorService is provided, and we are the primary process
            // we await onOrchestratorConnect() before we start().
            mOrchestratorListener = new OrchestratedInstrumentationListener(this);
            mOrchestratorListener.connect(getContext());
        } else {
            // If no orchestration service is given, or we are not the primary process we can
            // start() immediately.
            start();
        }
    }

    /**
     * Called when AndroidJUnitRunner connects to a test orchestrator, if the {@code
     * orchestratorService} parameter is set.
     *
     * @hide
     */
    @Override
    public void onOrchestratorConnect() {
        start();
    }

    /**
     * Build the arguments.
     *
     * <p>Read from manifest first so manifest-provided args can be overridden with command line
     * arguments
     *
     * @param arguments
     */
    private void parseRunnerArgs(Bundle arguments) {
        mRunnerArgs = new RunnerArgs.Builder().fromManifest(this).fromBundle(arguments).build();
    }

    /**
     * Get the Bundle object that contains the arguments passed to the instrumentation
     *
     * @return the Bundle object
     */
    private Bundle getArguments() {
        return mArguments;
    }

    @VisibleForTesting
    InstrumentationResultPrinter getInstrumentationResultPrinter() {
        return mInstrumentationResultPrinter;
    }

    @Override
    public void onStart() {
        setJsBridgeClassName("android.support.test.espresso.web.bridge.JavaScriptBridge");
        super.onStart();

        /*
         * The orchestrator cannot collect the list of tests as it is running in a different process
         * than the test app.  On first run, the Orchestrator will ask AJUR to list the tests
         * out that would be run for a given class parameter.  AJUR will then be successively
         * called with whatever it passes back to the orchestratorListener.
         */
        if (mRunnerArgs.listTestsForOrchestrator && isPrimaryInstrProcess(mRunnerArgs.targetProcess)) {
            Request testRequest = buildRequest(mRunnerArgs, getArguments());
            mOrchestratorListener.addTests(testRequest.getRunner().getDescription());
            finish(Activity.RESULT_OK, new Bundle());
            return;
        }

        if (mRunnerArgs.remoteMethod != null) {
            reflectivelyInvokeRemoteMethod(
                    mRunnerArgs.remoteMethod.testClassName, mRunnerArgs.remoteMethod.methodName);
        }

        if (!isPrimaryInstrProcess(mRunnerArgs.targetProcess)) {
            Log.i(LOG_TAG, "Runner is idle...");
            return;
        }

        Bundle results = new Bundle();
        try {
            TestExecutor.Builder executorBuilder = new TestExecutor.Builder(this);

            addListeners(mRunnerArgs, executorBuilder);

            Request testRequest = buildRequest(mRunnerArgs, getArguments());

            results = executorBuilder.build().execute(testRequest);

        } catch (RuntimeException e) {
            final String msg = "Fatal exception when running tests";
            Log.e(LOG_TAG, msg, e);
            // report the exception to instrumentation out
            results.putString(
                    Instrumentation.REPORT_KEY_STREAMRESULT, msg + "\n" + Log.getStackTraceString(e));
        }
        finish(Activity.RESULT_OK, results);
    }

    @Override
    public void finish(int resultCode, Bundle results) {
        try {
            mUsageTrackerFacilitator.trackUsage("AndroidJUnitRunner", AtslVersions.RUNNER_VERSION);
            mUsageTrackerFacilitator.sendUsages();
        } catch (RuntimeException re) {
            Log.w(LOG_TAG, "Failed to send analytics.", re);
        }
        super.finish(resultCode, results);
    }

    @VisibleForTesting
    final void addListeners(RunnerArgs args, TestExecutor.Builder builder) {
        if (args.newRunListenerMode) {
            addListenersNewOrder(args, builder);
        } else {
            addListenersLegacyOrder(args, builder);
        }
    }

    private void addListenersLegacyOrder(RunnerArgs args, TestExecutor.Builder builder) {
        if (args.logOnly) {
            // Only add the listener that will report the list of tests when running in logOnly
            // mode.
            builder.addRunListener(getInstrumentationResultPrinter());
        } else if (args.suiteAssignment) {
            builder.addRunListener(new SuiteAssignmentPrinter());
        } else {
            builder.addRunListener(new LogRunListener());
            if (mOrchestratorListener != null) {
                builder.addRunListener(mOrchestratorListener);
            } else {
                builder.addRunListener(getInstrumentationResultPrinter());
            }
            builder.addRunListener(
                    new ActivityFinisherRunListener(
                            this,
                            new MonitoringInstrumentation.ActivityFinisher(),
                            new Runnable() {
                                // Yes, this is terrible and weird but avoids adding a new public API
                                // outside the internal package.
                                @Override
                                public void run() {
                                    waitForActivitiesToComplete();
                                }
                            }));
            addDelayListener(args, builder);
            addCoverageListener(args, builder);
        }
        addListenersFromArg(args, builder);
    }

    private void addListenersNewOrder(RunnerArgs args, TestExecutor.Builder builder) {
        // User defined listeners go first, to guarantee running before InstrumentationResultPrinter
        // and ActivityFinisherRunListener. Delay and Coverage Listener are also moved before for the
        // same reason.
        addListenersFromArg(args, builder);
        if (args.logOnly) {
            // Only add the listener that will report the list of tests when running in logOnly
            // mode.
            builder.addRunListener(getInstrumentationResultPrinter());
        } else if (args.suiteAssignment) {
            builder.addRunListener(new SuiteAssignmentPrinter());
        } else {
            builder.addRunListener(new LogRunListener());
            addDelayListener(args, builder);
            addCoverageListener(args, builder);
            if (mOrchestratorListener != null) {
                builder.addRunListener(mOrchestratorListener);
            } else {
                builder.addRunListener(getInstrumentationResultPrinter());
            }
            builder.addRunListener(
                    new ActivityFinisherRunListener(
                            this,
                            new MonitoringInstrumentation.ActivityFinisher(),
                            new Runnable() {
                                // Yes, this is terrible and weird but avoids adding a new public API
                                // outside the internal package.
                                @Override
                                public void run() {
                                    waitForActivitiesToComplete();
                                }
                            }));
        }
    }

    private void addScreenCaptureProcessors(RunnerArgs args) {
        Screenshot.addScreenCaptureProcessors(
                new HashSet<ScreenCaptureProcessor>(args.screenCaptureProcessors));
    }

    private void addCoverageListener(RunnerArgs args, TestExecutor.Builder builder) {
        if (args.codeCoverage) {
            builder.addRunListener(new CoverageListener(args.codeCoveragePath));
        }
    }

    /** Sets up listener to inject a delay between each test, if specified. */
    private void addDelayListener(RunnerArgs args, TestExecutor.Builder builder) {
        if (args.delayInMillis > 0) {
            builder.addRunListener(new DelayInjector(args.delayInMillis));
        } else if (args.logOnly && Build.VERSION.SDK_INT < 16) {
            // On older platforms, collecting tests can fail for large volume of tests.
            // Insert a small delay between each test to prevent this
            builder.addRunListener(new DelayInjector(15 /* msec */));
        }
    }

    private void addListenersFromArg(RunnerArgs args, TestExecutor.Builder builder) {
        builder.addRunListener(new TesterListener(getContext()));
//        for (RunListener listener : args.listeners) {
//            builder.addRunListener(listener);
//        }
    }

    @Override
    public boolean onException(Object obj, Throwable e) {
        InstrumentationResultPrinter instResultPrinter = getInstrumentationResultPrinter();
        if (instResultPrinter != null) {
            // report better error message back to Instrumentation results.
            instResultPrinter.reportProcessCrash(e);
        }
        return super.onException(obj, e);
    }

    /** Builds a {@link Request} based on given input arguments. */
    @VisibleForTesting
    Request buildRequest(RunnerArgs runnerArgs, Bundle bundleArgs) {

        TestRequestBuilder builder = createTestRequestBuilder(this, bundleArgs);

        Log.i("AndroidJUnitRunner","bundle loader is: "+bundleArgs.getClassLoader());

        if(bundleArgs!=null && bundleArgs.getString("loaderPath")!=null)
        {
            String loaderPath = bundleArgs.getString("loaderPath");
            Log.i("AndroidJUnitRunner","loaderPath is: " + loaderPath);
            DexClassLoader loader = new DexClassLoader(loaderPath,"",null, getClass().getClassLoader());

            builder.setClassLoader(loader);
        }


        builder.addPathsToScan(runnerArgs.classpathToScan);
        if (runnerArgs.classpathToScan.isEmpty()) {
            // Only scan for tests for current apk aka testContext
            // Note that this represents a change from InstrumentationTestRunner where
            // getTargetContext().getPackageCodePath() aka app under test was also scanned
            // Only add the package classpath when no custom classpath is provided in order to
            // avoid duplicate class issues.
            builder.addPathToScan(getContext().getPackageCodePath());
        }
        builder.addFromRunnerArgs(runnerArgs);

        registerUserTracker();

        return builder.build();
    }

    private void registerUserTracker() {
        Context targetContext = getTargetContext();
        if (targetContext != null) {
            mUsageTrackerFacilitator.registerUsageTracker(
                    new AnalyticsBasedUsageTracker.Builder(targetContext).buildIfPossible());
        }
    }

    /** Factory method for {@link TestRequestBuilder}. */
    TestRequestBuilder createTestRequestBuilder(Instrumentation instr, Bundle arguments) {
        return new TestRequestBuilder(instr, arguments);
    }
}
