package com.appdynamics.monitors.hbase.metrics;

import static com.appdynamics.TaskInputArgs.PASSWORD_ENCRYPTED;

import com.appdynamics.TaskInputArgs;
import com.appdynamics.extensions.crypto.CryptoUtil;
import com.appdynamics.monitors.hbase.ConfigConstants;
import com.appdynamics.monitors.hbase.JMXConnectionAdapter;
import com.appdynamics.monitors.hbase.Util;
import com.appdynamics.monitors.hbase.filters.ExcludeFilter;
import com.appdynamics.monitors.hbase.filters.IncludeFilter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class NodeMetricsProcessor {

    static final org.slf4j.Logger logger = LoggerFactory.getLogger(NodeMetricsProcessor.class);

    private MetricPropertiesBuilder propertyBuilder = new MetricPropertiesBuilder();


    private final MetricKeyFormatter keyFormatter = new MetricKeyFormatter();
    private final MetricValueTransformer valueConverter = new MetricValueTransformer();

    private Map server;
    private Map<String, List<Map>> configMBeans;

    public NodeMetricsProcessor(Map server, Map<String, List<Map>> configMBeans) {
        this.server = server;
        this.configMBeans = configMBeans;

    }

    public List<Metric> getMetrics() throws Exception {

        List<Metric> metrics = Lists.newArrayList();

        List<Map> masterMbeans = new ArrayList();
        masterMbeans.addAll(configMBeans.get("common"));
        masterMbeans.addAll(configMBeans.get("master"));

        List<Map> regionServerMbeans = new ArrayList<Map>();
        regionServerMbeans.addAll(configMBeans.get("common"));
        regionServerMbeans.addAll(configMBeans.get("regionServer"));

        JMXConnectionAdapter masterJmxAdapter = getJMXConnectionAdapter(server);
        JMXConnector masterJmxConnection = masterJmxAdapter.open();

        try {
            for (Map aConfigMBean : masterMbeans) {

                String configObjectName = Util.convertToString(aConfigMBean.get(ConfigConstants.OBJECT_NAME), "");
                logger.debug("Processing mbean %s from the config file", configObjectName);
                java.util.Map<String, MetricProperties> metricPropsMap = propertyBuilder.build(aConfigMBean);

                //Each mbean mentioned in the config.yaml can fetch multiple object instances. Metrics need to be extracted
                //from each object instance separately.
                Set<ObjectInstance> objectInstances = masterJmxAdapter.queryMBeans(masterJmxConnection, ObjectName.getInstance(configObjectName));
                for (ObjectInstance instance : objectInstances) {
                    List<String> metricNamesDictionary = masterJmxAdapter.getReadableAttributeNames(masterJmxConnection, instance);
                    List<String> metricNamesToBeExtracted = applyFilters(aConfigMBean, metricNamesDictionary);
                    List<Attribute> attributes = masterJmxAdapter.getAttributes(masterJmxConnection, instance.getObjectName(), metricNamesToBeExtracted.toArray(new String[metricNamesToBeExtracted.size()]));
                    //get node metrics
                    collectMaster(metrics, attributes, instance, metricPropsMap);
                }
            }

            getRegionServerMetrics(server, regionServerMbeans, metrics);

        } catch (MalformedObjectNameException e) {
            logger.error("Illegal object name", e);
            throw e;
        } catch (Exception e) {
            //System.out.print("" + e);
            logger.error("Error fetching JMX metrics", e);
            throw e;
        } finally {
            if (masterJmxAdapter != null && masterJmxConnection != null) {
                try {
                    logger.debug("Closing master connection [" + masterJmxConnection.getConnectionId() + "]");
                    masterJmxAdapter.close(masterJmxConnection);
                } catch (Exception e) {
                    logger.error("Error closing JMX connection", e);
                    throw e;
                }
            }
        }
        return metrics;
    }

    private void getRegionServerMetrics(Map server, List<Map> regionServerMbeans, List<Metric> metrics) throws Exception {
        List<Map> regionServers = (List<Map>) server.get(ConfigConstants.REGIONSERVERS);

        for (Map regionServer : regionServers) {

            String displayName = Util.convertToString(regionServer.get(ConfigConstants.DISPLAY_NAME), "");

            JMXConnectionAdapter regionServerJmxAdapter = getJMXConnectionAdapter(regionServer);
            JMXConnector regionServerJmxConnection = regionServerJmxAdapter.open();

            try {
                for (Map aConfigMBean : regionServerMbeans) {

                    String configObjectName = Util.convertToString(aConfigMBean.get(ConfigConstants.OBJECT_NAME), "");
                    logger.debug("Processing mbean %s from the config file", configObjectName);
                    java.util.Map<String, MetricProperties> metricPropsMap = propertyBuilder.build(aConfigMBean);

                    //Each mbean mentioned in the config.yaml can fetch multiple object instances. Metrics need to be extracted
                    //from each object instance separately.
                    Set<ObjectInstance> objectInstances = regionServerJmxAdapter.queryMBeans(regionServerJmxConnection, ObjectName.getInstance(configObjectName));
                    for (ObjectInstance instance : objectInstances) {
                        List<String> metricNamesDictionary = regionServerJmxAdapter.getReadableAttributeNames(regionServerJmxConnection, instance);
                        List<String> metricNamesToBeExtracted = applyFilters(aConfigMBean, metricNamesDictionary);
                        List<Attribute> attributes = regionServerJmxAdapter.getAttributes(regionServerJmxConnection, instance.getObjectName(), metricNamesToBeExtracted.toArray(new String[metricNamesToBeExtracted.size()]));
                        //get node metrics
                        collectRegionServer(metrics, attributes, instance, metricPropsMap, displayName);
                    }
                }
            } finally {
                if (regionServerJmxAdapter != null && regionServerJmxConnection != null) {
                    try {
                        logger.debug("Closing region server connection [" + regionServerJmxConnection.getConnectionId() + "]");
                        regionServerJmxAdapter.close(regionServerJmxConnection);
                    } catch (Exception e) {
                        logger.error("Error closing JMX connection", e);
                        throw e;
                    }
                }
            }
        }
    }

    private JMXConnectionAdapter getJMXConnectionAdapter(Map configMap) throws MalformedURLException {

        String serviceUrl = Util.convertToString(configMap.get(ConfigConstants.SERVICE_URL), "");
        String host = Util.convertToString(configMap.get(ConfigConstants.HOST), "");
        String portStr = Util.convertToString(configMap.get(ConfigConstants.PORT), "");
        int port = portStr != null ? Integer.parseInt(portStr) : -1;
        String username = Util.convertToString(configMap.get(ConfigConstants.USERNAME), "");
        String password = getPassword(configMap);

        return JMXConnectionAdapter.create(serviceUrl, host, port, username, password);
    }

    private String getPassword(Map configMap) {
        String password = Util.convertToString(configMap.get(ConfigConstants.PASSWORD), "");
        if (!Strings.isNullOrEmpty(password)) {
            return password;
        }
        String encryptionKey = Util.convertToString(server.get(ConfigConstants.ENCRYPTION_KEY), "");
        String encryptedPassword = Util.convertToString(configMap.get(ConfigConstants.ENCRYPTED_PASSWORD), "");
        if (!Strings.isNullOrEmpty(encryptionKey) && !Strings.isNullOrEmpty(encryptedPassword)) {
            java.util.Map<String, String> cryptoMap = Maps.newHashMap();
            cryptoMap.put(PASSWORD_ENCRYPTED, encryptedPassword);
            cryptoMap.put(TaskInputArgs.ENCRYPTION_KEY, encryptionKey);
            return CryptoUtil.getPassword(cryptoMap);
        }
        return null;
    }

    private List<String> applyFilters(Map aConfigMBean, List<String> metricNamesDictionary) throws IntrospectionException, ReflectionException, InstanceNotFoundException, IOException {
        Set<String> filteredSet = Sets.newHashSet();
        Map configMetrics = (Map) aConfigMBean.get(ConfigConstants.METRICS);
        List includeDictionary = (List) configMetrics.get(ConfigConstants.INCLUDE);
        List excludeDictionary = (List) configMetrics.get(ConfigConstants.EXCLUDE);
        new ExcludeFilter(excludeDictionary).apply(filteredSet, metricNamesDictionary);
        new IncludeFilter(includeDictionary).apply(filteredSet, metricNamesDictionary);
        return Lists.newArrayList(filteredSet);
    }

    private void collectMaster(List<Metric> nodeMetrics, List<Attribute> attributes, ObjectInstance instance, Map<String, MetricProperties> metricPropsPerMetricName) {
        for (Attribute attr : attributes) {
            try {
                String attrName = attr.getName();
                MetricProperties props = metricPropsPerMetricName.get(attrName);
                if (props == null) {
                    logger.error("Could not find metric props for {}", attrName);
                    continue;
                }
                //get metric value by applying conversions if necessary
                BigDecimal metricValue = valueConverter.transform(attrName, attr.getValue(), props);
                if (metricValue != null) {
                    Metric nodeMetric = new Metric();
                    nodeMetric.setMetricName(attrName);
                    String metricName = nodeMetric.getMetricNameOrAlias();
                    String metricPath = keyFormatter.getMasterPath(instance, metricName);

                    nodeMetric.setProperties(props);
                    nodeMetric.setMetricPath(metricPath);
                    nodeMetric.setMetricValue(metricValue);
                    nodeMetrics.add(nodeMetric);
                }

            } catch (Exception e) {
                logger.error("Error collecting value for {} {}", instance.getObjectName(), attr.getName(), e);
            }
        }
    }

    private void collectRegionServer(List<Metric> nodeMetrics, List<Attribute> attributes, ObjectInstance instance, Map<String, MetricProperties> metricPropsPerMetricName, String displayName) {
        for (Attribute attr : attributes) {
            try {
                String attrName = attr.getName();
                MetricProperties props = metricPropsPerMetricName.get(attrName);
                if (props == null) {
                    logger.error("Could not find metric props for {}", attrName);
                    continue;
                }
                //get metric value by applying conversions if necessary
                BigDecimal metricValue = valueConverter.transform(attrName, attr.getValue(), props);
                if (metricValue != null) {
                    Metric nodeMetric = new Metric();
                    nodeMetric.setMetricName(attrName);
                    String metricName = nodeMetric.getMetricNameOrAlias();
                    String metricPath = keyFormatter.getRegionServerPath(instance, metricName, displayName);
                    nodeMetric.setClusterKey(keyFormatter.getRegionServerClusterPath(instance));

                    nodeMetric.setProperties(props);
                    nodeMetric.setMetricPath(metricPath);
                    nodeMetric.setMetricValue(metricValue);
                    nodeMetrics.add(nodeMetric);
                }

            } catch (Exception e) {
                logger.error("Error collecting value for {} {}", instance.getObjectName(), attr.getName(), e);
            }
        }
    }
}