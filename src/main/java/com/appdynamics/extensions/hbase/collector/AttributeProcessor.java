package com.appdynamics.extensions.hbase.collector;/*
 * Copyright 2019. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

import com.appdynamics.extensions.hbase.Config.MetricConfig;
import com.appdynamics.extensions.hbase.Config.MetricConverter;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;

import javax.management.Attribute;
import java.util.Map;

public class AttributeProcessor {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(AttributeProcessor.class);
    private ObjectMapper objectMapper = new ObjectMapper();

    public Metric processAttributeToMetric(Attribute attr, Map<String, MetricConfig> mbeanMetricsWithConfig, String metricPath) {
        Metric metric = null;
        try {
            String attrName = attr.getName();
            Object value = attr.getValue();
            if (value != null) {
                MetricConfig config = mbeanMetricsWithConfig.get(attrName);
                if (config.getMetricConverter() != null)
                    value = getConvertedStatus(config.getMetricConverter(), String.valueOf(value));
                metric = new Metric(attrName, String.valueOf(value), metricPath + attrName, objectMapper.convertValue(config, Map.class));
            } else {
                logger.warn("Ignoring metric {} with path {} as the value is null", attrName, metricPath);
            }
        } catch (Exception e) {
            logger.error("Error collecting value for {} ", attr.getName(), e);
        } finally {
            return metric;
        }
    }

    /**
     * @param converters
     * @param status
     * @return
     */
    private String getConvertedStatus(MetricConverter[] converters, String status) {
        for (MetricConverter converter : converters) {
            if (converter.getLabel().equals(status))
                return converter.getValue();
        }
        return "";
    }
}
