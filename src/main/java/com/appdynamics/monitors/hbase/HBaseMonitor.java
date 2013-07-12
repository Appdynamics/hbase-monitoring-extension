package com.appdynamics.monitors.hbase;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.log4j.Logger;

import com.appdynamics.monitors.hbase.lookup.MetricLookup;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

public class HBaseMonitor extends AManagedMonitor
{
	private MBeanServerConnection connection = null;
	private MBeanAttributeInfo[] hbaseMetrics = null;
	private ObjectName hbaseBean = null;

	private static Logger logger = Logger.getLogger(HBaseMonitor.class);

	/**
	 * Main execution method that uploads the metrics to the AppDynamics Controller
	 * @see com.singularity.ee.agent.systemagent.api.ITask#execute(java.util.Map, com.singularity.ee.agent.systemagent.api.TaskExecutionContext)
	 */
	@Override
	public TaskOutput execute(Map<String, String> args, TaskExecutionContext arg1)
			throws TaskExecutionException
	{
		try
		{
			String host = args.get("host");
			String port = args.get("port");
			String username = args.get("user");
			String password = args.get("pass");

			connect(host, port, username, password);
			populate();

			printMetric("Uptime", 1, MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
				MetricWriter.METRIC_TIME_ROLLUP_TYPE_SUM,
				MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

			for (MBeanAttributeInfo metric : hbaseMetrics)
			{
				String attributeName = metric.getName();
				String metricName = MetricLookup.getMetric(attributeName);
				if (metricName != null && !metricName.equals("")) 
				{
					Double result = Double.parseDouble(connection.getAttribute(hbaseBean, attributeName).toString());
    				printMetric("Activity|" + metricName,
						result.intValue(),
						MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
						MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
						MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
				}
			}

			return new TaskOutput("HBase Metric Upload Complete");
		} catch (Exception e)
		{
			logger.error(e.toString());
			return new TaskOutput("HBase Metric Upload Failed!");
		}
	}

	/**
	 * Connects to JMX Remote Server to access HBase JMX Metrics
	 * 
	 * @param 	host				Host of the remote jmx server.
	 * @param 	port				Port of the remote jmx server.
	 * @param 	username			Username to access the remote jmx server.
	 * @param 	password			Password to access the remote jmx server.
	 * @throws 	IOException			Failed to connect to server.
	 */
	public void connect(String host, String port, String username, String password)
			throws IOException
	{
		JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + host + ":" + port
				+ "/jmxrmi");
		Map<String, Object> env = new HashMap<String, Object>();
		env.put(JMXConnector.CREDENTIALS, new String[] { username, password });
		connection = JMXConnectorFactory.connect(url, env).getMBeanServerConnection();
	}

	/**
	 * Fetches all the attributes from HBase RegionServer JMX
	 * @throws Exception
	 */
	public void populate() throws Exception
	{
		hbaseBean = new ObjectName("hadoop:service=RegionServer,name=RegionServerStatistics");
		hbaseMetrics = connection.getMBeanInfo(hbaseBean).getAttributes();
	}

	/**
	 * Returns the metric to the AppDynamics Controller.
	 * 
	 * @param metricName 	Name of the Metric
	 * @param metricValue 	Value of the Metric
	 * @param aggregation 	Average OR Observation OR Sum
	 * @param timeRollup 	Average OR Current OR Sum
	 * @param cluster 		Collective OR Individual
	 */
	public void printMetric(String metricName, Object metricValue, String aggregation,
			String timeRollup, String cluster)
	{
		MetricWriter metricWriter = getMetricWriter(getMetricPrefix() + metricName, aggregation,
				timeRollup, cluster);

		metricWriter.printMetric(String.valueOf(metricValue));
	}

	/**
	 * Metric Prefix
	 * 
	 * @return Metric Location in the Controller (String)
	 */
	public String getMetricPrefix()
	{
		return "Custom Metrics|HBase|Status|";
	}
}