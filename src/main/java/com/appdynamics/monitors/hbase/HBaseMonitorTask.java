/*
 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.monitors.hbase;


import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.monitors.hbase.Config.MbeanObjectConfig;
import com.appdynamics.monitors.hbase.Config.Stats;
import static com.appdynamics.monitors.hbase.Constant.METRIC_SEPARATOR;
import com.appdynamics.monitors.hbase.metrics.JMXMetricCollector;
import com.google.common.collect.Lists;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

class HBaseMonitorTask implements AMonitorTaskRunnable {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(HBaseMonitorTask.class);

    private MonitorContextConfiguration configuration;

    private String displayName;

    /* server properties */
    private Map server;

    /* a facade to report metrics to the machine agent.*/
    private MetricWriteHelper metricWriter;

    /* config mbeans from config.yml. */
    private Stats stats;

    private List<FutureTask<List<Metric>>> futureTaskList = Lists.newArrayList();
    private BigInteger heartBeatValue = BigInteger.ZERO;

    HBaseMonitorTask(MonitorContextConfiguration monitorContextConfiguration, MetricWriteHelper metricWriteHelper, Map server) {
        this.configuration = monitorContextConfiguration;
        this.server = server;
        this.metricWriter = metricWriteHelper;
        stats = (Stats) configuration.getMetricsXml();
    }

    public void run() {
        displayName = Util.convertToString(server.get(Constant.DISPLAY_NAME), "");
        String metricPrefix = configuration.getMetricPrefix();
        long startTime = System.currentTimeMillis();
        List<Metric> metrics = Lists.newArrayList();
        try {
            logger.debug("HBase monitor thread for server {} started.", displayName);

            List<MbeanObjectConfig> masterAllMbeans = Lists.newArrayList();
            List<MbeanObjectConfig> commonMBeansObject = stats.getMatchingMbeanConfig("common");
            Util.addAllValidMbeans(masterAllMbeans, commonMBeansObject);
//            Picking zookeeper metrics for master only
            Util.addAllValidMbeans(masterAllMbeans, stats.getMatchingMbeanConfig("zooKeeperService"));
            Util.addAllValidMbeans(masterAllMbeans, stats.getMatchingMbeanConfig("master"));

            String masterMetricPrefix = metricPrefix + METRIC_SEPARATOR + displayName + "|Master|";
            initJMXCollector(server, masterAllMbeans, masterMetricPrefix);

            List<Map> regionServers = (List<Map>) server.get(Constant.REGIONSERVERS);
            if (regionServers == null || regionServers.size() <= 0) {
                logger.info("No region servers defined. Not collecting region server metrics");
            } else {
                List<MbeanObjectConfig> regionServerAllMbeans = Lists.newArrayList();
                Util.addAllValidMbeans(regionServerAllMbeans, commonMBeansObject);
                Util.addAllValidMbeans(regionServerAllMbeans, stats.getMatchingMbeanConfig("regionServer"));
                String regionServerMetricPrefix = metricPrefix + METRIC_SEPARATOR + displayName + "|RegionServer|";
                for (Map regionServer : regionServers) {
                    regionServer.put("encryptionKey", server.get("encryptionKey"));
                    initJMXCollector(regionServer, regionServerAllMbeans, regionServerMetricPrefix + regionServer.get(Constant.DISPLAY_NAME) + METRIC_SEPARATOR);
                }
            }

            metrics = collectTaskMetrics();
            logger.info("HBase monitor JMX collector thread for server {} successfully completed.", displayName);
        } catch (Exception e) {
            logger.error("Error in HBase Monitor thread for server {}", displayName, e);

        } finally {
            long endTime = System.currentTimeMillis() - startTime;
            logger.debug("HBase monitor thread for server {} ended. Time taken = {}", displayName, endTime);
            String prefix = metricPrefix + METRIC_SEPARATOR + "HeartBeat";
            Metric heartBeat = new Metric("HeartBeat", String.valueOf(heartBeatValue), prefix);
            metrics.add(heartBeat);
            metricWriter.transformAndPrintMetrics(metrics);
        }
    }

    private void initJMXCollector(Map server, List<MbeanObjectConfig> mbeans, String metricPrefix) {
        JMXMetricCollector serverJmxCollector = new JMXMetricCollector(server, mbeans, metricPrefix, configuration);
        FutureTask<List<Metric>> taskExecutor = new FutureTask<>(serverJmxCollector);
        configuration.getContext().getExecutorService().submit(server + " metric collection Task", taskExecutor);
        futureTaskList.add(taskExecutor);
        logger.debug("starting future task for {}", server.get(Constant.DISPLAY_NAME));
    }

    private List<Metric> collectTaskMetrics() {
        List<Metric> metrics = Lists.newArrayList();
        for (FutureTask task : futureTaskList) {
            try {
                List<Metric> taskMetrics = (List<Metric>) task.get();
                metrics.addAll(taskMetrics);
                heartBeatValue = BigInteger.ONE;
            } catch (InterruptedException var6) {
                logger.error("Task interrupted. ", var6);
            } catch (ExecutionException var7) {
                logger.error("Task execution failed. ", var7);
            } catch (Exception e) {
                logger.error("Exception in future task. ", e);
            }

        }
        return metrics;
    }

    public void onTaskComplete() {
        logger.info("All tasks for server {} finished", displayName);
    }
}
