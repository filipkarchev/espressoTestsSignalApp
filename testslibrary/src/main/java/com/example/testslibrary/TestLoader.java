package com.example.testslibrary;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class TestLoader {
    private Context context;
    //The name of the file that is downloaded. The name is not relevant, since it is only used on the FLY
    final String testClassesName = "TestClasses.dex";

    public TestLoader(Context ctx) {
        this.context = ctx;
    }


    /**
     * This Method triggers the whole procedure of running Instrumentation tests dynamically on the users device.
     * @param jsonConfiguration This Obtained Phone configuration presented in json format. Note that the order in the json is important and should be the same every time!!!
     */
    public void startTesting(String jsonConfiguration) {
        //Save the configuration string in a singleton object
        new Configuration().setConf_representation(jsonConfiguration);

        //Run task to get the DEX file contains the test files
        new DownloadFileFromURL().execute(Constants.fileUrl, jsonConfiguration);

        //Run task to get which test will be executed for the configuration
        new DownloadJsonFromURL().execute(Constants.jsonUrl, jsonConfiguration);
    }


    class DownloadFileFromURL extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... f_url) {
            int count;
            HttpURLConnection c = null;

            //Prepare to send the json String representing the configuration as a POST parameter
            String urlParameters = null;
            try {
                urlParameters = "config=" + URLEncoder.encode(f_url[1], "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            byte[] postData = urlParameters.getBytes();
            int postDataLength = postData.length;

            try {
                URL url = new URL(f_url[0]);
                c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("POST");
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                c.setRequestProperty("Content-Length", Integer.toString(postDataLength));
                c.getOutputStream().write(postData);

                // Get the response of the server. This should be the DEX file
                InputStream input = c.getInputStream();

                // Create new File and copy the content that is received inside
                File outFile = new File(context.getFilesDir(), testClassesName);

                OutputStream output = new FileOutputStream(outFile);

                byte data[] = new byte[1024];

                while ((count = input.read(data)) != -1) {
                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                //closing streams
                output.close();
                input.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

            return f_url[0];
        }


        /**
         * After completing background task
         **/
        @Override
        protected void onPostExecute(String void_) {
            Log.i("TestLoader", "File: " + testClassesName + " downloaded!");

            File file = new File(context.getFilesDir(), testClassesName);

            //Check if the Dex file exist
            if (file.exists()) {
                //State that the DEX file is Ready
                prepareRunFile(file.getAbsolutePath());
            }
        }

    }

    JSONObject json;
    String filePath = "";

    /**
     * This method is called when the DEX file is ready.
     * If the Configuration Json file is ready as well, this method runs the test
     * @param filePath This is the path to the downloaded DEX file
     */
    private void prepareRunFile(String filePath) {
        //Check if the configuration file is already available
        if (json != null) {
            //Run the tests
            InstrumentationHelper.runTests(context, filePath, json);

            //Clear the value
            this.json = null;
            this.filePath = "";
        } else {
            //The configuration file is not ready yet, so we save that the Dex is ready
            this.filePath = filePath;
        }

    }

    /**
     * This method is called when the configuration file is ready.
     * If the DEX  file is ready as well, this method runs the test
     * @param json
     */
    private void prepareRunJson(JSONObject json) {
        Log.i("TestLoader", "json is: " + json.toString());

        //Check if the DEX file is available
        if (!filePath.equals("")) {
            //Run the tests
            InstrumentationHelper.runTests(context, filePath, json);

            //Clear the values
            this.json = null;
            this.filePath = "";
        } else {
            //The DEX file is not ready yet, so we save that the Configuration is ready
            this.json = json;
        }

    }


     class DownloadJsonFromURL extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            String resultString = null;
            //Get the Configurations Json file
            resultString = getJSON(params[0], params[1]);

            return resultString;
        }

        @Override
        protected void onPostExecute(String config) {
            super.onPostExecute(config);
            Log.i("TestLoader", "response: " + config);

            try {
                prepareRunJson(new JSONObject(config));
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        /**
         * This method download the configuration JSON file that informs which test should be executed for this
         * configuration
         * @param str_url   The endpoint where we are reading the configuration json
         * @param config    The string variant of the JSON representation of the configuration on that device
         * @return          The json string obtained from the server(If it was success)
         */
        private String getJSON(String str_url, String config) {
            HttpURLConnection c = null;

            //Prepare to send the configuration json as a POST Parameter
            //TODO improve this by sending it as a file
            String urlParameters = null;
            try {
                urlParameters = "config=" + URLEncoder.encode(config, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            Log.i("SendTestsReport", "urlParameters: " + urlParameters);
            byte[] postData = urlParameters.getBytes();
            int postDataLength = postData.length;

            try {
                URL url = new URL(str_url);
                c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("POST");
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                c.setRequestProperty("Content-Length", Integer.toString(postDataLength));

                //Send the configuration json
                c.getOutputStream().write(postData);


                //Get the response code of the request
                int status = c.getResponseCode();
                switch (status) {
                    case 200:
                    case 201:
                        //On success, read the incoming data from the server, which should be the configuration json
                        BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line + "\n");
                        }
                        br.close();
                        return sb.toString();
                }

            } catch (Exception ex) {
                return ex.toString();
            } finally {
                if (c != null) {
                    try {
                        c.disconnect();
                    } catch (Exception ex) {
                        //disconnect error
                    }
                }
            }
            return null;
        }
    }


}
