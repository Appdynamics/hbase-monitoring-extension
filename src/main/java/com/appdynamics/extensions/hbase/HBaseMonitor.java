/*
 *   Copyright 2019. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */


package com.appdynamics.extensions.hbase;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.hbase.Config.Stats;
import static com.appdynamics.extensions.hbase.Util.Constants.DISPLAY_NAME;
import static com.appdynamics.extensions.hbase.Util.Constants.METRIC_PREFIX;
import static com.appdynamics.extensions.hbase.Util.Constants.MonitorName;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.util.AssertUtils;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

public class HBaseMonitor extends ABaseMonitor {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(HBaseMonitor.class);
    private MonitorContextConfiguration monitorContextConfiguration;

    @Override
    protected String getDefaultMetricPrefix() {
        return METRIC_PREFIX;
    }

    @Override
    public String getMonitorName() {
        return MonitorName;
    }

    @Override
    protected void doRun(TasksExecutionServiceProvider serviceProvider) {
        try {
            List<Map<String, ?>> servers = getServers();
            AssertUtils.assertNotNull(monitorContextConfiguration.getMetricsXml(), "Metrics xml not available");
            for (Map<String, ?> server : servers) {
                AssertUtils.assertNotNull(server, "the server arguments are empty");
                AssertUtils.assertNotNull(server.get(DISPLAY_NAME), DISPLAY_NAME + " can not be null in the config.yml");
                logger.info("Starting the Hbase Monitoring Task for server : " + server.get(DISPLAY_NAME));
                HBaseMonitorTask task = new HBaseMonitorTask(monitorContextConfiguration, serviceProvider.getMetricWriteHelper(), server);
                serviceProvider.submit((String) server.get(DISPLAY_NAME), task);
            }
        } catch (Exception e) {
            logger.error("HBase monitoring extension errored-out while processing the servers", e);
        }
    }

    @Override
    protected List<Map<String, ?>> getServers() {
        Map<String, ?> config = monitorContextConfiguration.getConfigYml();
        AssertUtils.assertNotNull(config, "The config is not loaded due to previous error");
        List<Map<String, ?>> servers = (List<Map<String, ?>>) config.get("servers");
        AssertUtils.assertNotNull(servers, "The 'instances' section in config.yml is not initialised");
        return servers;
    }

    @Override
    protected void initializeMoreStuff(Map<String, String> args) {
        monitorContextConfiguration = getContextConfiguration();
        logger.info("initializing metric.xml file");
        monitorContextConfiguration.setMetricXml(args.get("metric-file"), Stats.class);
    }

//    public static void main(String[] args) {
//
//        ConsoleAppender ca = new ConsoleAppender();
//        ca.setWriter(new OutputStreamWriter(System.out));
//        ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
//        ca.setThreshold(Level.DEBUG);
//
//        org.apache.log4j.Logger.getRootLogger().addAppender(ca);
//
//        final Map<String, String> taskArgs = new HashMap<>();
//        taskArgs.put("config-file", "src/main/resources/conf/config.yml");
//        taskArgs.put("metric-file", "src/main/resources/conf/metrics.xml");
//        try {
//            final HBaseMonitor monitor = new HBaseMonitor();
//            monitor.execute(taskArgs, null);
//        } catch (Exception e) {
//            logger.error("Error while running the task", e);
//        }
//    }

}
