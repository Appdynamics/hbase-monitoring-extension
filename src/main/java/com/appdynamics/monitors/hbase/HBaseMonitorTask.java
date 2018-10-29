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
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.monitors.hbase.metrics.JMXMetricCollector;
import com.google.common.collect.Lists;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

class HBaseMonitorTask implements AMonitorTaskRunnable {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(HBaseMonitorTask.class);

    private MonitorConfiguration configuration;

    private String displayName;

    /* server properties */
    private Map server;

    /* a facade to report metrics to the machine agent.*/
    private MetricWriteHelper metricWriter;

    /* config mbeans from config.yml. */
    private Map<String, List<Map>> configMBeans;


    HBaseMonitorTask(TasksExecutionServiceProvider serviceProvider, Map server) {
        this.configuration = serviceProvider.getMonitorConfiguration();
        this.server = server;
        this.metricWriter = serviceProvider.getMetricWriteHelper();
        configMBeans = (Map<String, List<Map>>) configuration.getConfigYml().get(ConfigConstants.MBEANS);
    }

    public void run() {

        displayName = Util.convertToString(server.get(ConfigConstants.DISPLAY_NAME), "");
        String metricPrefix = configuration.getMetricPrefix();
        long startTime = System.currentTimeMillis();

        List<Metric> metrics = new ArrayList<Metric>();
        List<FutureTask<List<Metric>>> futureTaskList = Lists.newArrayList();
        try {
            logger.debug("HBase monitor thread for server {} started.", displayName);

            List<Map> masterAllMbeans = new ArrayList();
            List<Map> commonMBeans = configMBeans.get("common");
            if (commonMBeans != null) {
                masterAllMbeans.addAll(commonMBeans);
            }

            List<Map> masterMBeans = configMBeans.get("master");
            if (masterMBeans != null) {
                masterAllMbeans.addAll(masterMBeans);
            }

            String masterMetricPrefix = metricPrefix + "|" + displayName + "|Master|";
            JMXMetricCollector masterJmxCollector = new JMXMetricCollector(server, masterAllMbeans, masterMetricPrefix);
            FutureTask<List<Metric>> masterTaskExecutor = new FutureTask<>(masterJmxCollector);
            configuration.getExecutorService().submit(displayName + " metric collection Task", masterTaskExecutor);
            futureTaskList.add(masterTaskExecutor);
            logger.debug("starting future task for {}", displayName);

            List<Map> regionServers = (List<Map>) server.get(ConfigConstants.REGIONSERVERS);

            if (regionServers == null || regionServers.size() <= 0) {
                logger.info("No region servers defined. Not collecting region server metrics");
            }
            else{
                List<Map> regionServerAllMbeans = new ArrayList<Map>();
                if (commonMBeans != null) {
                    regionServerAllMbeans.addAll(commonMBeans);
                }


                List<Map> regionServerMbeans = configMBeans.get("regionServer");
                if (regionServerMbeans != null) {
                    regionServerAllMbeans.addAll(regionServerMbeans);
                }
                for (Map regionServer : regionServers) {

                    String regionServerDisplayName = Util.convertToString(regionServer.get(ConfigConstants.DISPLAY_NAME), "");
                    String regionServerMetricPrefix = metricPrefix + "|" + displayName + "|RegionServer|" + regionServerDisplayName + "|";
                    JMXMetricCollector regionServerJmxCollector = new JMXMetricCollector(regionServer, regionServerAllMbeans, regionServerMetricPrefix);
                    FutureTask<List<Metric>> regionTaskExecutor = new FutureTask<>(regionServerJmxCollector);
                    configuration.getExecutorService().submit(regionServerDisplayName + " metric collection Task", regionTaskExecutor);
                    futureTaskList.add(regionTaskExecutor);
                    logger.debug("starting future task for region {}", regionServerDisplayName);
                }
            }

            //collect all the metrics
            for(FutureTask task : futureTaskList) {
                try {
                    List<Metric> taskMetrics = (List<Metric>) task.get();
                    metrics.addAll(taskMetrics);
                } catch (InterruptedException var6) {
                    logger.error("Task interrupted. ", var6);
                } catch (ExecutionException var7) {
                    logger.error("Task execution failed. ", var7);
                }
            }
            //Wait for all tasks to finish
            if (metrics.size() > 0) {
                metricWriter.transformAndPrintMetrics(metrics);
            }

        } catch (Exception e) {
            logger.error("Error in HBase Monitor thread for server {}", displayName, e);

        } finally {
            long endTime = System.currentTimeMillis() - startTime;
            logger.debug("HBase monitor thread for server {} ended. Time taken = {}", displayName, endTime);
        }
    }

    public void onTaskComplete() {
        logger.info("All tasks for server {} finished", displayName);
    }
}
