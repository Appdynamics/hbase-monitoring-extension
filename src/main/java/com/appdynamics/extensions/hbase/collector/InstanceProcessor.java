package com.appdynamics.extensions.hbase.collector;/*
 * Copyright 2019. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

import com.appdynamics.extensions.hbase.Config.MbeanObjectConfig;
import com.appdynamics.extensions.hbase.Config.MetricConfig;
import com.appdynamics.extensions.hbase.JMXConnectionAdapter;
import com.appdynamics.extensions.hbase.Util.Constants;
import com.appdynamics.extensions.hbase.filters.IncludeFilter;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InstanceProcessor {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(InstanceProcessor.class);

    private JMXConnectionAdapter jmxAdapter;
    private JMXConnector jmxConnector;
    private MbeanObjectConfig aConfigMBeanObject;
    Map<String, MetricConfig> mbeanMetricsWithConfig;
    private String metricPrefix;

    public InstanceProcessor(JMXConnectionAdapter jmxAdapter, JMXConnector jmxConnector, MbeanObjectConfig aConfigMBeanObject, String metricPrefix) {
        this.jmxAdapter = jmxAdapter;
        this.jmxConnector = jmxConnector;
        this.aConfigMBeanObject = aConfigMBeanObject;
        this.metricPrefix = metricPrefix;
    }

    public List<Metric> processInstance(ObjectInstance instance) throws IntrospectionException, ReflectionException, InstanceNotFoundException, IOException {
        List<String> jmxReadableAttributes = jmxAdapter.getReadableAttributeNames(jmxConnector, instance);
        List<Attribute> attributes = fetchAttributes(instance, jmxReadableAttributes);
        String metricPath = getMetricPath(instance, metricPrefix);
        List<Metric> attributeMetrics = Lists.newArrayList();
        Metric metric = null;
        for (Attribute attr : attributes) {
            AttributeProcessor attributeProcessor = new AttributeProcessor();
            try {
                metric = attributeProcessor.processAttributeToMetric(attr, mbeanMetricsWithConfig, metricPath);
            } catch (Exception e) {
                logger.error("Error collecting value for {} {}", instance.getObjectName(), attr.getName(), e);
            }
            attributeMetrics.add(metric);
        }

        logger.debug("collecting metrics for {} with attributes {}", instance, attributes);
        return attributeMetrics;
    }

    public List<Attribute> fetchAttributes(ObjectInstance instance, List<String> jmxReadableAttributes) throws InstanceNotFoundException, IOException, ReflectionException {
        mbeanMetricsWithConfig = buildMetricToCollectWithConfig(aConfigMBeanObject.getMetricConfigs());
        List<String> metricNamesToBeExtracted = applyFilters(mbeanMetricsWithConfig.keySet(), jmxReadableAttributes);
        return jmxAdapter.getAttributes(jmxConnector, instance.getObjectName(), metricNamesToBeExtracted.toArray(new String[metricNamesToBeExtracted.size()]));
    }

    private Map<String, MetricConfig> buildMetricToCollectWithConfig(MetricConfig[] metricConfigs) {

        Map<String, MetricConfig> metricsWithConfig = Maps.newHashMap();
        for (MetricConfig metricConfig : metricConfigs) {
            String metricName = metricConfig.getAttr();
            metricsWithConfig.put(metricName, metricConfig);
        }
        return metricsWithConfig;
    }

    private List<String> applyFilters(Set<String> metricKeys, List<String> jmxReadableAttributes) {
        return new IncludeFilter(metricKeys).apply(jmxReadableAttributes);
    }

    private String getMetricPath(ObjectInstance instance, String metricPrefix) {
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

    private String getKeyProperty(ObjectInstance instance, String property) {
        if (instance == null) {
            return "";
        }
        return getObjectName(instance).getKeyProperty(property);
    }

    private ObjectName getObjectName(ObjectInstance instance) {
        return instance.getObjectName();
    }

}
