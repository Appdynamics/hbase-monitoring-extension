/*
 *   Copyright 2019. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.hbase.metrics;

import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.hbase.Config.Stats;
import com.appdynamics.extensions.hbase.ConfigTestUtil;
import com.appdynamics.extensions.hbase.collector.JMXMetricCollector;
import com.appdynamics.extensions.metrics.Metric;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.powermock.api.mockito.PowerMockito.when;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
public class JMXMetricCollectorTest {

    @Mock
    JMXMetricCollector jmxMetricCollector;

    private Map<String, ?> configYml;
    private Stats stats;


    @Before
    public void initialize() {
        MonitorContextConfiguration configuration = ConfigTestUtil.getContextConfiguration("src/test/resources/conf/metrics.xml", "src/test/resources/conf/config.yml");
        stats = (Stats) configuration.getMetricsXml();
        configYml = configuration.getConfigYml();
    }

    @Test
    public void testJMXCollectorCallForMaster() throws IOException {
        List<Metric> masterMetric = ConfigTestUtil.readAllMetrics("src/test/resources/conf/masterMetrics.txt");
        when(jmxMetricCollector.call()).thenReturn(masterMetric);
        List<Metric> metrics = jmxMetricCollector.call();
        Assert.assertEquals(metrics.size(), 10);

    }

    @Test
    public void testJMXCollectorCallForRegionServer1() throws IOException {
        List<Metric> rs1Metrics = ConfigTestUtil.readAllMetrics("src/test/resources/conf/rsMetrics.txt").subList(0, 11);
        when(jmxMetricCollector.call()).thenReturn(rs1Metrics);
        List<Metric> metrics = jmxMetricCollector.call();
        Assert.assertEquals(metrics.size(), 11);
    }

    @Test
    public void testJMXCollectorCallForRegionServer2() throws IOException {
        List<Metric> rs2Metrics = ConfigTestUtil.readAllMetrics("src/test/resources/conf/rsMetrics.txt").subList(11, 22);
        when(jmxMetricCollector.call()).thenReturn(rs2Metrics);
        List<Metric> metrics = jmxMetricCollector.call();
        Assert.assertEquals(metrics.size(), 11);
    }

}