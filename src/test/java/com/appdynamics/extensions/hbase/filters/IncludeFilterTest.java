/*
 *   Copyright 2019. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.hbase.filters;

import com.appdynamics.extensions.hbase.DictionaryGenerator;
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
        Set<String> filteredResult =  filter.apply(metrics);
        Assert.assertTrue(filteredResult.contains("storeCount"));
        Assert.assertTrue(filteredResult.contains("storeFileCount"));
        Assert.assertTrue(!filteredResult.contains("MemHeapCommittedM"));
    }

    @Test
    public void whenNullDictionary_thenReturnUnchangedSet() {
        List<String> metrics = Lists.newArrayList("MemHeapCommittedM", "storeCount");
        IncludeFilter filter = new IncludeFilter(null);
        Set<String> filteredResult = filter.apply( metrics);
        Assert.assertTrue(filteredResult.size() == 0);
    }

    @Test
    public void whenEmptyDictionary_thenReturnUnchangedSet() {
        Set dictionary = Sets.newHashSet();
        dictionary.add("storeCount");
        List<String> metrics = Lists.newArrayList("MemHeapCommittedM", "storeCount");
        IncludeFilter filter = new IncludeFilter(dictionary);
        Set<String> filteredResult = filter.apply(metrics);
        Assert.assertTrue(filteredResult.size() == 1);
    }
}
