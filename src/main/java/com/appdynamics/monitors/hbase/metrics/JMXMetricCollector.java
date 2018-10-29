/*
 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.monitors.hbase.metrics;

import static com.appdynamics.extensions.crypto.CryptoUtil.getPassword;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.monitors.hbase.ConfigConstants;
import com.appdynamics.monitors.hbase.HBaseMBeanKeyPropertyEnum;
import com.appdynamics.monitors.hbase.JMXConnectionAdapter;
import com.appdynamics.monitors.hbase.Util;
import com.appdynamics.monitors.hbase.filters.IncludeFilter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * @author Satish Muddam
 */
public class JMXMetricCollector implements Callable<List<Metric>> {

    static final org.slf4j.Logger logger = LoggerFactory.getLogger(JMXMetricCollector.class);

    private Map server;
    private List<Map> mbeans;
    private String metricPrefix;

    public JMXMetricCollector(Map server, List<Map> mbeans, String metricPrefix) {
        this.server = server;
        this.mbeans = mbeans;
        this.metricPrefix = metricPrefix;
    }


    public List<Metric> call() {

        List<Metric> collectedMetrics = Lists.newArrayList();

        JMXConnectionAdapter jmxAdapter = null;
        JMXConnector jmxConnection = null;
        String serverDisplayName = Util.convertToString(server.get(ConfigConstants.DISPLAY_NAME), "");

        try {
            logger.debug("Collecting metrics from {}", serverDisplayName);

            try {
                jmxAdapter = getJMXConnectionAdapter(server);
            } catch (MalformedURLException e) {
                logger.error("Error creating JMX connection to server {}", serverDisplayName, e);
                return collectedMetrics;
            }

            if (jmxAdapter != null) {
                try {
                    jmxConnection = jmxAdapter.open();
                } catch (Exception e) {
                    logger.error("Error opening JMX connection to server {}", serverDisplayName, e);
                    return collectedMetrics;
                }
            }

            try {
                if (jmxConnection != null) {

                    for (Map aConfigMBean : mbeans) {

                        String configObjectName = Util.convertToString(aConfigMBean.get(ConfigConstants.OBJECT_NAME), "");
                        logger.debug("Processing mbean %s from the config file", configObjectName);

                        //Each mbean mentioned in the config.yml can fetch multiple object instances. Metrics need to be extracted
                        //from each object instance separately.
                        Set<ObjectInstance> objectInstances = jmxAdapter.queryMBeans(jmxConnection, ObjectName.getInstance(configObjectName));
                        for (ObjectInstance instance : objectInstances) {
                            List<String> metricNamesDictionary = jmxAdapter.getReadableAttributeNames(jmxConnection, instance);
                            String metricPath = getMetricPath(instance, metricPrefix);
                            Map<String, Map> metricsToCollectWithConfig = buildMetricToCollect(aConfigMBean);
                            List<String> metricNamesToBeExtracted = applyFilters(metricsToCollectWithConfig.keySet(), metricNamesDictionary);
                            List<Attribute> attributes = jmxAdapter.getAttributes(jmxConnection, instance.getObjectName(), metricNamesToBeExtracted.toArray(new String[metricNamesToBeExtracted.size()]));
                            for (Attribute attr : attributes) {
                                try {
                                    String attrName = attr.getName();
                                    Object value = attr.getValue();
                                    if (value != null) {
                                        Metric metric = new Metric(attrName, String.valueOf(value), metricPath + attrName, metricsToCollectWithConfig.get(attrName));
                                        collectedMetrics.add(metric);
                                    } else {
                                        logger.warn("Ignoring metric {} with path {} as the value is null", attrName, metricPath);
                                    }

                                } catch (Exception e) {
                                    logger.error("Error collecting value for {} {}", instance.getObjectName(), attr.getName(), e);
                                }
                            }
                        }
                    }
                    logger.debug("Successfully completed JMX mbeans metrics for {}", serverDisplayName);
                }
            } catch (Exception e) {
                logger.error("Error while collecting metrics from {}", serverDisplayName, e);
            }

        } finally {

            if (jmxAdapter != null) {
                try {
                    jmxAdapter.close(jmxConnection);
                } catch (Exception e) {
                    logger.error("Failed to close the JMX connection for server {}", serverDisplayName);
                }
            }
            return collectedMetrics;
        }
    }

    private Map<String, Map> buildMetricToCollect(Map aConfigMBean) {

        Map<String, Map> metricsWithConfig = Maps.newHashMap();

        List<Map<String, Map>> configMetrics = (List<Map<String, Map>>) aConfigMBean.get(ConfigConstants.METRICS);

        for (Map<String, Map> metricConfigured : configMetrics) {
            for (Map.Entry<String, Map> metricEntry : metricConfigured.entrySet()) {
                String metricName = metricEntry.getKey();
                Map metricPropertiesMap = metricEntry.getValue();
                metricsWithConfig.put(metricName, metricPropertiesMap);
            }
        }
        return metricsWithConfig;
    }

    private List<String> applyFilters(Set<String> metricsNames, List<String> metricNamesDictionary) throws IntrospectionException, ReflectionException, InstanceNotFoundException, IOException {
        Set<String> filteredSet = Sets.newHashSet();
        new IncludeFilter(metricsNames).apply(filteredSet, metricNamesDictionary);
        return Lists.newArrayList(filteredSet);
    }


    private JMXConnectionAdapter getJMXConnectionAdapter(Map configMap) throws MalformedURLException {

        String serviceUrl = Util.convertToString(configMap.get(ConfigConstants.SERVICE_URL), "");
        String host = Util.convertToString(configMap.get(ConfigConstants.HOST), "");

        if (Strings.isNullOrEmpty(serviceUrl) && Strings.isNullOrEmpty(host)) {
            logger.info("JMX details not provided, not creating connection");
            return null;
        }

        String portStr = Util.convertToString(configMap.get(ConfigConstants.PORT), "");
        int port = portStr != null ? Integer.parseInt(portStr) : -1;
        String username = Util.convertToString(configMap.get(ConfigConstants.USERNAME), "");
        String password = getPassword(configMap);

        try {
            JMXConnectionAdapter jmxConnectionAdapter = JMXConnectionAdapter.create(serviceUrl, host, port, username, password);
            return jmxConnectionAdapter;
        } catch (Exception e) {
            logger.error("Error while connecting to JMX interface", e);
            return null;
        }
    }

    public String getMetricPath(ObjectInstance instance, String metricPrefix) {
        if (instance == null) {
            return "";
        }
        String subType = getKeyProperty(instance, HBaseMBeanKeyPropertyEnum.SUBTYPE.toString());
        String name = getKeyProperty(instance, HBaseMBeanKeyPropertyEnum.NAME.toString());

        StringBuilder metricsKey = new StringBuilder(metricPrefix);

        if (!metricsKey.toString().contains(name)) {
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