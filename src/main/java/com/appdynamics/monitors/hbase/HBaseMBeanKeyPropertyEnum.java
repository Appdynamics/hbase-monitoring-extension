package com.appdynamics.monitors.hbase;


public enum HBaseMBeanKeyPropertyEnum {

    NAME("name"),
    SUBTYPE("sub");

    private final String name;

    HBaseMBeanKeyPropertyEnum(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
}
