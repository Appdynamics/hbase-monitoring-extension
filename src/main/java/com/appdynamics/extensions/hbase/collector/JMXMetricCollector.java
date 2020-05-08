/*
 *   Copyright 2019. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.hbase.collector;

import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.hbase.Config.MbeanObjectConfig;
import com.appdynamics.extensions.hbase.JMXConnectionAdapter;
import com.appdynamics.extensions.hbase.Util.Constants;
import static com.appdynamics.extensions.hbase.Util.Constants.ENCRYPTION_KEY;
import static com.appdynamics.extensions.hbase.Util.Constants.OBJECT_NAME;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.util.CryptoUtils;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.slf4j.Logger;

import javax.management.JMException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * @author Satish Muddam, Prashant Mehta
 */

public class JMXMetricCollector implements Callable<List<Metric>> {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(JMXMetricCollector.class);
    private Map<String, ?> server;
    private List<MbeanObjectConfig> mbeans;
    private String metricPrefix;
    private String serverDisplayName;
    private MonitorContextConfiguration configuration;
    private JMXConnectionAdapter jmxConnectionAdapter;

    public JMXMetricCollector(Map<String, ?> server, List<MbeanObjectConfig> mbeans, MonitorContextConfiguration configuration, String metricPrefix) {
        this.server = server;
        this.mbeans = mbeans;
        this.metricPrefix = metricPrefix;
        this.configuration = configuration;
    }


    public List<Metric> call() {
        serverDisplayName = (String) server.get(Constants.DISPLAY_NAME);
        try {
            initJMXConnectionAdapter();
        } catch (Exception e) {
            logger.error("Malformed Error while creating Jmx connection Adapter {}", serverDisplayName, e);
            return Collections.emptyList();
        }
        return populateJMXStats();
    }

    private List<Metric> populateJMXStats() {
        List<Metric> collectedMetrics = Lists.newArrayList();
        JMXConnector jmxConnector = null;
        if (jmxConnectionAdapter != null) {
            try {
                jmxConnector = jmxConnectionAdapter.open();
            } catch (Exception e) {
                logger.error("Error opening JMX connection to server {}", serverDisplayName, e);
                return collectedMetrics;
            }
        }
        try {
            if (jmxConnector != null) {
                for (MbeanObjectConfig aConfigMBeanObject : mbeans) {
                    String configObjectName = aConfigMBeanObject.getObjectName(OBJECT_NAME);
                    logger.debug("Processing mbean {} from the config file", configObjectName);
                    try {
                        Set<ObjectInstance> objectInstances = jmxConnectionAdapter.queryMBeans(jmxConnector, ObjectName.getInstance(configObjectName));
                        collectedMetrics.addAll(collectJMXattributeMetrics(objectInstances, jmxConnectionAdapter, jmxConnector, aConfigMBeanObject));
                    } catch (Exception ioe) {
                        logger.error("exception while reading JMX Instance attributes for {}", configObjectName, ioe);
                    }
                }
                logger.debug("Successfully completed All JMX mbeans metrics for {}", serverDisplayName);
            }
        } catch (Exception e) {
            logger.error("Error while collecting metrics from {}", serverDisplayName, e);
        } finally {
            if (jmxConnectionAdapter != null) {
                try {
                    jmxConnectionAdapter.close(jmxConnector);
                } catch (Exception e) {
                    logger.error("Failed to close the JMX connection for server {}", serverDisplayName);
                }
            }
            return collectedMetrics;
        }
    }

    private List<Metric> collectJMXattributeMetrics(Set<ObjectInstance> objectInstances, JMXConnectionAdapter jmxAdapter, JMXConnector jmxConnector, MbeanObjectConfig aConfigMBeanObject) throws JMException, IOException {
        List<Metric> collectedMetrics = Lists.newArrayList();
        for (ObjectInstance instance : objectInstances) {
            InstanceProcessor instanceProcessor = new InstanceProcessor(jmxAdapter, jmxConnector, aConfigMBeanObject, metricPrefix);
            collectedMetrics.addAll(instanceProcessor.processInstance(instance));
        }
        return collectedMetrics;
    }


    private void initJMXConnectionAdapter() throws Exception {
        String serviceUrl = (String) server.get(Constants.SERVICE_URL);
        String host = (String) server.get(Constants.HOST);
        if (Strings.isNullOrEmpty(serviceUrl) && Strings.isNullOrEmpty(host))
            logger.info("JMX details not provided, not creating connection");
        String portStr = server.get(Constants.PORT).toString();
        int port = portStr != null ? Integer.parseInt(portStr) : -1;
        String username = (String) server.get(Constants.USERNAME);
        String password = getPassword(server);
        jmxConnectionAdapter = JMXConnectionAdapter.create(serviceUrl, host, port, username, password);
    }


    private String getPassword(Map server) {
        if (configuration.getConfigYml().get(ENCRYPTION_KEY) != null) {
            String encryptionKey = (String) configuration.getConfigYml().get(ENCRYPTION_KEY);
            server.put(ENCRYPTION_KEY, encryptionKey);
        }
        return CryptoUtils.getPassword(server);
    }
}