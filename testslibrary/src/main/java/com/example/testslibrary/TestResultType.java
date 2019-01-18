package com.example.testslibrary;

public enum TestResultType {
    //The Test is SUCCESSFUL
    PASSED,

    //The test FAILED due to wrong assertion result
    ASSERTION_FAIL,

    //There was other error in the test, besides Assertion fail. It could be a crash, or etc.
    ERROR
}
