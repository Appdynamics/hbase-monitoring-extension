package com.appdynamics.monitors.hbase.Config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Arrays;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Stats {
    @XmlElement(name = "mbeans")
    private MbeanConfig[] mbeanConfigs;

    public MbeanConfig[] setMbeanConfigs() {
        return mbeanConfigs;
    }

    public void setMbeanConfigs(MbeanConfig[] mbeanConfigs) {
        this.mbeanConfigs = mbeanConfigs;
    }

    public MbeanConfig[] getMbeanConfigs(){return mbeanConfigs;}

    public List<MbeanObjectConfig> getMatchingMbeanConfig(String mbeanName){
        for(MbeanConfig mbeanConfig : mbeanConfigs){
            if(mbeanConfig.getName().equals(mbeanName))
                return Arrays.asList(mbeanConfig.getMbeanObjectConfigs());
        }
        return null;
    }
}