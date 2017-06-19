package com.appdynamics.monitors.hbase;


public enum HBaseMBeanKeyPropertyEnum {

    //NODEID("nodeId"),
    SERVICE("service"),
    //RESPONSIBILITY("responsibility"),
    //DOMAIN("Domain"),
    NAME("name"),
    SUBTYPE("sub");
    //CACHE("cache"),
    //TYPE("type"),
    //SCOPE("scope"),

    //KEYSPACE("keyspace"),
    //PATH("path"),
    //TIER("tier");

    private final String name;

    HBaseMBeanKeyPropertyEnum(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
}
