package com.appdynamics.monitors.hbase;

import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Test;

import java.util.Map;

/**
 * @author Satish Muddam
 */
public class HBaseMonitorTest {

    @Test
    public void test() throws TaskExecutionException {
        HBaseMonitor monitor = new HBaseMonitor();
        Map<String, String> taskArgs = Maps.newHashMap();
        taskArgs.put("config-file", "src/test/resources/conf/config.yaml");
        monitor.execute(taskArgs, null);
    }
}
