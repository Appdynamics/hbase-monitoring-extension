package com.appdynamics.monitors.hbase;

import java.util.List;

/**
 * @author Satish Muddam
 */
public class Configuration {

    private List<HBase> hbase;

    public List<HBase> getHbase() {
        return hbase;
    }

    public void setHbase(List<HBase> hbase) {
        this.hbase = hbase;
    }
}
