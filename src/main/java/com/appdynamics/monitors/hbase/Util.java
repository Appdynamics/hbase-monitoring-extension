/*
 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.monitors.hbase;


import com.appdynamics.monitors.hbase.Config.MbeanObjectConfig;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class Util {

    public static String convertToString(final Object field, final String defaultStr){
        if(field == null){
            return defaultStr;
        }
        return field.toString();
    }

    public static void addAllValidMbeans(List<MbeanObjectConfig> allMbeans, List<MbeanObjectConfig> fetchedMbeans) {
        if (fetchedMbeans != null && !fetchedMbeans.isEmpty())
            allMbeans.addAll(fetchedMbeans);
    }

    public static String[] split(final String metricType, final String splitOn) {
        return metricType.split(splitOn);
    }

    public static String toBigIntString(final BigDecimal bigD) {
        return bigD.setScale(0, RoundingMode.HALF_UP).toBigInteger().toString();
    }
}
