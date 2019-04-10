/*
 *   Copyright 2019. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.hbase;

import com.appdynamics.extensions.AMonitorJob;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.hbase.Config.MbeanObjectConfig;
import com.appdynamics.extensions.hbase.Config.Stats;
import com.appdynamics.extensions.hbase.Util.MbeanUtil;
import com.appdynamics.extensions.metrics.Metric;
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

    // todo removed unused method
    public static List<MbeanObjectConfig> collectAllMasterMbeans(Stats stats){
        List<MbeanObjectConfig> mbeanObjectConfigs = Lists.newArrayList();
        List<MbeanObjectConfig> commonMBeansObject = stats.getMatchingMbeanConfig("common");
        MbeanUtil.addAllValidMbeans(mbeanObjectConfigs, commonMBeansObject);
//            Picking zookeeper metrics for master only
        MbeanUtil.addAllValidMbeans(mbeanObjectConfigs, stats.getMatchingMbeanConfig("zooKeeperService"));
        MbeanUtil.addAllValidMbeans(mbeanObjectConfigs, stats.getMatchingMbeanConfig("master"));
        return mbeanObjectConfigs;
    }

    // todo removed unused method
    public static List<MbeanObjectConfig> collectAllRegionMbeans(Stats stats){
        List<MbeanObjectConfig> mbeanObjectConfigs = Lists.newArrayList();
        List<MbeanObjectConfig> commonMBeansObject = stats.getMatchingMbeanConfig("common");
        MbeanUtil.addAllValidMbeans(mbeanObjectConfigs, commonMBeansObject);
        MbeanUtil.addAllValidMbeans(mbeanObjectConfigs, stats.getMatchingMbeanConfig("regionServer"));
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
