package com.appdynamics.monitors.hbase.Config;

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
