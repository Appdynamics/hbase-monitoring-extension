package com.appdynamics.monitors.hbase;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.MonitorExecutorService;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.yml.YmlReader;
import com.appdynamics.monitors.hbase.metrics.JMXMetricCollector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;

/**
 * @author Satish Muddam
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({HBaseMonitorTask.class})
public class HBaseMonitorTaskTest {

    @Mock
    private TasksExecutionServiceProvider serviceProvider;

    private Map server;
    @Mock
    private MonitorConfiguration monitorConfiguration;
    @Mock
    private MetricWriteHelper metricWriter;
    private Map config;
    @Mock
    private MonitorExecutorService executorService;

    @Mock
    private Phaser phaser;

    private Map<String, List<Map>> configMBeans;

    @Test
    public void testMasterAndRegionServers() throws Exception {

        config = YmlReader.readFromFileAsMap(new File("src/test/resources/conf/config.yaml"));

        List<Map<String, String>> instances = (List<Map<String, String>>) config.get("instances");
        server = instances.get(0);
        configMBeans = (Map<String, List<Map>>) config.get(ConfigConstants.MBEANS);

        PowerMockito.whenNew(Phaser.class)
                .withNoArguments().thenReturn(phaser);

        Mockito.when(serviceProvider.getMonitorConfiguration()).thenReturn(monitorConfiguration);
        Mockito.when(serviceProvider.getMetricWriteHelper()).thenReturn(metricWriter);
        Mockito.when(monitorConfiguration.getConfigYml()).thenReturn(config);
        Mockito.when(monitorConfiguration.getExecutorService()).thenReturn(executorService);

        HBaseMonitorTask hBaseMonitorTask = new HBaseMonitorTask(serviceProvider, server);

        hBaseMonitorTask.run();

        verify(executorService, times(3)).submit(anyString(), any(JMXMetricCollector.class));
    }

    @Test
    public void testMasterOnly() throws Exception {

        config = YmlReader.readFromFileAsMap(new File("src/test/resources/conf/configMasterOnly.yaml"));

        List<Map<String, String>> instances = (List<Map<String, String>>) config.get("instances");
        server = instances.get(0);
        configMBeans = (Map<String, List<Map>>) config.get(ConfigConstants.MBEANS);

        PowerMockito.whenNew(Phaser.class)
                .withNoArguments().thenReturn(phaser);

        Mockito.when(serviceProvider.getMonitorConfiguration()).thenReturn(monitorConfiguration);
        Mockito.when(serviceProvider.getMetricWriteHelper()).thenReturn(metricWriter);
        Mockito.when(monitorConfiguration.getConfigYml()).thenReturn(config);
        Mockito.when(monitorConfiguration.getExecutorService()).thenReturn(executorService);

        HBaseMonitorTask hBaseMonitorTask = new HBaseMonitorTask(serviceProvider, server);

        hBaseMonitorTask.run();

        verify(executorService, times(3)).submit(anyString(), any(JMXMetricCollector.class));
    }

    @Test
    public void testRegionServerOnly() throws Exception {

        config = YmlReader.readFromFileAsMap(new File("src/test/resources/conf/configRSOnly.yaml"));

        List<Map<String, String>> instances = (List<Map<String, String>>) config.get("instances");
        server = instances.get(0);
        configMBeans = (Map<String, List<Map>>) config.get(ConfigConstants.MBEANS);

        PowerMockito.whenNew(Phaser.class)
                .withNoArguments().thenReturn(phaser);

        Mockito.when(serviceProvider.getMonitorConfiguration()).thenReturn(monitorConfiguration);
        Mockito.when(serviceProvider.getMetricWriteHelper()).thenReturn(metricWriter);
        Mockito.when(monitorConfiguration.getConfigYml()).thenReturn(config);
        Mockito.when(monitorConfiguration.getExecutorService()).thenReturn(executorService);

        HBaseMonitorTask hBaseMonitorTask = new HBaseMonitorTask(serviceProvider, server);

        hBaseMonitorTask.run();

        verify(executorService, times(3)).submit(anyString(), any(JMXMetricCollector.class));
    }
}
