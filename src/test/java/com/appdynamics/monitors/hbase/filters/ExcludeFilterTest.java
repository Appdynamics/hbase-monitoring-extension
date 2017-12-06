package com.appdynamics.monitors.hbase.filters;


import com.appdynamics.monitors.hbase.DictionaryGenerator;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Set;

public class ExcludeFilterTest {

    @Test
    public void whenValidDictionary_thenExcludeMetrics() {
        List dictionary = DictionaryGenerator.createExcludeDictionary();
        List<String> metrics = Lists.newArrayList("MemHeapCommittedM", "storeCount");
        ExcludeFilter filter = new ExcludeFilter(dictionary);
        Set<String> filteredSet = Sets.newHashSet();
        filter.apply(filteredSet, metrics);
        Assert.assertTrue(filteredSet.contains("MemHeapCommittedM"));
        Assert.assertTrue(!filteredSet.contains("storeCount"));
    }

    @Test
    public void whenNullDictioary_thenReturnUnchangedSet() {
        List<String> metrics = Lists.newArrayList("MemHeapCommittedM", "storeCount");
        ExcludeFilter filter = new ExcludeFilter(null);
        Set<String> filteredSet = Sets.newHashSet();
        filter.apply(filteredSet, metrics);
        Assert.assertTrue(filteredSet.size() == 0);
    }

    @Test
    public void whenEmptyDictionary_thenReturnUnchangedSet() {
        List dictionary = Lists.newArrayList();
        List<String> metrics = Lists.newArrayList("MemHeapCommittedM", "storeCount");
        ExcludeFilter filter = new ExcludeFilter(dictionary);
        Set<String> filteredSet = Sets.newHashSet();
        filter.apply(filteredSet, metrics);
        Assert.assertTrue(filteredSet.contains("MemHeapCommittedM"));
        Assert.assertTrue(filteredSet.contains("storeCount"));
    }

}
