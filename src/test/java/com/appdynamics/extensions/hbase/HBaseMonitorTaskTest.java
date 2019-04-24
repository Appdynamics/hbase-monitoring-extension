/*
 *   Copyright 2019. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.hbase;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorContext;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.conf.modules.MonitorExecutorServiceModule;
import com.appdynamics.extensions.executorservice.MonitorExecutorService;
import com.appdynamics.extensions.hbase.Config.Stats;
import com.appdynamics.extensions.hbase.Util.Constants;
import com.appdynamics.extensions.metrics.Metric;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.Matchers.anyList;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;
import java.util.Map;

/**
 * @author Satish Muddam
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({HBaseMonitorTask.class})
@PowerMockIgnore("javax.net.ssl.*")
public class HBaseMonitorTaskTest {

    @Mock
    private TasksExecutionServiceProvider serviceProvider;

    private Map server;
    @Mock
    private MonitorContextConfiguration monitorConfiguration;
    @Mock
    private MetricWriteHelper metricWriter;
    @Mock
    private MonitorContext context;
    private Map config;

    private Map<String, List<Map>> configMBeans;

    private ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);

    @Before
    public void setup(){
        MonitorContextConfiguration contextConfiguration = ConfigTestUtil.getContextConfiguration("src/test/resources/conf/metrics.xml", "src/test/resources/conf/config.yml");
        Stats stats = (Stats) contextConfiguration.getMetricsXml();
        config = contextConfiguration.getConfigYml();
        MonitorExecutorServiceModule executorServiceModule = Mockito.spy(new MonitorExecutorServiceModule());
        executorServiceModule.initExecutorService(config, "HBaseMonitor");
        MonitorExecutorService executorService = executorServiceModule.getExecutorService();
        List<Map<String, String>> instances = (List<Map<String, String>>) config.get("servers");
        server = instances.get(0);
        configMBeans = (Map<String, List<Map>>) config.get(Constants.MBEANS);

        Mockito.when(serviceProvider.getMetricWriteHelper()).thenReturn(metricWriter);
        Mockito.when(monitorConfiguration.getConfigYml()).thenReturn(config);
        Mockito.when(monitorConfiguration.getContext()).thenReturn(context);
        Mockito.when(monitorConfiguration.getMetricsXml()).thenReturn(stats);
        Mockito.when(monitorConfiguration.getMetricPrefix()).thenReturn("Custom Metrics|HBase|");
        Mockito.when(context.getExecutorService()).thenReturn(executorService);
    }

    @Test
    public void testMasterAndRegionServers() throws Exception {
        HBaseMonitorTask hBaseMonitorTask = new HBaseMonitorTask(monitorConfiguration, metricWriter, server);
        HBaseMonitorTask task = PowerMockito.spy(hBaseMonitorTask);
        List<Metric> metrics = ConfigTestUtil.readAllMetrics("src/test/resources/conf/metrics.txt");
        PowerMockito.doReturn(metrics).when(task,"processServer",null, null, null);

        task.run();

        verify(metricWriter).transformAndPrintMetrics(pathCaptor.capture());
        Assert.assertEquals(((List<Metric>)pathCaptor.getValue()).size(), 33);
        verify(metricWriter, times(1)).transformAndPrintMetrics(anyList());
    }

    @Test
    public void testMasterOnly() throws Exception {
        MonitorContextConfiguration contextConfiguration = ConfigTestUtil.getContextConfiguration("src/test/resources/conf/metrics.xml", "src/test/resources/conf/configMasterOnly.yml");
        Map masterConfig = contextConfiguration.getConfigYml();
        Map masterServer = ((List<Map<String, String>>) masterConfig.get("servers")).get(0);

        HBaseMonitorTask hBaseMonitorTask = new HBaseMonitorTask(monitorConfiguration, metricWriter, masterServer);
        HBaseMonitorTask task = PowerMockito.spy(hBaseMonitorTask);
        List<Metric> metrics = ConfigTestUtil.readAllMetrics("src/test/resources/conf/masterMetrics.txt");
        PowerMockito.doReturn(metrics).when(task,"processServer",null, null, null);

        task.run();

        verify(metricWriter).transformAndPrintMetrics(pathCaptor.capture());
        Assert.assertEquals(((List<Metric>)pathCaptor.getValue()).size(), 11);
        verify(metricWriter, times(1)).transformAndPrintMetrics(anyList());
    }

    @Test
    public void testRegionServerOnly() throws Exception {
        MonitorContextConfiguration contextConfiguration = ConfigTestUtil.getContextConfiguration("src/test/resources/conf/metrics.xml", "src/test/resources/conf/configRSOnly.yml");
        Map rsConfig = contextConfiguration.getConfigYml();
        Map regionServer = ((List<Map<String, String>>) rsConfig.get("servers")).get(0);

        HBaseMonitorTask hBaseMonitorTask = new HBaseMonitorTask(monitorConfiguration, metricWriter, regionServer);
        HBaseMonitorTask task = PowerMockito.spy(hBaseMonitorTask);
        List<Metric> metrics = ConfigTestUtil.readAllMetrics("src/test/resources/conf/rsMetrics.txt");
        PowerMockito.doReturn(metrics).when(task,"processServer",null, null, null);

        task.run();

        verify(metricWriter).transformAndPrintMetrics(pathCaptor.capture());
        Assert.assertEquals(((List<Metric>)pathCaptor.getValue()).size(), 23);
        verify(metricWriter, times(1)).transformAndPrintMetrics(anyList());
    }
}
