/*
 *   Copyright 2019. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.hbase.Config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class MbeanConfig {
    @XmlAttribute
    private String name;
    @XmlElement(name = "mbeanObject")
    private MbeanObjectConfig[] mbeanObjectConfigs;
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MbeanObjectConfig[] getMbeanObjectConfigs() {
        return mbeanObjectConfigs;
    }

    public void setMbeanObjectConfigs(MbeanObjectConfig[] mbeanObjectConfigs) {
        this.mbeanObjectConfigs = mbeanObjectConfigs;
    }
}
