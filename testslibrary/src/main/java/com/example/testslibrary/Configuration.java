package com.example.testslibrary;


public class Configuration {
    private static Configuration instance;

    private String conf_representation="";

    public Configuration() {
    }


    public synchronized static Configuration getInstance()
    {
        if (instance==null)
        {
            instance = new Configuration();
        }
        return instance;
    }

    public String getConf_representation() {
        return conf_representation;
    }

    public void setConf_representation(String conf_representation) {
        this.conf_representation = conf_representation;
    }
}
