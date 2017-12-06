package com.appdynamics.monitors.hbase;


import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;

public class DictionaryGenerator {

    public static Set<String> createIncludeDictionaryWithDefaults() {
        return Sets.newHashSet("storeCount", "storeFileCount", "storeFileIndexSize", "ProcessCallTime_num_ops");
    }

    public static List<String> createExcludeDictionary() {
        return Lists.newArrayList("storeCount", "storeFileIndexSize", "AppendSize_min");
    }
}
