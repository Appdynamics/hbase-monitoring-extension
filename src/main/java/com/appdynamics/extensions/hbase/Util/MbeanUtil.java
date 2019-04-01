/*
 *   Copyright 2019. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.hbase.Util;


import com.appdynamics.extensions.hbase.Config.MbeanObjectConfig;

import java.util.List;

public class MbeanUtil {

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
}
