/*
 *   Copyright 2019. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.hbase.metrics;

import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.hbase.Config.MbeanObjectConfig;
import com.appdynamics.extensions.hbase.Config.MetricConfig;
import com.appdynamics.extensions.hbase.Config.MetricConverter;
import com.appdynamics.extensions.hbase.JMXConnectionAdapter;
import com.appdynamics.extensions.hbase.Util.Constant;
import static com.appdynamics.extensions.hbase.Util.Constant.ENCRYPTION_KEY;
import static com.appdynamics.extensions.hbase.Util.Constant.NAME;
import static com.appdynamics.extensions.hbase.Util.Constant.OBJECT_NAME;
import static com.appdynamics.extensions.hbase.Util.Constant.SUBTYPE;
import com.appdynamics.extensions.hbase.Util.MbeanUtil;
import com.appdynamics.extensions.hbase.filters.IncludeFilter;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.util.CryptoUtils;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;

import javax.management.Attribute;
import javax.management.JMException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * @author Satish Muddam, Prashant Mehta
 */
public class JMXMetricCollector implements Callable<List<Metric>> {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(JMXMetricCollector.class);
    private ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, ?> server;
    private List<MbeanObjectConfig> mbeans;
    private String metricPrefix;
    private MonitorContextConfiguration configuration;

    public JMXMetricCollector(Map<String, ?> server, List<MbeanObjectConfig> mbeans, String metricPrefix, MonitorContextConfiguration configuration) {
        this.server = server;
        this.mbeans = mbeans;
        this.metricPrefix = metricPrefix;
        this.configuration = configuration;
    }


    public List<Metric> call() {
        List<Metric> collectedMetrics = Lists.newArrayList();
        JMXConnectionAdapter jmxAdapter = null;
        JMXConnector jmxConnector = null;
        String serverDisplayName = MbeanUtil.convertToString(server.get(Constant.DISPLAY_NAME), "");

        try {
            try {
                jmxAdapter = getJMXConnectionAdapter(server);
            } catch (NumberFormatException e) {
                logger.error("Number format exception while creating JMX connection to server {}", serverDisplayName, e);
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
                        String configObjectName = aConfigMBeanObject.getObjectName(OBJECT_NAME);
                        logger.debug("Processing mbean %s from the config file", configObjectName);
                        try {
                            Set<ObjectInstance> objectInstances = jmxAdapter.queryMBeans(jmxConnector, ObjectName.getInstance(configObjectName));
                            collectedMetrics.addAll(collectJMXattributeMetrics(objectInstances, jmxAdapter, jmxConnector, aConfigMBeanObject));
                        } catch (JMException jmxe) {
                            logger.error("Error thrown while reading JMX Instance attributes for {}", configObjectName, jmxe);
                        }catch (IOException ioe) {
                            logger.error("I/O exception while reading JMX Instance attributes for {}", configObjectName, ioe);
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
                    jmxAdapter.close(jmxConnector);
                } catch (Exception e) {
                    logger.error("Failed to close the JMX connection for server {}", serverDisplayName);
                }
            }
            return collectedMetrics;
        }
    }

    private Map<String, MetricConfig> buildMetricToCollectWithConfig(MetricConfig[] metricConfigs) {

        Map<String, MetricConfig> metricsWithConfig = Maps.newHashMap();
        for (MetricConfig metricConfig : metricConfigs) {
            String metricName = metricConfig.getAttr();
            metricsWithConfig.put(metricName, metricConfig);
        }
        return metricsWithConfig;
    }

    private List<Metric> collectJMXattributeMetrics(Set<ObjectInstance> objectInstances, JMXConnectionAdapter jmxAdapter, JMXConnector jmxConnector, MbeanObjectConfig aConfigMBeanObject) throws JMException, IOException {
        List<Metric> collectedMetrics = Lists.newArrayList();
        //Each mbean mentioned in the config.yml can fetch multiple object instances. Metrics need to be extracted
        //from each object instance separately.
        for (ObjectInstance instance : objectInstances) {

            List<String> jmxReadableAttributes = jmxAdapter.getReadableAttributeNames(jmxConnector, instance);
            String metricPath = getMetricPath(instance, metricPrefix);
            Map<String, MetricConfig> mbeanMetricsWithConfig = buildMetricToCollectWithConfig(aConfigMBeanObject.getMetricConfigs());
            List<String> metricNamesToBeExtracted = applyFilters(mbeanMetricsWithConfig.keySet(), jmxReadableAttributes);
            List<Attribute> attributes = jmxAdapter.getAttributes(jmxConnector, instance.getObjectName(), metricNamesToBeExtracted.toArray(new String[metricNamesToBeExtracted.size()]));
            logger.debug("collecting metrics for {} with attributes {}", instance, attributes);

            for (Attribute attr : attributes) {
                try {
                    String attrName = attr.getName();
                    Object value = attr.getValue();
                    if (value != null) {
                        MetricConfig config = mbeanMetricsWithConfig.get(attrName);
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

    private List<String> applyFilters(Set<String> metricKeys, List<String> jmxReadableAttributes) {
        return new IncludeFilter(metricKeys).apply(jmxReadableAttributes);
    }


    private JMXConnectionAdapter getJMXConnectionAdapter(Map configMap) throws NumberFormatException{

        String serviceUrl = MbeanUtil.convertToString(configMap.get(Constant.SERVICE_URL), "");
        String host = MbeanUtil.convertToString(configMap.get(Constant.HOST), "");

        if (Strings.isNullOrEmpty(serviceUrl) && Strings.isNullOrEmpty(host)) {
            logger.info("JMX details not provided, not creating connection");
            return null;
        }

        String portStr = MbeanUtil.convertToString(configMap.get(Constant.PORT), "");
        int port = portStr != null ? Integer.parseInt(portStr) : -1;
        String username = MbeanUtil.convertToString(configMap.get(Constant.USERNAME), "");
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
        String subType = getKeyProperty(instance, SUBTYPE);
        String name = getKeyProperty(instance, NAME);

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

    private String getPassword(Map server) {
        if (configuration.getConfigYml().get(ENCRYPTION_KEY) != null) {
            String encryptionKey = configuration.getConfigYml().get(ENCRYPTION_KEY).toString();
            server.put(ENCRYPTION_KEY, encryptionKey);
        }
        return CryptoUtils.getPassword(server);
    }
}