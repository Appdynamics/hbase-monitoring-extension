package com.appdynamics.monitors.hbase;

public class Constant {
    public static String USER_NAME;
    public static String PASSWORD;
    public static String ENCRYPTED_PASSWORD;
    public static String ENCRYPTION_KEY;
    public static String METRIC_SEPARATOR;
    public static String METRIC_PREFIX;
    public static String MonitorName;
    public static String CONFIG_ARG;
    public static String METRIC_ARG;
    public static final String CONNECTION;
    public static final String DISPLAY_NAME;
    public static final String HOST;
    public static final String PORT;
    public static final String SERVICE_URL;
    public static final String USERNAME;
    public static final String MBEANS;
    public static final String OBJECT_NAME;
    public static final String REGIONSERVERS;

    static {
        METRIC_PREFIX = "Custom Metrics|HBase|";
        MonitorName = "HBase Monitor";
        USER_NAME = "username";
        ENCRYPTED_PASSWORD = "encryptedPassword";
        ENCRYPTION_KEY = "encryptionKey";
        PASSWORD = "password";
        METRIC_SEPARATOR = "|";
        CONFIG_ARG = "config-file";
        METRIC_ARG = "metric-file";
        CONNECTION = "connection";
        DISPLAY_NAME = "displayName";
        HOST = "host";
        PORT = "port";
        SERVICE_URL = "serviceUrl";
        USERNAME = "username";
        MBEANS = "mbeans";
        OBJECT_NAME = "objectName";
        REGIONSERVERS = "regionServers";
    }
}