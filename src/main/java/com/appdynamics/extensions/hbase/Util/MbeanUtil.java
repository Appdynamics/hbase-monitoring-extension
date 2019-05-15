/*
 *   Copyright 2019. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.hbase.Util;


import com.appdynamics.extensions.hbase.Config.MbeanObjectConfig;
import com.appdynamics.extensions.hbase.Config.Stats;
import com.google.common.collect.Lists;

import java.util.List;

public class MbeanUtil {

    public static void addAllValidMbeans(List<MbeanObjectConfig> allMbeans, List<MbeanObjectConfig> fetchedMbeans) {
        if (fetchedMbeans != null && !fetchedMbeans.isEmpty())
            allMbeans.addAll(fetchedMbeans);
    }

    public static List<MbeanObjectConfig> collectAllMasterMbeans(Stats stats){
        List<MbeanObjectConfig> mbeanObjectConfigs = Lists.newArrayList();
        List<MbeanObjectConfig> commonMBeansObject = stats.getMatchingMbeanConfig("common");
        MbeanUtil.addAllValidMbeans(mbeanObjectConfigs, commonMBeansObject);
//            Picking zookeeper metrics for master only
        MbeanUtil.addAllValidMbeans(mbeanObjectConfigs, stats.getMatchingMbeanConfig("zooKeeperService"));
        MbeanUtil.addAllValidMbeans(mbeanObjectConfigs, stats.getMatchingMbeanConfig("master"));
        //Adding composite memory metrics for master only
        MbeanUtil.addAllValidMbeans(mbeanObjectConfigs, stats.getMatchingMbeanConfig("memory"));
        return mbeanObjectConfigs;
    }

    public static List<MbeanObjectConfig> collectAllRegionMbeans(Stats stats){
        List<MbeanObjectConfig> mbeanObjectConfigs = Lists.newArrayList();
        List<MbeanObjectConfig> commonMBeansObject = stats.getMatchingMbeanConfig("common");
        MbeanUtil.addAllValidMbeans(mbeanObjectConfigs, commonMBeansObject);
        MbeanUtil.addAllValidMbeans(mbeanObjectConfigs, stats.getMatchingMbeanConfig("regionServer"));
        return mbeanObjectConfigs;
    }
}
