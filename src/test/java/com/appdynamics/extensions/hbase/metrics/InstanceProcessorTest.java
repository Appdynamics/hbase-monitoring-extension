package com.appdynamics.extensions.hbase.metrics;/*
 * Copyright 2019. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

import com.appdynamics.extensions.hbase.ConfigTestUtil;
import com.appdynamics.extensions.hbase.collector.InstanceProcessor;
import com.appdynamics.extensions.metrics.Metric;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.powermock.api.mockito.PowerMockito.when;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ReflectionException;
import java.io.IOException;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
public class InstanceProcessorTest {
    @Mock
    InstanceProcessor instanceProcessor;

    private ObjectInstance objectInstance;
    @Before
    public void inititalize() throws MalformedObjectNameException {
        objectInstance = new ObjectInstance("Hadoop:service=HBase,name=JvmMetrics", "JvmMetrics" );
    }

    @Test
    public void processInstanceTest() throws IOException, IntrospectionException, InstanceNotFoundException, ReflectionException {
        List<Metric> instanceMetric = ConfigTestUtil.readAllMetrics("src/test/resources/conf/objectInstance.txt");
        when(instanceProcessor.processInstance(objectInstance)).thenReturn(instanceMetric);
        List<Metric> metrics = instanceProcessor.processInstance(objectInstance);
        Assert.assertEquals(metrics.size(), 2);
    }

}
