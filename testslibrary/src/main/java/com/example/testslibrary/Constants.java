package com.example.testslibrary;

public class Constants {
    //The endpoint on the server where we are getting the .dex file from
    public static final String reportUrl = "http://10.0.2.2:8080/send_report";

    //The endpoint on the server where we are information which tests to run for our configuration
    public static final String fileUrl ="http://10.0.2.2:8080/get_test_classes";

    //The endpoint on the server where we are sending our report after all tests are executed
    public static final String jsonUrl ="http://10.0.2.2:8080/get_config_file";


}
