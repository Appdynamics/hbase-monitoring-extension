# AppDynamics HBase Monitoring Extension


## Use Case

HBase is an open-source, non-relational, distributed database. The monitoring extension captures HBase statistics from the JMX server and displays them in the AppDynamics Metric Browser.

## Prerequisites

The HBase Server must [enable JMX metrics](http://hbase.apache.org/metrics.html) or [setup JMX remote access](http://hbase.apache.org/metrics.html#Setup_JMX_remote_access). Once the configuration is done, you should be able to run JConsole (included with the JDK since JDK 5.0) to view the statistics via JMX .
To know more about JMX, please follow the [link]( http://docs.oracle.com/javase/6/docs/technotes/guides/management/agent.html).

 In order to use this extension, you do need a [Standalone JAVA Machine Agent](https://docs.appdynamics.com/display/PRO44/Standalone+Machine+Agents) or [SIM Agent](https://docs.appdynamics.com/display/PRO44/Server+Visibility).  For more details on downloading these products, please  visit [here](https://download.appdynamics.com/).
The extension needs to be able to connect to the HAProxy in order to collect and send metrics. To do this, you will have to either establish a remote connection in between the extension and the product, or have an agent on the same machine running the product in order for the extension to collect and send the metrics.

## Installation
    1. Download and unzip the HBaseMonitor-3.1.2.zip to the "<MachineAgent_Dir>/monitors" directory.
    2. Edit the file config.yml as described below in Configuration Section, located in <MachineAgent_Dir>/monitors/HBaseMonitor and update the server(s) details.
    3. All metrics to be reported are configured in metrics.xml. Users can remove entries from metrics.xml to stop the metric from reporting, or add new entries as well.
    4. Restart the Machine Agent.

Please place the extension in the **"monitors"** directory of your **Machine Agent** installation directory. Do not place the extension in the **"extensions"** directory of your **Machine Agent** installation directory.
In the AppDynamics Metric Browser, look for **Application Infrastructure Performance|\<Tier\>|Custom Metrics|HBase** and you should be able to see all the metrics.


## Configuration
### Config.yml

Configure the extension by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/HBaseMonitor/`.

  1. Configure the "COMPONENT_ID" under which the metrics need to be reported. This can be done by changing the value of `<COMPONENT_ID>` in   **metricPrefix: Server|Component:<COMPONENT_ID>|Custom Metrics|HBase|**.
       For example,
       ```
       metricPrefix: "Server|Component:100|Custom Metrics|HBase|"
       ```

  2. The extension supports reporting metrics from multiple HBase instances. The monitor provides an option to add HBase server/s for monitoring the metrics provided by the particular end-point. Have a look at config.yml for more details.
      For example:
      ```
      metricPrefix: Server|Component:<COMPONENT_ID>|Custom Metrics|HBase

      # List of HBase Instances
      instances:
         - displayName: "Local HBase"
           serviceUrl: "" #"service:jmx:rmi:///jndi/rmi://127.0.0.1:10101/jmxrmi" #provide jmx service URL [OR] provide [host][port] pair
           host: "localhost"
           port: 10101
           username:
           # Provide password or encryptedPassword
           password:
           encryptedPassword:
           regionServers:
             - displayName: "RegionServer1"
               serviceUrl: ""
               host: "localhost"
               port: 10101
               username:
               # Provide password or encryptedPassword
               password:
               encryptedPassword:
             - displayName: "RegionServer2"
               serviceUrl: ""
               host: "localhost"
               port: 10101
               username:
               # Provide password or encryptedPassword
               password:
               encryptedPassword:

      encryptionKey:
      ```
  3. Configure Service URL or host/port pair.
    Please provide either serviceUrl or the host/port pair which will be used to establish the connection with JMX.
      ```
            #"service:jmx:rmi:///jndi/rmi://127.0.0.1:10101/jmxrmi" #provide jmx service URL [OR] provide [host][port] pair
            serviceUrl: ""
            host: "localhost"
            port: 10101
     ```
  4. Configure the numberOfThreads.
     For example,
     Running 1 new thread for each new instance or its regionServers
     ```
     numberOfThreads: 10
     ```

### Metrics.xml

You can add/remove metrics of your choice by modifying the provided metrics.xml file. This file consists of all the metrics that will be monitored and sent to the controller. Please look how the metrics have been defined and follow the same convention, when adding new metrics. You do have the ability to chosoe your Rollup types as well as set an alias that you would like to be displayed on the metric browser.

   1. mbeans Configuration
    Add the `mbeans` and `mbeanObject` under the `stats` tag, which will have the metrics, as shown below.
```
    <mbeans name="common">
        <mbeanObject objectName="Hadoop:service=HBase,name=JvmMetrics">
            <metric attr="MemHeapCommittedM" alias="MemHeapCommittedM" aggregationType="AVERAGE" timeRollUpType="AVERAGE"
                    clusterRollUpType="COLLECTIVE"/>
            <metric attr="MemHeapMaxM" alias="MemHeapMaxM" aggregationType="AVERAGE" timeRollUpType="AVERAGE"
                    clusterRollUpType="COLLECTIVE"/>
        </mbeanObject>
 ```

   2. Metric Configuration
    Add the `metric` to be monitored with the metric tag as shown below.
```
         <metric attr="MemHeapCommittedM" alias="MemHeapCommittedM" aggregationType="AVERAGE" timeRollUpType="AVERAGE"
                    clusterRollUpType="COLLECTIVE"/>
 ```
For configuring the metrics, the following properties can be used:

 |     Property      |   Default value |         Possible values         |                                               Description                                                      |
 | ----------------- | --------------- | ------------------------------- | -------------------------------------------------------------------------------------------------------------- |
 | alias             | metric name     | Any string                      | The substitute name to be used in the metric browser instead of metric name.                                   |
 | aggregationType   | "AVERAGE"       | "AVERAGE", "SUM", "OBSERVATION" | [Aggregation qualifier](https://docs.appdynamics.com/display/PRO44/Build+a+Monitoring+Extension+Using+Java)    |
 | timeRollUpType    | "AVERAGE"       | "AVERAGE", "SUM", "CURRENT"     | [Time roll-up qualifier](https://docs.appdynamics.com/display/PRO44/Build+a+Monitoring+Extension+Using+Java)   |
 | clusterRollUpType | "INDIVIDUAL"    | "INDIVIDUAL", "COLLECTIVE"      | [Cluster roll-up qualifier](https://docs.appdynamics.com/display/PRO44/Build+a+Monitoring+Extension+Using+Java)|
 | multiplier        | 1               | Any number                      | Value with which the metric needs to be multiplied.                                                            |
 | convert           | null            | Any key value map               | Set of key value pairs that indicates the value to which the metrics need to be transformed. eg: UP:1, OPEN:1  |
 | delta             | false           | true, false                     | If enabled, gives the delta values of metrics instead of actual values.                                        |


 **All these metric properties are optional, and the default value shown in the table is applied to the metric (if a property has not been specified) by default.**

## Metrics
HBase metrics are exported under the `hadoop` domain in JMX and the extension collects the metrics as available in the Jconsole. For more details on the available metrics, please refer [hbase metrics](https://hbase.apache.org/book.html#hbase_metrics).

## Credentials Encryption

Please visit [this page](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-Password-Encryption-with-Extensions/ta-p/29397) to get detailed instructions on password encryption. The steps in this document will guide you through the whole process.

## Extensions Workbench
Workbench is an inbuilt feature provided with each extension in order to assist you to fine tune the extension setup before you actually deploy it on the controller. Please review the following document on [How to use the Extensions WorkBench](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-the-Extensions-WorkBench/ta-p/30130).

## Troubleshooting
Before configuring the extension, please make sure to run the below steps to check if the set up is correct.

1. Telnet into your HBaseMonitor server from the box where the extension is deployed.
       telnet <hostname> <port> or telnet serviceUrl

       <port> - It is the jmxremote.port specified.
        <hostname> - IP address

    If telnet works, it confirm the access to the HBaseMonitor server.

2. Start jconsole. Jconsole comes as a utility with installed jdk. After giving the correct host and port , check if HBase mbean shows up under `Hadoop`.

3. It is a good idea to match the mbean configuration in the config.yml against the jconsole. JMX is case sensitive so make
sure the config matches exact.

Also, please follow the steps listed in this [troubleshooting-document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) in order to troubleshoot your issue. These are a set of common issues that customers might have faced during the installation of the extension. If these don't solve your issue, please follow the last step on the [troubleshooting-document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) to contact the support team.


## Support Tickets
If after going through the [Troubleshooting Document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) you have not been able to get your extension working, please file a ticket and add the following information.

Please provide the following in order for us to assist you better.

    1. Stop the running machine agent.
    2. Delete all existing logs under <MachineAgent>/logs.
    3. Please enable debug logging by editing the file <MachineAgent>/conf/logging/log4j.xml. Change the level value of the following <logger> elements to debug.
        <logger name="com.singularity">
        <logger name="com.appdynamics">
    4. Start the machine agent and please let it run for 10 mins. Then zip and upload all the logs in the directory <MachineAgent>/logs/*.
    5. Attach the zipped <MachineAgent>/conf/* directory here.
    6. Attach the zipped <MachineAgent>/monitors/ExtensionFolderYouAreHavingIssuesWith directory here.

For any support related questions, you can also contact help@appdynamics.com.

## Contributing
Always feel free to fork and contribute any changes directly here on [GitHub](https://github.com/Appdynamics/hbase-monitoring-extension).

## Version
|          Name            |  Version   |
|--------------------------|------------|
|Extension Version         |3.1.2       |
|Controller Compatibility  |3.7 or Later|
|Product Tested On         |1.2.6       |
|Last Update               |10/12/2018  |
|Changes list              |[ChangeLog](https://github.com/Appdynamics/hbase-monitoring-extension/blob/master/CHANGELOG.md)|