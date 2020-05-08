/*
 *   Copyright 2019. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.hbase;


import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.hbase.Config.MbeanObjectConfig;
import com.appdynamics.extensions.hbase.Config.Stats;
import com.appdynamics.extensions.hbase.Util.Constants;
import static com.appdynamics.extensions.hbase.Util.Constants.METRIC_SEPARATOR;
import com.appdynamics.extensions.hbase.Util.MbeanUtil;
import com.appdynamics.extensions.hbase.collector.JMXMetricCollector;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

class HBaseMonitorTask implements AMonitorTaskRunnable {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(HBaseMonitorTask.class);

    private MonitorContextConfiguration configuration;

    private String displayName;

    /* server properties */
    private Map<String, ?> server;

    /* a facade to report metrics to the machine agent.*/
    private MetricWriteHelper metricWriter;

    /* config mbeans from config.yml. */
    private Stats stats;

    private BigInteger heartBeatValue = BigInteger.ZERO;

    HBaseMonitorTask(MonitorContextConfiguration monitorContextConfiguration, MetricWriteHelper metricWriteHelper, Map<String, ?> server) {
        this.configuration = monitorContextConfiguration;
        this.server = server;
        this.metricWriter = metricWriteHelper;
        stats = (Stats) configuration.getMetricsXml();
    }

    public void run() {
        displayName = (String) server.get(Constants.DISPLAY_NAME);
        String metricPrefix = configuration.getMetricPrefix();
        long startTime = System.currentTimeMillis();
        List<Metric> metrics = Lists.newArrayList();
        try {
            processTask(metrics, metricPrefix);
        } catch (Exception e) {
            logger.error("Error in HBase Monitor thread for server {}", displayName, e);
        } finally {
            logger.debug("HBase monitor thread for server {} ended. Time taken = {}", displayName, System.currentTimeMillis() - startTime);
            Metric heartBeat = new Metric("HeartBeat", String.valueOf(heartBeatValue), metricPrefix + METRIC_SEPARATOR + "HeartBeat");
            metrics.add(heartBeat);
            metricWriter.transformAndPrintMetrics(metrics);
        }
    }

    private void processTask(List<Metric> metrics, String metricPrefix) {
        List<MbeanObjectConfig> masterMbeans = MbeanUtil.collectAllMasterMbeans(stats);

        String masterMetricPrefix = metricPrefix + METRIC_SEPARATOR + displayName + "|Master|";
        metrics.addAll(processServer(server, masterMbeans, masterMetricPrefix));
        List<Map<String, ?>> regionServers = (List<Map<String, ?>>) server.get(Constants.REGIONSERVERS);
        if (regionServers == null || regionServers.size() == 0) {
            logger.info("No region servers defined, not collecting region server metrics");
        } else {
            List<MbeanObjectConfig> regionServerMbeans = MbeanUtil.collectAllRegionMbeans(stats);
            String regionServerMetricPrefix = metricPrefix + METRIC_SEPARATOR + displayName + "|RegionServer|";
            for (Map<String, ?> regionServer : regionServers) {
                logger.info("Starting the Hbase Monitoring Task for region server : " + regionServer.get(Constants.DISPLAY_NAME));
                metrics.addAll(processServer(regionServer, regionServerMbeans, regionServerMetricPrefix + regionServer.get(Constants.DISPLAY_NAME) + METRIC_SEPARATOR));
            }
        }
        if (metrics.size() > 0)
            heartBeatValue = BigInteger.ONE;
        logger.info("HBase monitor JMX collector thread for server {} successfully completed.", displayName);
    }

    private List<Metric> processServer(Map<String, ?> server, List<MbeanObjectConfig> mbeans, String metricPrefix) {
        FutureTask<List<Metric>> task = initJMXCollector(server, mbeans, metricPrefix);
        return collectTaskMetrics(task);
    }

    private FutureTask<List<Metric>> initJMXCollector(Map<String, ?> server, List<MbeanObjectConfig> mbeans, String metricPrefix) {
        JMXMetricCollector serverJmxCollector = new JMXMetricCollector(server, mbeans,configuration, metricPrefix);
        FutureTask<List<Metric>> taskExecutor = new FutureTask<>(serverJmxCollector);
        configuration.getContext().getExecutorService().submit(server + " metric collection Task", taskExecutor);
        return taskExecutor;
    }

    private List<Metric> collectTaskMetrics(FutureTask<List<Metric>> futureTask) {
        List<Metric> metrics = Lists.newArrayList();
        try {
            List<Metric> taskMetrics = futureTask.get();
            metrics.addAll(taskMetrics);
        } catch (InterruptedException var6) {
            logger.error("Task interrupted. ", var6);
        } catch (ExecutionException var7) {
            logger.error("Task execution failed. ", var7);
        } catch (Exception e) {
            logger.error("Exception in future task. ", e);
        }
        return metrics;
    }

    public void onTaskComplete() {
        logger.info("All tasks for server {} finished", displayName);
    }
}
