/*
 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.monitors.hbase.metrics;

import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import static com.appdynamics.extensions.crypto.CryptoUtil.getPassword;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.monitors.hbase.Config.MbeanObjectConfig;
import com.appdynamics.monitors.hbase.Config.MetricConfig;
import com.appdynamics.monitors.hbase.Config.MetricConverter;
import com.appdynamics.monitors.hbase.Constant;
import com.appdynamics.monitors.hbase.HBaseMBeanKeyPropertyEnum;
import com.appdynamics.monitors.hbase.JMXConnectionAdapter;
import com.appdynamics.monitors.hbase.Util;
import com.appdynamics.monitors.hbase.filters.IncludeFilter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.codehaus.jackson.map.ObjectMapper;
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
    private ObjectMapper objectMapper = new ObjectMapper();
    private Map server;
    private List<MbeanObjectConfig> mbeans;
    private String metricPrefix;
    private MonitorContextConfiguration configuration;

    public JMXMetricCollector(Map server, List<MbeanObjectConfig> mbeans, String metricPrefix, MonitorContextConfiguration configuration) {
        this.server = server;
        this.mbeans = mbeans;
        this.metricPrefix = metricPrefix;
        this.configuration = configuration;
    }


    public List<Metric> call() {
        List<Metric> collectedMetrics = Lists.newArrayList();
        JMXConnectionAdapter jmxAdapter = null;
        JMXConnector jmxConnector = null;
        String serverDisplayName = Util.convertToString(server.get(Constant.DISPLAY_NAME), "");

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
                    jmxConnector = jmxAdapter.open();
                } catch (Exception e) {
                    logger.error("Error opening JMX connection to server {}", serverDisplayName, e);
                    return collectedMetrics;
                }
            }

            try {
                if (jmxConnector != null) {
                    for (MbeanObjectConfig aConfigMBeanObject : mbeans) {
                        String configObjectName = aConfigMBeanObject.getObjectName(Constant.OBJECT_NAME);
                        logger.debug("Processing mbean %s from the config file", configObjectName);
                        Set<ObjectInstance> objectInstances = jmxAdapter.queryMBeans(jmxConnector, ObjectName.getInstance(configObjectName));

                        collectedMetrics.addAll(collectJMXattributeMetrics(objectInstances, jmxAdapter, jmxConnector, aConfigMBeanObject));
                    }
                    logger.debug("Successfully completed JMX mbeans metrics for {}", serverDisplayName);
                }
            } catch (Exception e) {
                logger.error("Error while collecting metrics from {}", serverDisplayName, e);
            }

        } finally {

            if (jmxAdapter != null) {
                try {
                    jmxAdapter.close(jmxConnector);
                } catch (Exception e) {
                    logger.error("Failed to close the JMX connection for server {}", serverDisplayName);
                }
            }
            return collectedMetrics;
        }
    }

    private Map<String, MetricConfig> buildMetricToCollect(MetricConfig[] metricConfigs) {

        Map<String, MetricConfig> metricsWithConfig = Maps.newHashMap();
        for (MetricConfig metricConfig : metricConfigs) {
            String metricName = metricConfig.getAttr();
            metricsWithConfig.put(metricName, metricConfig);
        }
        return metricsWithConfig;
    }

    private List<Metric> collectJMXattributeMetrics(Set<ObjectInstance> objectInstances, JMXConnectionAdapter jmxAdapter, JMXConnector jmxConnector, MbeanObjectConfig aConfigMBeanObject) throws IntrospectionException, ReflectionException, InstanceNotFoundException, IOException {
        List<Metric> collectedMetrics = Lists.newArrayList();
        //Each mbean mentioned in the config.yml can fetch multiple object instances. Metrics need to be extracted
        //from each object instance separately.
        for (ObjectInstance instance : objectInstances){
            List<String> metricNamesDictionary = jmxAdapter.getReadableAttributeNames(jmxConnector, instance);
            String metricPath = getMetricPath(instance, metricPrefix);
            Map<String, MetricConfig> metricsToCollectWithConfig = buildMetricToCollect(aConfigMBeanObject.getMetricConfigs());
            List<String> metricNamesToBeExtracted = applyFilters(metricsToCollectWithConfig.keySet(), metricNamesDictionary);
            List<Attribute> attributes = jmxAdapter.getAttributes(jmxConnector, instance.getObjectName(), metricNamesToBeExtracted.toArray(new String[metricNamesToBeExtracted.size()]));
            for (Attribute attr : attributes) {
                try {
                    String attrName = attr.getName();
                    Object value = attr.getValue();
                    if (value != null) {
                        MetricConfig config = metricsToCollectWithConfig.get(attrName);
                        if (config.getMetricConverter() != null)
                            value = getConvertedStatus(config.getMetricConverter(), String.valueOf(value));
                        Metric metric = new Metric(attrName, String.valueOf(value), metricPath + attrName, objectMapper.convertValue(config, Map.class));
                        collectedMetrics.add(metric);
                    } else {
                        logger.warn("Ignoring metric {} with path {} as the value is null", attrName, metricPath);
                    }
                } catch (Exception e) {
                    logger.error("Error collecting value for {} {}", instance.getObjectName(), attr.getName(), e);
                }
            }
        }
        return collectedMetrics;
    }

    private List<String> applyFilters(Set<String> metricsNames, List<String> metricNamesDictionary) {
        Set<String> filteredSet = Sets.newHashSet();
        new IncludeFilter(metricsNames).apply(filteredSet, metricNamesDictionary);
        return Lists.newArrayList(filteredSet);
    }


    private JMXConnectionAdapter getJMXConnectionAdapter(Map configMap) throws MalformedURLException {

        String serviceUrl = Util.convertToString(configMap.get(Constant.SERVICE_URL), "");
        String host = Util.convertToString(configMap.get(Constant.HOST), "");

        if (Strings.isNullOrEmpty(serviceUrl) && Strings.isNullOrEmpty(host)) {
            logger.info("JMX details not provided, not creating connection");
            return null;
        }

        String portStr = Util.convertToString(configMap.get(Constant.PORT), "");
        int port = portStr != null ? Integer.parseInt(portStr) : -1;
        String username = Util.convertToString(configMap.get(Constant.USERNAME), "");
        String password = getPassword(configMap);

        try {
            JMXConnectionAdapter jmxConnectionAdapter = JMXConnectionAdapter.create(serviceUrl, host, port, username, password);
            return jmxConnectionAdapter;
        } catch (Exception e) {
            logger.error("Error while connecting to JMX interface", e);
            return null;
        }
    }

    private String getMetricPath(ObjectInstance instance, String metricPrefix) {
        if (instance == null) {
            return "";
        }
        String subType = getKeyProperty(instance, HBaseMBeanKeyPropertyEnum.SUBTYPE.toString());
        String name = getKeyProperty(instance, HBaseMBeanKeyPropertyEnum.NAME.toString());

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


    /**
     * @param converters
     * @param status
     * @return
     */
    private String getConvertedStatus(MetricConverter[] converters, String status) {
        for (MetricConverter converter : converters) {
            if (converter.getLabel().equals(status))
                return converter.getValue();
        }
        return "";
    }
}