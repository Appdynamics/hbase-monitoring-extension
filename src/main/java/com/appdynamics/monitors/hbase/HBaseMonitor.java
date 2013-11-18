
package com.appdynamics.monitors.hbase;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.log4j.Logger;

import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

public class HBaseMonitor extends AManagedMonitor
{
    private static Logger logger = Logger.getLogger(HBaseMonitor.class);

    // private static final String HADOOP_REGION_STATISTICS_BEAN = "hadoop:service=RegionServer,name=RegionServerStatistics";
    private static final String CUSTOM_METRICS_H_BASE_STATUS = "Custom Metrics|HBase|Status|";
    private static final String HADOOP_REGION_STATISTICS_PATTERN1 = "hadoop:name=regionserver";
    private static final String HADOOP_REGION_STATISTICS_PATTERN2 = "hadoop:service=regionserver";
    private static final String CAMEL_CASE_REGEX = "(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])";

    private MBeanServerConnection connection = null;
    private Map<MBeanAttributeInfo, Object> hbaseMetrics = new HashMap<MBeanAttributeInfo, Object>();

    /**
     * Connects to JMX Remote Server to access HBase JMX Metrics
     * 
     * @param   host                Host of the remote jmx server.
     * @param   port                Port of the remote jmx server.
     * @param   username            Username to access the remote jmx server.
     * @param   password            Password to access the remote jmx server.
     * @throws  IOException         Failed to connect to server.
     */
    public void connect(final String host, final String port, final String username, final String password)
        throws IOException
    {
        final JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi");
        final Map<String, Object> env = new HashMap<String, Object>();
        env.put(JMXConnector.CREDENTIALS, new String[] { username, password });
        this.connection = JMXConnectorFactory.connect(url, env).getMBeanServerConnection();
    }

    /**
     * Main execution method that uploads the metrics to the AppDynamics Controller
     * @see com.singularity.ee.agent.systemagent.api.ITask#execute(java.util.Map, com.singularity.ee.agent.systemagent.api.TaskExecutionContext)
     */
    @Override
    public TaskOutput execute(Map<String, String> args, TaskExecutionContext arg1) throws TaskExecutionException
    {
        try {
            final String host = args.get("host");
            final String port = args.get("port");
            final String username = args.get("user");
            final String password = args.get("pass");

            connect(host, port, username, password);

            final Set<String> patterns = new HashSet<String>();
            patterns.add(HADOOP_REGION_STATISTICS_PATTERN1);
            patterns.add(HADOOP_REGION_STATISTICS_PATTERN2);

            populate(patterns);

            printMetric(
                "Uptime", 1, MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                MetricWriter.METRIC_TIME_ROLLUP_TYPE_SUM, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

            for (Map.Entry<MBeanAttributeInfo, Object> metric : hbaseMetrics.entrySet()) {
                String attributeName = metric.getKey().getName();
                String metricName = getTileCase(attributeName, true);
                if (metricName != null && !metricName.equals("")) {
                    if (null != metric.getValue() && Number.class.isAssignableFrom(metric.getValue().getClass())) {
                        Double result = Double.parseDouble(String.valueOf(metric.getValue()));
                        printMetric(
                            "Activity|" + getTileCase(metricName, true), result.intValue(),
                            MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                            MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                            MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
                    }
                }
            }

            return new TaskOutput("HBase Metric Upload Complete");
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
            return new TaskOutput("HBase Metric Upload Failed!");
        }
    }

    /**
     * Fetches all the attributes from HBase RegionServer JMX
     * @throws Exception
     */
    public void populate(final Set<String> regionServerNamePatterns) throws Exception
    {
        try {
            // Collect all the useful metrics.
            // Get all the m-beans registered.
            final Set<ObjectInstance> queryMBeans = this.connection.queryMBeans(null, null);

            // Iterate through each of them available.
            for (final ObjectInstance mbean : queryMBeans) {

                // Get the canonical name
                final String canonicalName = mbean.getObjectName().getCanonicalName();

                if (startsWith(canonicalName, regionServerNamePatterns)) {
                    final ObjectName objectName = mbean.getObjectName();

                    // Fetch all attributes.
                    final MBeanAttributeInfo[] attributes = this.connection.getMBeanInfo(objectName).getAttributes();

                    for (final MBeanAttributeInfo attr : attributes) {
                        // See we do not violate the security rules, i.e. only if the attribute is readable.
                        if (attr.isReadable()) {
                            // Filter valid attributes.
                            final String attributeName = attr.getName();

                            if (isDisplayableAttribute(attributeName)) {
                                // Collect the statistics.
                                final Object value = this.connection.getAttribute(objectName, attr.getName());
                                this.hbaseMetrics.put(attr, value);
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            logger.error("Collecting statistics failed.", e);
        }
    }

    public boolean isDisplayableAttribute(final String attributeName)
    {
        return !attributeName.contains(":") && !attributeName.contains(".");
    }

    /**
     * @param canonicalName
     * @param regionServerNamePatterns
     * @return
     */
    private boolean startsWith(final String canonicalName, final Set<String> regionServerNamePatterns)
    {
        for (final String pattern : regionServerNamePatterns) {
            if (canonicalName.toLowerCase().contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param camelCase
     * @param caps
     * @return
     */
    public String getTileCase(final String camelCase, final boolean caps)
    {
        if (-1 == camelCase.indexOf('_')) {
            return _getTileCase(camelCase, CAMEL_CASE_REGEX);
        }
        else {
            return _getTileCase(camelCase, "_");
        }
    }

    /**
     * @param camelCase
     * @param regEx
     * @param caps
     * @return
     */
    public String _getTileCase(final String camelCase, final String regEx)
    {
        String tileCase = "";
        String[] tileWords = camelCase.split(regEx);

        for (String tileWord : tileWords) {
            tileCase += Character.toUpperCase(tileWord.charAt(0)) + tileWord.substring(1) + " ";
        }

        return tileCase.trim();
    }

    /**
     * Returns the metric to the AppDynamics Controller.
     * 
     * @param metricName 	Name of the Metric
     * @param metricValue 	Value of the Metric
     * @param aggregation 	Average OR Observation OR Sum
     * @param timeRollup 	Average OR Current OR Sum
     * @param cluster 		Collective OR Individual
     */
    public void printMetric(
        String metricName,
        Object metricValue,
        String aggregation,
        String timeRollup,
        String cluster)
    {
        logger.info("Sending [" + getMetricPrefix() + metricName + "]");

        MetricWriter metricWriter = getMetricWriter(getMetricPrefix() + metricName, aggregation, timeRollup, cluster);

        metricWriter.printMetric(String.valueOf(metricValue));
    }

    /**
     * Metric Prefix
     * 
     * @return Metric Location in the Controller (String)
     */
    public String getMetricPrefix()
    {
        return CUSTOM_METRICS_H_BASE_STATUS;
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception
    {
        Map<String, String> args1 = new HashMap<String, String>();
        args1.put("host", "localhost");
        args1.put("port", "10101");

        new HBaseMonitor().execute(args1, null);
    }
}
