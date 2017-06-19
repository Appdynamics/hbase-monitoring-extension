package com.appdynamics.monitors.hbase.metrics;


import com.appdynamics.monitors.hbase.HBaseMBeanKeyPropertyEnum;
import com.google.common.base.Strings;

import javax.management.ObjectInstance;
import javax.management.ObjectName;

public class MetricKeyFormatter {


    private ObjectName getObjectName(ObjectInstance instance) {
        return instance.getObjectName();
    }

    public String getMasterPath(ObjectInstance instance, String metricName) {
        if (instance == null) {
            return "";
        }
        String subType = getKeyProperty(instance, HBaseMBeanKeyPropertyEnum.SUBTYPE.toString());
        String name = getKeyProperty(instance, HBaseMBeanKeyPropertyEnum.NAME.toString());

        StringBuilder metricsKey = new StringBuilder("Master|");

        //Include name ( Master ) if not already included
        if (!metricsKey.toString().contains(name)) {
            metricsKey.append(Strings.isNullOrEmpty(name) ? "" : name + "|");
        }
        metricsKey.append(Strings.isNullOrEmpty(subType) ? "" : subType + "|");
        metricsKey.append(metricName);
        return metricsKey.toString();
    }

    String getKeyProperty(ObjectInstance instance, String property) {
        if (instance == null) {
            return "";
        }
        return getObjectName(instance).getKeyProperty(property);
    }

    public String getRegionServerPath(ObjectInstance instance, String metricName, String displayName) {

        String subType = getKeyProperty(instance, HBaseMBeanKeyPropertyEnum.SUBTYPE.toString());
        String name = getKeyProperty(instance, HBaseMBeanKeyPropertyEnum.NAME.toString());

        StringBuilder metricsKey = new StringBuilder("RegionServer|" + displayName + "|");

        //Include name ( Master ) if not already included
        if (!metricsKey.toString().contains(name)) {
            metricsKey.append(Strings.isNullOrEmpty(name) ? "" : name + "|");
        }
        metricsKey.append(Strings.isNullOrEmpty(subType) ? "" : subType + "|");
        metricsKey.append(metricName);
        return metricsKey.toString();
    }

    public String getRegionServerClusterPath(ObjectInstance instance) {

        String subType = getKeyProperty(instance, HBaseMBeanKeyPropertyEnum.SUBTYPE.toString());

        StringBuilder metricsKey = new StringBuilder("RegionServer|Cluster|");
        metricsKey.append(Strings.isNullOrEmpty(subType) ? "" : subType + "|");
        return metricsKey.toString();
    }

}
