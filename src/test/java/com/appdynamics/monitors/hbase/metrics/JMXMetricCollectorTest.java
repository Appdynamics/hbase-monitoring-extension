package com.appdynamics.monitors.hbase.metrics;

import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.monitors.hbase.Config.MbeanObjectConfig;
import com.appdynamics.monitors.hbase.Config.Stats;
import com.appdynamics.monitors.hbase.ConfigTestUtil;
import com.appdynamics.monitors.hbase.JMXConnectionAdapter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.management.remote.JMXConnector;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
public class JMXMetricCollectorTest {
    @Mock
    MonitorContextConfiguration contextConfiguration;
    @Mock
    JMXConnector jmxConnector;
    @Mock
    JMXConnectionAdapter jmxConnectionAdapter;
    private Map<String, ?> configYml;
    private Stats stats;


    @Before
    public void initialize() throws Exception{
        MonitorContextConfiguration configuration = ConfigTestUtil.getContextConfiguration("src/test/resources/conf/metrics.xml", "src/test/resources/conf/config.yml");
        stats = (Stats) configuration.getMetricsXml();
        configYml = (Map<String, String>) configuration.getConfigYml();
    }

    @Test
    public void testJMXCollectorCallForMaster(){
        Map server =  ((List<Map>)configYml.get("instances")).get(0);
        List<MbeanObjectConfig> mbeans = ConfigTestUtil.collectAllMasterMbeans(stats);
        JMXMetricCollector jmxMetricCollector = new JMXMetricCollector(server, mbeans, "Custom Metrics|HBase|", contextConfiguration);
        List<Metric> metrics = jmxMetricCollector.call();
        Assert.assertEquals(metrics.size(), 10);

    }

    @Test
    public void testJMXCollectorCallForRegionServer(){
        Map server = (Map) ((List)(((List<Map>)configYml.get("instances")).get(0).get("regionServers"))).get(0);

        List<MbeanObjectConfig> mbeans = ConfigTestUtil.collectAllRegionMbeans(stats);
        JMXMetricCollector jmxMetricCollector = new JMXMetricCollector(server, mbeans, "Custom Metrics|HBase|", contextConfiguration);
        List<Metric> metrics = jmxMetricCollector.call();
        Assert.assertEquals(metrics.size(), 11);
    }

    @Test
    public void testJMXCollectorCallForRegionServer2(){
        Map server = (Map) ((List)(((List<Map>)configYml.get("instances")).get(0).get("regionServers"))).get(1);

        List<MbeanObjectConfig> mbeans = ConfigTestUtil.collectAllRegionMbeans(stats);
        JMXMetricCollector jmxMetricCollector = new JMXMetricCollector(server, mbeans, "Custom Metrics|HBase|", contextConfiguration);
        List<Metric> metrics = jmxMetricCollector.call();
        Assert.assertEquals(metrics.size(), 11);

    }

}