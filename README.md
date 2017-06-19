# AppDynamics HBase - Monitoring Extension

This extension works only with the standalone machine agent.

## Use Case

The HBase custom monitor captures HBase statistics from the JMX server and displays them in the AppDynamics Metric Browser.

## Prerequisites ##

The HBase Server must [enable JMX metrics](http://hbase.apache.org/metrics.html).

To know more about JMX, please follow the below link
 
 http://docs.oracle.com/javase/6/docs/technotes/guides/management/agent.html


## Troubleshooting steps ##
Before configuring the extension, please make sure to run the below steps to check if the set up is correct.

1. Telnet into your HBaseMonitor server from the box where the extension is deployed.
       telnet <hostname> <port>

       <port> - It is the jmxremote.port specified.
        <hostname> - IP address

    If telnet works, it confirm the access to the HBaseMonitor server.


2. Start jconsole. Jconsole comes as a utility with installed jdk. After giving the correct host and port , check if HBase mbean shows up.

3. It is a good idea to match the mbean configuration in the config.yml against the jconsole. JMX is case sensitive so make
sure the config matches exact.

## Installation

1. Run 'mvn clean install' from the hbase-monitoring-extension directory
2. Download the file HBaseMonitor-{version}.zip found in the 'target' directory into \<machineagent install dir\>/monitors/
3. Unzip the downloaded file
4. Open \<machineagent install dir\>/monitors/HBaseMonitor/config.yaml and configure the HBase server peoperties. You can provide multiple HBase server properties.
5. Restart the machineagent
6. In the AppDynamics Metric Browser, look for: Application Infrastructure Performance | \<Tier\> | Custom Metrics | HBase | 

## Configuration

Note : Please make sure to not use tab (\t) while editing yaml files. You may want to validate the yaml file using a [yaml validator](http://yamllint.com/)

1. Configure the HBase instances by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/HBaseMonitor/`.
2. Below is the default config.yml which has metrics configured already
   For eg.
 
 ```
   ### ANY CHANGES TO THIS FILE DOES NOT REQUIRE A RESTART ###

#This will create this metric in all the tiers, under this path
#metricPrefix: Custom Metrics|HBase

#This will create it in specific Tier/Component. Make sure to replace <COMPONENT_ID> with the appropriate one from your environment.
#To find the <COMPONENT_ID> in your environment, please follow the screenshot https://docs.appdynamics.com/display/PRO42/Build+a+Monitoring+Extension+Using+Java
metricPrefix: Server|Component:<COMPONENT_ID>|Custom Metrics|HBase

# List of HBase Instances
instances:
  - displayName: "Local HBase"  #displayName is a REQUIRED field for  level metrics.
    host: ""
    port:
    username: ""
    # Provide password or encryptedPassword
    password: ""
    encryptedPassword:
    regionServers:
       - displayName: "RegionServer1"
         host: ""
         port:
         username: ""
         # Provide password or encryptedPassword
         password: ""
         encryptedPassword:
       - displayName: "RegionServer2"
         host: ""
         port: 10101
         username: ""
         # Provide password or encryptedPassword
         password: ""
         encryptedPassword:
       - displayName: "RegionServer3"
         host: ""
         port:
         username: ""
         # Provide password or encryptedPassword
         password: ""
         encryptedPassword:
         
encryptionKey:

# number of concurrent tasks.
# This doesn't need to be changed unless many instances are configured
numberOfThreads: 10

# The configuration of different metrics from various mbeans of HBase server
mbeans:
   # Common mbeans which are applicable to both master and region server
   common:
      - objectName: "Hadoop:service=HBase,name=JvmMetrics"
        metrics:
          include:
            - MemHeapCommittedM : "MemHeapCommittedM"
            - MemHeapMaxM : "MemHeapMaxM"

   # Master specific mbeans
   master:
      # This mbean is to get cluster related metrics.
      - objectName: "Hadoop:service=HBase,name=Master,sub=AssignmentManger"
        metrics:
          include:
            - BulkAssign_max : "BulkAssign_max"  # If this attribute is removed, nodeIds will be seen in the metric paths and not their corressponding names.
            - Assign_max : "Assign_max"

      - objectName: "Hadoop:service=HBase,name=Master,sub=Server"
        #aggregation: true #uncomment this only if you want the extension to do aggregation for all the metrics in this mbean for a cluster
        metrics:
          include:
            - averageLoad : "averageLoad" #The rough number of cache hits since the last time statistics were reset. A cache hit is a read operation invocation (that is, get()) for which an entry exists in this map.
            - clusterRequests : "clusterRequests" #The rough number of cache misses since the last time statistics were reset.
            - numDeadRegionServers : "numDeadRegionServers" #The number of prune operations since the last time statistics were reset. A prune operation occurs every time the cache reaches its high watermark as specified by the HighUnits attribute.
            - numRegionServers : "numRegionServers" #The total number of get() operations since the last time statistics were reset.
   # region server specific mbeans
   regionServer:
      - objectName: "Hadoop:service=HBase,name=RegionServer,sub=Server"
        aggregation: true
        metricType: "OBSERVATION AVERAGE COLLECTIVE"
        metrics:
          include:
            - storeCount : "storeCount"
            - storeFileCount : "storeFileCount"
            - storeFileIndexSize : "storeFileIndexSize"
      - objectName: "Hadoop:service=HBase,name=RegionServer,sub=IPC"
        aggregation: false
        metricType: "OBSERVATION AVERAGE COLLECTIVE"
        metrics:
          include:
            - TotalCallTime_num_ops : "TotalCallTime_num_ops"
            - exceptions : "exceptions"
              aggregation: true
            - ProcessCallTime_num_ops : "ProcessCallTime_num_ops"
      - objectName: "Hadoop:service=HBase,name=RegionServer,sub=WAL"
       #aggregation: true #uncomment this only if you want the extension to do aggregation for all the metrics in this mbean for a cluster
        metrics:
          include:
            - AppendSize_num_ops : "AppendSize_num_ops"
            - AppendSize_min : "AppendSize_min"
            - AppendSize_max : "AppendSize_max"
```
The objectNames mentioned in the above yaml may not match your environment exactly. Please use jconsole to extract the objectName and configure it accordingly in the config.yaml. 

3. Configure the path to the config.yml file by editing the <task-arguments> in the monitor.xml file in the `<MACHINE_AGENT_HOME>/monitors/HBaseMonitor/` directory. Below is the sample
   For Windows, make sure you enter the right path.
     ```
     <task-arguments>
         <!-- config file-->
         <argument name="config-file" is-required="true" default-value="monitors/HBaseMonitor/config.yml" />
          ....
     </task-arguments>
    ```

## Workbench

Workbench is a feature by which you can preview the metrics before registering it with the controller. This is useful if you want to fine tune the configurations. Workbench is embedded into the extension jar.

To use the workbench
1. Follow all the installation steps
2. Start the workbench with the command
`java -jar /path/to/MachineAgent/monitors/HBaseMonitor/hbase-monitoring-extension.jar`
This starts an http server at http://host:9090/. This can be accessed from the browser.
3. If the server is not accessible from outside/browser, you can use the following end points to see the list of registered metrics and errors.
```
#Get the stats
curl http://localhost:9090/api/stats
#Get the registered metrics
curl http://localhost:9090/api/metric-paths
```
4. You can make the changes to config.yml and validate it from the browser or the API
5. Once the configuration is complete, you can kill the workbench and start the Machine Agent

## Contributing

Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/Appdynamics/hbase-monitoring-extension).

## Community

Find out more in the [AppSphere](http://appsphere.appdynamics.com/t5/Extensions/HBase-Monitoring-Extension/idi-p/829) community.

## Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:help@appdynamics.com).
