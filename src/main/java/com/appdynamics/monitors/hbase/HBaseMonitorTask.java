package com.appdynamics.monitors.hbase;


import com.appdynamics.extensions.util.AggregatorFactory;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.monitors.hbase.metrics.ClusterMetricsProcessor;
import com.appdynamics.monitors.hbase.metrics.Metric;
import com.appdynamics.monitors.hbase.metrics.MetricPrinter;
import com.appdynamics.monitors.hbase.metrics.NodeMetricsProcessor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

class HBaseMonitorTask implements Runnable {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(HBaseMonitorTask.class);
    private static final String METRICS_COLLECTION_SUCCESSFUL = "Metrics Collection Successful";
    private static final BigDecimal ERROR_VALUE = BigDecimal.ZERO;
    private static final BigDecimal SUCCESS_VALUE = BigDecimal.ONE;

    private String displayName;
    /* metric prefix from the config.yaml to be applied to each metric path*/
    private String metricPrefix;

    /* server properties */
    private Map server;

    /* a facade to report metrics to the machine agent.*/
    private MetricWriteHelper metricWriter;

    /* config mbeans from config.yaml. */
    private Map<String, List<Map>> configMBeans;

    /* a utility to collect cluster metrics. */
    private final ClusterMetricsProcessor clusterMetricsCollector = new ClusterMetricsProcessor();


    private HBaseMonitorTask() {
    }

    public void run() {

        displayName = Util.convertToString(server.get(ConfigConstants.DISPLAY_NAME), "");
        long startTime = System.currentTimeMillis();
        MetricPrinter metricPrinter = new MetricPrinter(metricPrefix, displayName, metricWriter);
        try {
            logger.debug("HBase monitor thread for server {} started.", displayName);

            BigDecimal status = extractAndReportMetrics(metricPrinter);
            metricPrinter.printMetric(metricPrinter.formMetricPath(METRICS_COLLECTION_SUCCESSFUL), status
                    , MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
        } catch (Exception e) {
            logger.error("Error in HBase Monitor thread for server {}", displayName, e);
            metricPrinter.printMetric(metricPrinter.formMetricPath(METRICS_COLLECTION_SUCCESSFUL), ERROR_VALUE
                    , MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);

        } finally {
            long endTime = System.currentTimeMillis() - startTime;
            logger.debug("HBase monitor thread for server {} ended. Time taken = {} and Total metrics reported = {}", displayName, endTime, metricPrinter.getTotalMetricsReported());
        }
    }


    private BigDecimal extractAndReportMetrics(final MetricPrinter metricPrinter) throws Exception {
        try {
            logger.debug("JMX Connection is open");

            NodeMetricsProcessor nodeProcessor = new NodeMetricsProcessor(server, configMBeans);
            List<Metric> nodeMetrics = nodeProcessor.getMetrics();

            AggregatorFactory aggregatorFactory = new AggregatorFactory();
            clusterMetricsCollector.collect(aggregatorFactory, nodeMetrics);
            if (nodeMetrics.size() > 0) {
                metricPrinter.reportClusterLevelMetrics(aggregatorFactory);
                metricPrinter.reportNodeMetrics(nodeMetrics);
            }
        } catch (Exception e) {
            logger.error("Error while collection metrics from JMX", e);
            return ERROR_VALUE;
        }
        return SUCCESS_VALUE;
    }

    static class Builder {
        private HBaseMonitorTask task = new HBaseMonitorTask();

        Builder metricPrefix(String metricPrefix) {
            task.metricPrefix = metricPrefix;
            return this;
        }

        Builder metricWriter(MetricWriteHelper metricWriter) {
            task.metricWriter = metricWriter;
            return this;
        }

        Builder server(Map server) {
            task.server = server;
            return this;
        }

        Builder mbeans(Map<String, List<Map>> mBeans) {
            task.configMBeans = mBeans;
            return this;
        }

        HBaseMonitorTask build() {
            return task;
        }
    }
}
