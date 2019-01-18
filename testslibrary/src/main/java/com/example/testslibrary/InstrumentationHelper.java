package com.example.testslibrary;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class InstrumentationHelper {
    private static final String TAG = "InstrumentationHelper";
    public static void runTests(Context context, String testClassesPath, JSONObject json) {

        //Get all available instrumentations
        final String packageName = context.getPackageName();
        final List<InstrumentationInfo> list =
                context.getPackageManager().queryInstrumentation(packageName, PackageManager.GET_META_DATA);

        Log.i(TAG,"Instrumentations size: " + list.size());
        if (list.isEmpty()) {
            Toast.makeText(context, "Cannot find instrumentation for " + packageName,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        InstrumentationInfo instrumentationInfoLibrary=null;
        for(InstrumentationInfo instrumentationInfo: list)
        {
            if(instrumentationInfo.name.equals("com.example.testslibrary.AndroidJUnitRunner"))
            {
                instrumentationInfoLibrary =  instrumentationInfo;
            }
        }

        if(instrumentationInfoLibrary==null)
        {
            Toast.makeText(context, "Cannot find library instrumentation",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Log.i(TAG, "data pack: " + instrumentationInfoLibrary.packageName + " name: " + instrumentationInfoLibrary.name);
        final ComponentName componentName =
                new ComponentName(instrumentationInfoLibrary.packageName,
                        instrumentationInfoLibrary.name);


        //In the bundle send what is your loader and send which test class#method you want to start
        Bundle arguments = new Bundle();
        arguments.putString("loaderPath", testClassesPath);
        JSONArray testArr=null;
        try {
            testArr = json.getJSONArray("testClasses");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //Add all tests classes for that configuration to be executed
        String testsStr = "";
        if(testArr!=null && testArr.length()>0)
        {
            for(int i=0;i<testArr.length();i++)
            {
                try {
                    Log.i(TAG,"class added: " + testArr.getString(i));
                    testsStr+= "," + testArr.getString(i);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        if(!testsStr.equals("")) {
            arguments.putString("class", testsStr.substring(1));
        }

        //Start the instrumentation. THE RESULT of the test will be available at TestListener class
        if (!context.startInstrumentation(componentName, null, arguments)) {
            Toast.makeText(context, "Cannot run instrumentation for " + packageName,
                    Toast.LENGTH_SHORT).show();
        }
    }
}
