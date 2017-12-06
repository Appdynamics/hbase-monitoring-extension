package com.appdynamics.monitors.hbase.filters;


import java.util.List;
import java.util.Set;

public class IncludeFilter {

    private Set<String> dictionary;

    public IncludeFilter(Set<String> includeDictionary) {
        this.dictionary = includeDictionary;
    }

    public void apply(Set<String> filteredSet, List<String> allMetrics) {
        if (allMetrics == null || dictionary == null) {
            return;
        }
        for (String metric : allMetrics) {
            if (dictionary.contains(metric)) {
                filteredSet.add(metric);
            }
        }
    }
}
