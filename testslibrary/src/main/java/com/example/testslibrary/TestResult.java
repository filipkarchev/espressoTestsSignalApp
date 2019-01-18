package com.example.testslibrary;

import org.json.JSONException;
import org.json.JSONObject;

public class TestResult {
    //The name of the Test class
    private String className = "";

    //The name of the Test MEthod
    private String methodName = "";

    //The result of the Test. By default it is PASSED since there is no callback on success.
    //FIXME WHen the test is not found it stays by default as PASSED
    private TestResultType resultStatus = TestResultType.PASSED;

    //The error message we get from if the test fails
    private String errorMessage = "";

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public TestResultType getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(TestResultType resultStatus) {
        this.resultStatus = resultStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!TestResult.class.isAssignableFrom(obj.getClass())) {
            return false;
        }

        //The object will be found in a list if the second object has the same class name and method name
        final TestResult other = (TestResult) obj;
        if (this.className.equals(other.getClassName()) && this.methodName.equals(other.getMethodName())) {
            return true;
        }

        return false;
    }

    @Override
    public int hashCode() {
        //Used to differentiate the object when searching them in a list
        int hash = 3;
        hash = 53 * hash + (this.className != null ? this.className.hashCode() : 0) + (this.methodName !=null ? this.methodName.hashCode() : 0);
        return hash;
    }

    public JSONObject convertToJsonObject() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("className",this.className);
        object.put("methodName",this.methodName);
        object.put("errorMessage",this.errorMessage);
        object.put("resultStatus",this.resultStatus);
        return  object;
    }
}
