package com.appdynamics.monitors.hbase;

import org.apache.log4j.Logger;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

public class HBaseCommunicator implements Callable<HBaseCommunicator> {
    private Logger logger;
    private static final String HADOOP_REGION_STATISTICS_PATTERN1 = "hadoop:name=regionserver";
    private static final String HADOOP_REGION_STATISTICS_PATTERN2 = "hadoop:service=regionserver";
    private static final String CAMEL_CASE_REGEX = "(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])";

    private final HBaseMonitor.Credential cred;

    private MBeanServerConnection connection;
    private Map<String, Object> hbaseMetrics = new HashMap<String, Object>();


    public HBaseCommunicator(final HBaseMonitor.Credential credential, Logger logger) {
        cred = credential;

        this.logger = logger;
    }


    @Override
    public HBaseCommunicator call() throws Exception {
        if (isNotEmpty(cred.host) && isNotEmpty(cred.port)) {
            try {
                connect();
            } catch (IOException e) {
                logger.error("Failed to connect to " + cred.host + ":" + cred.port, e);
                return null;
            }

            final Set<String> patterns = new HashSet<String>();
            patterns.add(HADOOP_REGION_STATISTICS_PATTERN1);
            patterns.add(HADOOP_REGION_STATISTICS_PATTERN2);

            populate(patterns);
            return this;
        } else {
            return null;
        }
    }

    public String getDbname() {
        return cred.dbname;
    }

    public Map<String, Object> getMetrics() {
        return hbaseMetrics;
    }

    /**
     * Connects to JMX Remote Server to access HBase JMX Metrics
     *
     * @throws IOException Failed to connect to server.
     */
    private void connect() throws IOException {
        final JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + cred.host + ":" + cred.port + "/jmxrmi");
        final Map<String, Object> env = new HashMap<String, Object>();
        env.put(JMXConnector.CREDENTIALS, new String[]{cred.username, cred.password});
        connection = JMXConnectorFactory.connect(url, env).getMBeanServerConnection();
    }

    /**
     * Fetches all the attributes from HBase RegionServer JMX
     *
     * @throws Exception
     */
    private void populate(final Set<String> regionServerNamePatterns) throws Exception {
        try {
            // Collect all the useful metrics.
            // Get all the m-beans registered.
            final Set<ObjectInstance> queryMBeans = connection.queryMBeans(null, null);

            // Iterate through each of them available.
            for (final ObjectInstance mbean : queryMBeans) {

                // Get the canonical name
                final String canonicalName = mbean.getObjectName().getCanonicalName();

                if (startsWith(canonicalName, regionServerNamePatterns)) {
                    final ObjectName objectName = mbean.getObjectName();

                    // Fetch all attributes.
                    final MBeanAttributeInfo[] attributes = connection.getMBeanInfo(objectName).getAttributes();

                    for (final MBeanAttributeInfo attr : attributes) {
                        // See we do not violate the security rules, i.e. only if the attribute is readable.
                        if (attr.isReadable()) {
                            // Filter valid attributes.
                            final String attributeName = attr.getName();

                            if (isDisplayableAttribute(attributeName)) {
                                // Collect the statistics.
                                final Object value = connection.getAttribute(objectName, attr.getName());

                                if (value instanceof Number) {
                                    String metricName = getTileCase(attributeName);
                                    if (isNotEmpty(metricName)) {
                                        hbaseMetrics.put("Activity|" + metricName, value);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Collecting statistics failed for '" + cred.host + ":" + cred.port + "'.", e);
        }
    }


    private boolean isDisplayableAttribute(final String attributeName) {
        return !attributeName.contains(":") && !attributeName.contains(".");
    }

    /**
     * @param canonicalName
     * @param regionServerNamePatterns
     * @return
     */
    private boolean startsWith(final String canonicalName, final Set<String> regionServerNamePatterns) {
        for (final String pattern : regionServerNamePatterns) {
            if (canonicalName.toLowerCase().contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param camelCase
     * @return
     */
    private String getTileCase(String camelCase) {
        if (camelCase.contains("_")) {
            return getTileCase(camelCase, "_+");
        } else {
            return getTileCase(camelCase, CAMEL_CASE_REGEX);
        }
    }

    /**
     * @param camelCase
     * @param regex
     * @return
     */
    private String getTileCase(String camelCase, String regex) {
        String tileCase = "";
        String[] tileWords = camelCase.split(regex);

        for (String tileWord : tileWords) {
            if (tileWord.length() > 0) {
                tileCase += Character.toUpperCase(tileWord.charAt(0)) + tileWord.substring(1) + " ";
            }
        }

        return tileCase.trim();
    }

    private static boolean isNotEmpty(final String input) {
        return input != null && input.trim().length() > 0;
    }
}
