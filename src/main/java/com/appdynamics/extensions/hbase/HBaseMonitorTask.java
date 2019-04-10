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
import com.appdynamics.extensions.hbase.Util.Constant;
import static com.appdynamics.extensions.hbase.Util.Constant.METRIC_SEPARATOR;
import com.appdynamics.extensions.hbase.Util.MbeanUtil;
import com.appdynamics.extensions.hbase.metrics.JMXMetricCollector;
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

    private List<FutureTask<List<Metric>>> futureTaskList = Lists.newArrayList();
    private BigInteger heartBeatValue = BigInteger.ZERO;

    HBaseMonitorTask(MonitorContextConfiguration monitorContextConfiguration, MetricWriteHelper metricWriteHelper, Map<String, ?> server) {
        this.configuration = monitorContextConfiguration;
        this.server = server;
        this.metricWriter = metricWriteHelper;
        stats = (Stats) configuration.getMetricsXml();
    }

    // todo: refactor this method
    // todo: collect everything from master, then the server, then its region server and then move to the next server
    // todo: each region server will create its own thread, so we want to be cautious of spawning new threads, refactoring needed
    public void run() {
        displayName = (String) server.get(Constant.DISPLAY_NAME);
        String metricPrefix = configuration.getMetricPrefix();
        long startTime = System.currentTimeMillis();
        List<Metric> metrics = Lists.newArrayList();
        try {
            List<MbeanObjectConfig> masterAllMbeans = Lists.newArrayList();
            List<MbeanObjectConfig> commonMBeansObject = stats.getMatchingMbeanConfig("common");
            MbeanUtil.addAllValidMbeans(masterAllMbeans, commonMBeansObject);
//            Picking zookeeper metrics for master only
            MbeanUtil.addAllValidMbeans(masterAllMbeans, stats.getMatchingMbeanConfig("zooKeeperService"));
            MbeanUtil.addAllValidMbeans(masterAllMbeans, stats.getMatchingMbeanConfig("master"));

            String masterMetricPrefix = metricPrefix + METRIC_SEPARATOR + displayName + "|Master|";
            initJMXCollector(server, masterAllMbeans, masterMetricPrefix);

            List<Map<String, ?>> regionServers = (List<Map<String, ?>>) server.get(Constant.REGIONSERVERS);
            if (regionServers == null || regionServers.size() == 0) {
                logger.info("No region servers defined, not collecting region server metrics");
            } else {
                List<MbeanObjectConfig> regionServerAllMbeans = Lists.newArrayList();
                MbeanUtil.addAllValidMbeans(regionServerAllMbeans, commonMBeansObject);
                MbeanUtil.addAllValidMbeans(regionServerAllMbeans, stats.getMatchingMbeanConfig(Constant.REGIONSERVERS));
                String regionServerMetricPrefix = metricPrefix + METRIC_SEPARATOR + displayName + "|RegionServer|";
                for (Map<String, ?> regionServer : regionServers) {
                    logger.info("Starting the Hbase Monitoring Task for region server : " + regionServer.get(Constant.DISPLAY_NAME));
                    initJMXCollector(regionServer, regionServerAllMbeans, regionServerMetricPrefix + regionServer.get(Constant.DISPLAY_NAME) + METRIC_SEPARATOR);
                }
            }

            metrics = collectTaskMetrics();
            if (metrics.size() > 0)
                heartBeatValue = BigInteger.ONE;
            logger.info("HBase monitor JMX collector thread for server {} successfully completed.", displayName);
        } catch (Exception e) {
            logger.error("Error in HBase Monitor thread for server {}", displayName, e);
        } finally {
            logger.debug("HBase monitor thread for server {} ended. Time taken = {}", displayName, System.currentTimeMillis() - startTime);
            Metric heartBeat = new Metric("HeartBeat", String.valueOf(heartBeatValue), metricPrefix + METRIC_SEPARATOR + "HeartBeat");
            metrics.add(heartBeat);
            metricWriter.transformAndPrintMetrics(metrics);
        }
    }

    private void initJMXCollector(Map<String, ?> server, List<MbeanObjectConfig> mbeans, String metricPrefix) {
        JMXMetricCollector serverJmxCollector = new JMXMetricCollector(server, mbeans, metricPrefix, configuration);
        FutureTask<List<Metric>> taskExecutor = new FutureTask<>(serverJmxCollector);
        configuration.getContext().getExecutorService().submit(server + " metric collection Task", taskExecutor);
        futureTaskList.add(taskExecutor);
        logger.debug("Added future task for {}", server.get(Constant.DISPLAY_NAME));
    }
    //todo: need to have some clarity on how many threads are being generated and if we can limit to one thread per main server instead of having each thread for every region server as well, but that would also include a discussion on how if we have a thread for each region server will impact the extension and the machine its running on.
    private List<Metric> collectTaskMetrics() {
        List<Metric> metrics = Lists.newArrayList();
        for (FutureTask task : futureTaskList) {
            try {
                List<Metric> taskMetrics = (List<Metric>) task.get();
                metrics.addAll(taskMetrics);
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
