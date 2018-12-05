package com.appdynamics.monitors.hbase;

import com.appdynamics.extensions.AMonitorJob;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.monitors.hbase.Config.MbeanObjectConfig;
import com.appdynamics.monitors.hbase.Config.Stats;
import com.google.common.collect.Lists;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class ConfigTestUtil {

    public static MonitorContextConfiguration getContextConfiguration(String xmlPath, String configPath) {
        MonitorContextConfiguration configuration = new MonitorContextConfiguration("HbaseMonitor", "Custom Metrics|HBase|", Mockito.mock(File.class), Mockito.mock(AMonitorJob.class));
        if (configPath != null)
            configuration.setConfigYml(configPath);
        if (xmlPath != null)
            configuration.setMetricXml(xmlPath, Stats.class);
        return configuration;
    }

    public static List<MbeanObjectConfig> collectAllMasterMbeans(Stats stats){
        List<MbeanObjectConfig> mbeanObjectConfigs = Lists.newArrayList();
        List<MbeanObjectConfig> commonMBeansObject = stats.getMatchingMbeanConfig("common");
        Util.addAllValidMbeans(mbeanObjectConfigs, commonMBeansObject);
//            Picking zookeeper metrics for master only
        Util.addAllValidMbeans(mbeanObjectConfigs, stats.getMatchingMbeanConfig("zooKeeperService"));
        Util.addAllValidMbeans(mbeanObjectConfigs, stats.getMatchingMbeanConfig("master"));
        return mbeanObjectConfigs;
    }

    public static List<MbeanObjectConfig> collectAllRegionMbeans(Stats stats){
        List<MbeanObjectConfig> mbeanObjectConfigs = Lists.newArrayList();
        List<MbeanObjectConfig> commonMBeansObject = stats.getMatchingMbeanConfig("common");
        Util.addAllValidMbeans(mbeanObjectConfigs, commonMBeansObject);
        Util.addAllValidMbeans(mbeanObjectConfigs, stats.getMatchingMbeanConfig("regionServer"));
        return mbeanObjectConfigs;
    }

    public static List<Metric> readAllMetrics(String path) throws IOException {
        List<Metric> collectedMetrics = Lists.newArrayList();
        File demoFile = new File(path);
        try {
            BufferedReader br = new BufferedReader(new FileReader(demoFile));
            String line = "";
            while ((line = br.readLine()) != null) {
                collectedMetrics.add(convertLineToMetrics(line));
            }
        } catch (FileNotFoundException e) {
        }
        return collectedMetrics;
    }

    private static Metric convertLineToMetrics(String line){
        Pattern p = Pattern.compile(",");
        String[] currLine = p.split(line, -1);
        return new Metric("test", currLine[1], currLine[0]);
    }
}
