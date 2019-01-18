package com.example.testslibrary;

import android.os.AsyncTask;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

public class SendTestsReport extends AsyncTask<String, String, Void> {

    @Override
    protected Void doInBackground(String... params) {
        String url = params[0];
        String report = params[1];
        String config = params[2];

        sendReport(url,report,config);

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        Log.i("SendTestsReport","report sent");
        super.onPostExecute(aVoid);
    }


    /**
     *
     * @param url This is the url where we are sending the report
     * @param report This string contains the json report of the tests result
     * @param config This string contains the json representation of the probed phone configuration
     */
    public void sendReport(String url, String report, String config) {
        HttpURLConnection c = null;

        //Insert the report and the configuration as a POST parameters
        //TODO modify this approach by sending these parameters as files
        Map<String,Object> params = new LinkedHashMap<>();
        params.put("report", report);
        params.put("config", config);

        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String,Object> param : params.entrySet()) {
            if (postData.length() != 0) postData.append('&');
            try {
                postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            postData.append('=');
            try {
                postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }



        // Log.i("SendTestsReport","urlParameters: " + urlParameters);
        byte[] postDataBytes = postData.toString().getBytes();
        int    postDataLength = postDataBytes.length;

        try {
            URL u = new URL(url);
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded");
            c.setRequestProperty( "Content-Length", Integer.toString( postDataLength ));

            //Send the data here
            c.getOutputStream().write(postDataBytes);

            //Get the response from the server
            //TODO The actual response is not important
            Reader in = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));

            StringBuilder sb = new StringBuilder();
            for (int ch; (ch = in.read()) >= 0;)
                sb.append((char)ch);
            String response = sb.toString();
            Log.i("SendTestsReport","Response: " + response);

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}