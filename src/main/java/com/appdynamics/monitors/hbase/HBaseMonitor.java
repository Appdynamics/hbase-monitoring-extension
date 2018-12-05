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
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.util.AssertUtils;
import com.appdynamics.monitors.hbase.Config.Stats;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class HBaseMonitor extends ABaseMonitor {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(HBaseMonitor.class);
    private static final String CONFIG_ARG = Constant.CONFIG_ARG;
    private static final String METRIC_PREFIX = Constant.METRIC_PREFIX;
    private static final String METRIC_ARG = Constant.METRIC_ARG;
    private MonitorContextConfiguration monitorContextConfiguration;
    private Map<String, ?> configYml;


    @Override
    protected String getDefaultMetricPrefix() {
        return METRIC_PREFIX;
    }

    @Override
    public String getMonitorName() {
        return Constant.MonitorName;
    }

    @Override
    protected void doRun(TasksExecutionServiceProvider serviceProvider) {
        List<Map<String, String>> instances = (List<Map<String, String>>) configYml.get("instances");
        AssertUtils.assertNotNull(instances, "The 'instances' section in config.yml is not initialised");
        for (Map server : instances) {
            server.put("encryptionKey", configYml.get("encryptionKey"));
            HBaseMonitorTask task = new HBaseMonitorTask(monitorContextConfiguration, serviceProvider.getMetricWriteHelper(), server);
            serviceProvider.submit((String) server.get("name"), task);
        }
    }

    @Override
    protected int getTaskCount() {
        List<Map<String, String>> instances = (List<Map<String, String>>) monitorContextConfiguration.getConfigYml().get("instances");
        AssertUtils.assertNotNull(instances, "The 'instances' section in config.yml is not initialised");
        return instances.size();
    }

    @Override
    protected void initializeMoreStuff(Map<String, String> args) {

        monitorContextConfiguration = getContextConfiguration();
        configYml = monitorContextConfiguration.getConfigYml();
        logger.info("initializing metric.xml file");
        this.getContextConfiguration().setMetricXml(args.get("metric-file"), Stats.class);
    }

//    public static void main(String[] args) throws TaskExecutionException {
//
//        ConsoleAppender ca = new ConsoleAppender();
//        ca.setWriter(new OutputStreamWriter(System.out));
//        ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
//        ca.setThreshold(Level.DEBUG);
//
//        org.apache.log4j.Logger.getRootLogger().addAppender(ca);
//
//        final Map<String, String> taskArgs = new HashMap<String, String>();
//        taskArgs.put(CONFIG_ARG, "src/main/resources/conf/config.yml");
//        taskArgs.put(METRIC_ARG, "src/main/resources/conf/metrics.xml");
//        try {
//            final HBaseMonitor monitor = new HBaseMonitor();
//            monitor.execute(taskArgs, null);
//        } catch (Exception e) {
//            logger.error("Error while running the task", e);
//        }
//    }

}
