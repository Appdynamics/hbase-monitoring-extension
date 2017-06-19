/**
 * Copyright 2013 AppDynamics
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.appdynamics.monitors.hbase;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.util.MetricWriteHelperFactory;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HBaseMonitor extends AManagedMonitor {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(HBaseMonitor.class);
    private static final String CONFIG_ARG = "config-file";
    private static final String METRIC_PREFIX = "Custom Metrics|HBase|";


    private boolean initialized;
    private MonitorConfiguration configuration;

    public HBaseMonitor() {
        System.out.println(logVersion());
    }

    public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext out) throws TaskExecutionException {
        logVersion();
        if (!initialized) {
            initialize(taskArgs);
        }
        logger.debug("The raw arguments are {}", taskArgs);
        configuration.executeTask();
        logger.info("HBase monitor run completed successfully.");
        return new TaskOutput("HBase monitor run completed successfully.");

    }

    private void initialize(Map<String, String> taskArgs) {
        if (!initialized) {
            //read the config.
            final String configFilePath = taskArgs.get(CONFIG_ARG);
            MetricWriteHelper metricWriteHelper = MetricWriteHelperFactory.create(this);
            MonitorConfiguration conf = new MonitorConfiguration(METRIC_PREFIX, new TaskRunnable(), metricWriteHelper);
            conf.setConfigYml(configFilePath);
            conf.checkIfInitialized(MonitorConfiguration.ConfItem.CONFIG_YML, MonitorConfiguration.ConfItem.EXECUTOR_SERVICE,
                    MonitorConfiguration.ConfItem.METRIC_PREFIX, MonitorConfiguration.ConfItem.METRIC_WRITE_HELPER);
            this.configuration = conf;
            initialized = true;
        }
    }

    private class TaskRunnable implements Runnable {

        public void run() {
            Map<String, ?> config = configuration.getConfigYml();
            if (config != null) {
                List<Map> servers = (List<Map>) config.get(ConfigConstants.INSTANCES);
                if (servers != null && !servers.isEmpty()) {
                    for (Map server : servers) {
                        try {
                            HBaseMonitorTask task = createTask(server);
                            configuration.getExecutorService().execute(task);
                        } catch (IOException e) {
                            logger.error("Cannot construct JMX uri for {}", Util.convertToString(server.get(ConfigConstants.DISPLAY_NAME), ""));
                        }

                    }
                } else {
                    logger.error("There are no servers configured");
                }
            } else {
                logger.error("The config.yml is not loaded due to previous errors.The task will not run");
            }
        }
    }

    private HBaseMonitorTask createTask(Map server) throws IOException {
        return new HBaseMonitorTask.Builder()
                .metricPrefix(configuration.getMetricPrefix())
                .metricWriter(configuration.getMetricWriter())
                .server(server)
                .mbeans((Map<String, List<Map>>) configuration.getConfigYml().get(ConfigConstants.MBEANS))
                .build();
    }


    private static String getImplementationVersion() {
        return HBaseMonitor.class.getPackage().getImplementationTitle();
    }

    private String logVersion() {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        return msg;
    }

    public static void main(String[] args) throws TaskExecutionException {

        ConsoleAppender ca = new ConsoleAppender();
        ca.setWriter(new OutputStreamWriter(System.out));
        ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
        ca.setThreshold(Level.DEBUG);

        org.apache.log4j.Logger.getRootLogger().addAppender(ca);

        HBaseMonitor monitor = new HBaseMonitor();

        Map<String, String> taskArgs = new HashMap<String, String>();
        taskArgs.put(CONFIG_ARG, "/Users/Muddam/AppDynamics/Code/extensions/hbase-monitoring-extension/src/main/resources/conf/config.yaml");

        monitor.execute(taskArgs, null);

    }
}
