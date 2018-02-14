/*
 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */


package com.appdynamics.monitors.hbase;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.util.AssertUtils;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.slf4j.LoggerFactory;

import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HBaseMonitor extends ABaseMonitor {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(HBaseMonitor.class);
    private static final String CONFIG_ARG = "config-file";
    private static final String METRIC_PREFIX = "Custom Metrics|HBase|";


    @Override
    protected String getDefaultMetricPrefix() {
        return METRIC_PREFIX;
    }

    @Override
    public String getMonitorName() {
        return "HBase Monitor1";
    }

    @Override
    protected void doRun(TasksExecutionServiceProvider serviceProvider) {
        List<Map<String, String>> instances = (List<Map<String, String>>) configuration.getConfigYml().get("instances");
        AssertUtils.assertNotNull(instances, "The 'instances' section in config.yml is not initialised");
        for (Map server : instances) {
            HBaseMonitorTask task = new HBaseMonitorTask(serviceProvider, server);
            serviceProvider.submit((String) server.get("name"), task);
        }
    }

    @Override
    protected int getTaskCount() {
        List<Map<String, String>> instances = (List<Map<String, String>>) configuration.getConfigYml().get("instances");
        AssertUtils.assertNotNull(instances, "The 'instances' section in config.yml is not initialised");
        return instances.size();
    }

    public static void main(String[] args) throws TaskExecutionException {

        ConsoleAppender ca = new ConsoleAppender();
        ca.setWriter(new OutputStreamWriter(System.out));
        ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
        ca.setThreshold(Level.DEBUG);

        org.apache.log4j.Logger.getRootLogger().addAppender(ca);

        final HBaseMonitor monitor = new HBaseMonitor();

        final Map<String, String> taskArgs = new HashMap<String, String>();
        taskArgs.put(CONFIG_ARG, "/Users/Muddam/AppDynamics/Code/extensions/hbase-monitoring-extension/src/main/resources/conf/config.yaml");

        //monitor.execute(taskArgs, null);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    monitor.execute(taskArgs, null);
                } catch (Exception e) {
                    logger.error("Error while running the task", e);
                }
            }
        }, 2, 30, TimeUnit.SECONDS);

    }
}