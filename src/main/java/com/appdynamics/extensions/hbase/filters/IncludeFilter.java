/*
 *   Copyright 2019. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.hbase.filters;


import com.appdynamics.extensions.hbase.Util.CommonUtil;
import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class IncludeFilter {

    private Set<String> metricKeys;

    public IncludeFilter(Set<String> metricKeys) {
        this.metricKeys = metricKeys;
    }

    public Set<String> apply(List<String> allMetrics) {
        Set<String> filteredSet = Sets.newHashSet();
        if (allMetrics == null || metricKeys == null) {
            return Collections.emptySet();
        }
        for (String metric : metricKeys) {
            if (CommonUtil.isCompositeObject(metric)) {
                metric = CommonUtil.getMetricNameFromCompositeObject(metric);
            }
            if (allMetrics.contains(metric)) {
                filteredSet.add(metric);
            }
        }
        return filteredSet;
    }
}
