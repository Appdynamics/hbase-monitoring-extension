package com.appdynamics.extensions.hbase.metrics;/*
 * Copyright 2019. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

import com.appdynamics.extensions.hbase.Config.MetricConfig;
import com.appdynamics.extensions.hbase.collector.AttributeProcessor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.management.Attribute;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
public class AttributeProcessorTest {
    Attribute compositeAttribute;
    @Before
    public void initialize(){
        compositeAttribute = new Attribute("usage", 60);
    }

    @Test
    public void processAttributeToMetricTest() throws IOException{
        Attribute attribute = new Attribute("usage", 60);
        MetricConfig config = new MetricConfig();
        config.setAttr("usage");
        Map<String, MetricConfig> attrMap = new HashMap<>();
        attrMap.put("usage", config);
        AttributeProcessor attributeProcessor = PowerMockito.spy(new AttributeProcessor());
        Assert.assertEquals((attributeProcessor.processAttributeToMetric(attribute, attrMap, null)).getMetricValue(), "60");

    }

    @Test
    public void processCompositeAttriubteToMetricTest() throws IOException{
        MetricConfig config = new MetricConfig();
        config.setAttr("usage");
        Map<String, MetricConfig> attrMap = new HashMap<>();
        attrMap.put("usage", config);
        AttributeProcessor attributeProcessor = PowerMockito.spy(new AttributeProcessor());
        Assert.assertEquals((attributeProcessor.processAttributeToMetric(compositeAttribute, attrMap, null)).getMetricValue(), "60");

    }

}
