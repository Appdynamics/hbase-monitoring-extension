package com.appdynamics.monitors.hbase.filters;

import com.appdynamics.monitors.hbase.DictionaryGenerator;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Set;

public class IncludeFilterTest {

    @Test
    public void whenAttribsMatch_thenIncludeMetrics() {
        Set dictionary = DictionaryGenerator.createIncludeDictionaryWithDefaults();
        List<String> metrics = Lists.newArrayList("MemHeapCommittedM", "storeCount", "storeFileCount");
        IncludeFilter filter = new IncludeFilter(dictionary);
        Set<String> filteredSet = Sets.newHashSet();
        filter.apply(filteredSet, metrics);
        Assert.assertTrue(filteredSet.contains("storeCount"));
        Assert.assertTrue(filteredSet.contains("storeFileCount"));
        Assert.assertTrue(!filteredSet.contains("MemHeapCommittedM"));
    }

    @Test
    public void whenNullDictionary_thenReturnUnchangedSet() {
        List<String> metrics = Lists.newArrayList("MemHeapCommittedM", "storeCount");
        IncludeFilter filter = new IncludeFilter(null);
        Set<String> filteredSet = Sets.newHashSet();
        filter.apply(filteredSet, metrics);
        Assert.assertTrue(filteredSet.size() == 0);
    }

    @Test
    public void whenEmptyDictionary_thenReturnUnchangedSet() {
        Set dictionary = Sets.newHashSet();
        List<String> metrics = Lists.newArrayList("MemHeapCommittedM", "storeCount");
        IncludeFilter filter = new IncludeFilter(dictionary);
        Set<String> filteredSet = Sets.newHashSet("storeCount");
        filter.apply(filteredSet, metrics);
        Assert.assertTrue(filteredSet.size() == 1);
    }
}
