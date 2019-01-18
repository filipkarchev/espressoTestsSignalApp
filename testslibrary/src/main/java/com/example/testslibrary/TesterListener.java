package com.example.testslibrary;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import junit.framework.AssertionFailedError;

import org.json.JSONArray;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.util.ArrayList;

public class TesterListener extends RunListener {
    private final String LOG_CAT = "TesterListener";
    ArrayList<TestResult> results = new ArrayList<>();
    Context context = null;


    public TesterListener(Context context) {
        this.context = context;
    }

    /**
     * This default constructor is required from the system
     */
    public TesterListener() {
    }

    @Override
    public void testFinished(Description description) throws Exception {
        Log.i(LOG_CAT, "testFinished class: " + description.getClassName() + " method: " + description.getMethodName());
        super.testFinished(description);
    }


    @Override
    public void testStarted(Description description) throws Exception {
        Log.i(LOG_CAT, "testStarted");
        //Add every new test in the list
        TestResult newResult = new TestResult();
        newResult.setClassName(description.getClassName());
        newResult.setMethodName(description.getMethodName());
        results.add(newResult);

        super.testStarted(description);
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        Log.e(LOG_CAT, "testFailure class: " + failure.getDescription().getClassName() + " method: " + failure.getDescription().getMethodName() + " message: " + failure.getMessage() + " toString:" + failure.toString());
        TestResult newResult = new TestResult();
        newResult.setClassName(failure.getDescription().getClassName());
        newResult.setMethodName(failure.getDescription().getMethodName());
        newResult.setErrorMessage(failure.getMessage());

        //Get is it from crash or assertion
        if (failure.getException() instanceof AssertionFailedError) {
            newResult.setResultStatus(TestResultType.ASSERTION_FAIL);
        } else {
            newResult.setResultStatus(TestResultType.ERROR);
        }

        if (results.contains(newResult)) {
            //Update this test information
            int index = results.indexOf(newResult);
            results.set(index, newResult);
        }
        super.testFailure(failure);
    }

    @Override
    public void testRunStarted(Description description) throws Exception {
        Log.e(LOG_CAT, "testRunStarted tests count: " + description.testCount() + " " + description.getClassName());
        super.testRunStarted(description);
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        Log.e(LOG_CAT, "testRunFinished runs: " + result.getRunCount() + " fails: " + result.getFailureCount());
        super.testRunFinished(result);

        //Create a json report with all the tests that were executed
        JSONArray report = new JSONArray();
        for (int i = 0; i < results.size(); i++) {
            TestResult testResult = results.get(i);
            report.put(i, testResult.convertToJsonObject());
        }

        // Clear the tests list for the next test execution
        results.clear();

        //After the tests are finished we can send the results to the server
        new SendTestsReport().execute(Constants.reportUrl, report.toString(), new Configuration().getConf_representation());


        //Relaunch the application
        //TODO Some information could be passed here to the application so we can handle the results of the tests when the app runs
        //TODO Get the package name dynamically
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage("org.thoughtcrime.securesms");
        if (launchIntent != null) {
            context.startActivity(launchIntent);//null pointer check in case package name was not found
        }

        PendingIntent pend = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                launchIntent.getFlags());

        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis(), pend);


    }
}
