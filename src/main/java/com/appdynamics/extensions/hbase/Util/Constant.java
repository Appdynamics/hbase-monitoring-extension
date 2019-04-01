/*
 *   Copyright 2019. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.hbase.Util;

public class Constant {
    public static String ENCRYPTION_KEY;
    public static String METRIC_SEPARATOR;
    public static String METRIC_PREFIX;
    public static String MonitorName;
    public static final String DISPLAY_NAME;
    public static final String HOST;
    public static final String PORT;
    public static final String SERVICE_URL;
    public static final String USERNAME;
    public static final String MBEANS;
    public static final String OBJECT_NAME;
    public static final String REGIONSERVERS;
    public static final String NAME;
    public static final String SUBTYPE;

    static {
        METRIC_PREFIX = "Custom Metrics|HBase|";
        MonitorName = "HBase Monitor";
        ENCRYPTION_KEY = "encryptionKey";
        METRIC_SEPARATOR = "|";
        DISPLAY_NAME = "displayName";
        HOST = "host";
        PORT = "port";
        SERVICE_URL = "serviceUrl";
        USERNAME = "username";
        MBEANS = "mbeans";
        OBJECT_NAME = "objectName";
        REGIONSERVERS = "regionServers";
        NAME = "name";
        SUBTYPE = "sub";
    }
}