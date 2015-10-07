/**
 * Copyright 2013 AppDynamics
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.appdynamics.monitors.hbase;

import com.appdynamics.extensions.ArgumentsValidator;
import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.base.Strings;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HBaseMonitor extends AManagedMonitor {
    private static Logger logger = Logger.getLogger(HBaseMonitor.class);

    private static final String CUSTOM_METRICS_H_BASE_STATUS = "Custom Metrics|HBase|";

    private static final String CONFIG_ARG = "config-file";

    private static final Map<String, String> defaultArgs = new HashMap<String, String>() {{
        put(CONFIG_ARG, "monitors/HBaseMonitor/config.yaml");
    }};

    public HBaseMonitor() {
        String msg = getVersionInfo();
        logger.info(msg);
        System.out.println(msg);
    }

    private String getVersionInfo() {
        String details = HBaseMonitor.class.getPackage().getImplementationTitle();
        return "Using Monitor Version [" + details + "]";
    }

    /**
     * Main execution method that uploads the metrics to the AppDynamics Controller
     *
     * @see com.singularity.ee.agent.systemagent.api.ITask#execute(java.util.Map, com.singularity.ee.agent.systemagent.api.TaskExecutionContext)
     */
    public TaskOutput execute(Map<String, String> args, TaskExecutionContext arg1) throws TaskExecutionException {
        try {


            logger.info("HBase monitor started collecting stats");
            logger.info(getVersionInfo());

            args = ArgumentsValidator.validateArguments(args, defaultArgs);

            String configFilename = getConfigFilename(args.get(CONFIG_ARG));
            Configuration config = YmlReader.readFromFile(configFilename, Configuration.class);

            List<HBase> hbaseConfig = config.getHbase();

            if(hbaseConfig == null || hbaseConfig.size() <= 0) {
                logger.info("No HBase configuration found. Exiting the process");
                throw new TaskExecutionException("No HBase configuration found. Exiting the process");
            }

            ExecutorService executor = Executors.newFixedThreadPool(hbaseConfig.size());

            CompletionService<Map<String, Object>> threadPool =
                    new ExecutorCompletionService<Map<String, Object>>(executor);

            for (HBase hBase : hbaseConfig) {
                threadPool.submit(new HBaseCommunicator(hBase));
            }

            for (int i = 0; i < hbaseConfig.size(); i++) {
                try {
                    Map<String, Object> metrics = threadPool.take().get();


                        for (Map.Entry<String, Object> metric : metrics.entrySet()) {
                            printMetric(metric.getKey(), metric.getValue(),
                                    MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                                    MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                                    MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
                        }

                } catch (Exception e) {
                    logger.error("Failed to get metrics", e);
                }
            }
            executor.shutdown();

            logger.info("HBase monitor finished collecting stats");

            return new TaskOutput("HBase Metric Upload Complete");
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
            return new TaskOutput("HBase Metric Upload Failed!");
        }
    }

    private String getConfigFilename(String filename) {
        if (filename == null) {
            return "";
        }
        // for absolute paths
        if (new File(filename).exists()) {
            return filename;
        }
        // for relative paths
        File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
        String configFileName = "";
        if (!Strings.isNullOrEmpty(filename)) {
            configFileName = jarPath + File.separator + filename;
        }
        return configFileName;
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

        logger.debug("Sending [" + getMetricPrefix() + metricName + "]");

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
}
