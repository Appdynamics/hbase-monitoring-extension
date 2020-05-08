/*
 *   Copyright 2019 . AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.hbase.Util;

import com.appdynamics.extensions.hbase.Config.MetricConfig;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.util.Map;

/**
 * Created by Prashant Mehta on 2/26/18.
 */
public class CommonUtil {

    public static boolean isCompositeObject(String objectName) {
        return (objectName.indexOf('.') != -1);
    }

    public static String getMetricNameFromCompositeObject(String objectName) {
        return objectName.split("\\.")[0];
    }

    public static Map<String, MetricConfig> buildMetricToCollectWithConfig(MetricConfig[] metricConfigs) {

        Map<String, MetricConfig> metricsWithConfig = Maps.newHashMap();
        for (MetricConfig metricConfig : metricConfigs) {
            String metricName = metricConfig.getAttr();
            metricsWithConfig.put(metricName, metricConfig);
        }
        return metricsWithConfig;
    }

    public static String getMetricPath(ObjectInstance instance, String metricPrefix) {
        if (instance == null) {
            return "";
        }
        String subType = getKeyProperty(instance, Constants.SUBTYPE);
        String name = getKeyProperty(instance, Constants.NAME);

        StringBuilder metricsKey = new StringBuilder(metricPrefix);

        if (name != null && !metricsKey.toString().contains(name)) {
            metricsKey.append(Strings.isNullOrEmpty(name) ? "" : name + "|");
        }
        metricsKey.append(Strings.isNullOrEmpty(subType) ? "" : subType + "|");
        return metricsKey.toString();
    }

    public static String getKeyProperty(ObjectInstance instance, String property) {
        if (instance == null) {
            return "";
        }
        return getObjectName(instance).getKeyProperty(property);
    }

    public static ObjectName getObjectName(ObjectInstance instance) {
        return instance.getObjectName();
    }
}
