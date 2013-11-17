
package com.appdynamics.monitors.hbase;

import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HBaseMonitor extends AManagedMonitor {
    private static Logger logger = Logger.getLogger(HBaseMonitor.class);
    private List<Credential> credentials;

    // private static final String HADOOP_REGION_STATISTICS_BEAN = "hadoop:service=RegionServer,name=RegionServerStatistics";
    private static final String CUSTOM_METRICS_H_BASE_STATUS = "Custom Metrics|HBase|";

    /**
     * Main execution method that uploads the metrics to the AppDynamics Controller
     *
     * @see com.singularity.ee.agent.systemagent.api.ITask#execute(java.util.Map, com.singularity.ee.agent.systemagent.api.TaskExecutionContext)
     */
    @Override
    public TaskOutput execute(Map<String, String> args, TaskExecutionContext arg1) throws TaskExecutionException {
        getCredentials(args);
        try {
            ExecutorService executor = Executors.newFixedThreadPool(credentials.size());

            CompletionService<HBaseCommunicator> threadPool =
                    new ExecutorCompletionService<HBaseCommunicator>(executor);

            for (Credential cred : credentials) {
                threadPool.submit(new HBaseCommunicator(cred, logger));
            }

            for (int i = 0; i < credentials.size(); i++) {
                try {
                    HBaseCommunicator comm = threadPool.take().get();
                    if (comm != null && !comm.getMetrics().isEmpty()) {
                        String dbname = comm.getDbname();
                        printMetric(dbname + "|Uptime", 1,
                                MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                                MetricWriter.METRIC_TIME_ROLLUP_TYPE_SUM,
                                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

                        Map<String, Object> metrics = comm.getMetrics();
                        for (Map.Entry<String, Object> metric : metrics.entrySet()) {
                            printMetric(dbname + "|" + metric.getKey(), metric.getValue(),
                                    MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                                    MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                                    MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to get metrics", e);
                }
            }
            executor.shutdown();

            return new TaskOutput("HBase Metric Upload Complete");
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
            return new TaskOutput("HBase Metric Upload Failed!");
        }
    }

    private void getCredentials(final Map<String, String> args) {
        credentials = new ArrayList<Credential>();
        Credential cred = new Credential();

        cred.dbname = args.get("dbname");
        cred.host = args.get("host");
        cred.port = args.get("port");
        cred.username = args.get("user");
        cred.password = args.get("pass");

        if (!isNotEmpty(cred.dbname)) {
            cred.dbname = "DB 1";
        }

        credentials.add(cred);

        String xmlPath = args.get("properties-path");
        if (isNotEmpty(xmlPath)) {
            try {
                SAXReader reader = new SAXReader();
                Document doc = reader.read(xmlPath);
                Element root = doc.getRootElement();

                for (Element credElem : (List<Element>) root.elements("credentials")) {
                    cred.dbname = credElem.elementText("dbname");
                    cred.host = credElem.elementText("host");
                    cred.port = credElem.elementText("port");
                    cred.username = credElem.elementText("username");
                    cred.password = credElem.elementText("password");

                    if (isNotEmpty(cred.host) && isNotEmpty(cred.port)) {
                        if (!isNotEmpty(cred.dbname)) {
                            cred.dbname = "DB " + (credentials.size() + 1);
                        }
                        credentials.add(cred);
                    }
                }
            } catch (DocumentException e) {
                logger.error("Cannot read '" + xmlPath + "'. Monitor is running without additional credentials");
            }
        }
    }

    /**
     * Returns the metric to the AppDynamics Controller.
     *
     * @param metricName  Name of the Metric
     * @param metricValue Value of the Metric
     * @param aggregation Average OR Observation OR Sum
     * @param timeRollup  Average OR Current OR Sum
     * @param cluster     Collective OR Individual
     */
    private void printMetric(
            String metricName,
            Object metricValue,
            String aggregation,
            String timeRollup,
            String cluster) {
        logger.info("Sending [" + getMetricPrefix() + metricName + "]");

        MetricWriter metricWriter = getMetricWriter(getMetricPrefix() + metricName, aggregation, timeRollup, cluster);
        if (metricValue instanceof Double) {
            metricWriter.printMetric(String.valueOf(Math.round((Double) metricValue)));
        } else if (metricValue instanceof Float) {
            metricWriter.printMetric(String.valueOf(Math.round((Float) metricValue)));
        } else {
            metricWriter.printMetric(String.valueOf(metricValue));
        }
    }

    /**
     * Metric Prefix
     *
     * @return Metric Location in the Controller (String)
     */
    private static String getMetricPrefix() {
        return CUSTOM_METRICS_H_BASE_STATUS;
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Map<String, String> args1 = new HashMap<String, String>();
        args1.put("host", "localhost");
        args1.put("port", "10101");

        new HBaseMonitor().execute(args1, null);
    }

    public class Credential {
        public String dbname;
        public String host;
        public String port;
        public String username;
        public String password;
    }

    private static boolean isNotEmpty(final String input) {
        return input != null && !input.trim().isEmpty();
    }
}
