/*
 *   Copyright 2019. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.hbase.Config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class MbeanObjectConfig {
    @XmlAttribute
    private String objectName;

    @XmlElement(name = "metric")
    private MetricConfig[] metricConfigs;

    public String getObjectName(String objectName) {
        return this.objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public MetricConfig[] getMetricConfigs() {
        return metricConfigs;
    }

    public void setMetricConfigs(MetricConfig[] metricConfigs) {
        this.metricConfigs = metricConfigs;
    }
}
