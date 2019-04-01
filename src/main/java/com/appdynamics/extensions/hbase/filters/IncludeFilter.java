/*
 *   Copyright 2019. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.hbase.filters;


import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class IncludeFilter {

    private Set<String> metricKeys;

    public IncludeFilter(Set<String> metricKeys) {
        this.metricKeys = metricKeys;
    }

    public List<String> apply(List<String> allMetrics) {
        List<String> filteredSet = new ArrayList<>();
        if (allMetrics == null || metricKeys == null) {
            return Lists.newArrayList();
        }
        for (String metric : allMetrics) {
            if (metricKeys.contains(metric)) {
                filteredSet.add(metric);
            }
        }
        return filteredSet;
    }
}
