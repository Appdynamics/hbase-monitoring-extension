# AppDynamics HBase - Monitoring Extension

##Use Case

The HBase custom monitor captures HBase statistics from the JMX server and displays them in the AppDynamics Metric Browser.

##Files and Folders Included

<table><tbody>
<tr>
<th align = 'left'> Directory/File </th>
<th align = 'left'> Description </th>
</tr>
<tr>
<td class='confluenceTd'> conf </td>
<td class='confluenceTd'> Contains the monitor.xml </td>
</tr>
<tr>
<td class='confluenceTd'> lib </td>
<td class='confluenceTd'> Contains third-party project references </td>
</tr>
<tr>
<td class='confluenceTd'> src </td>
<td class='confluenceTd'> Contains source code to the HBase Monitoring Extension </td>
</tr>
<tr>
<td class='confluenceTd'> dist </td>
<td class='confluenceTd'> Only obtained when using ant. Run 'ant build' to get binaries. Run 'ant package' to get the distributable .zip file. </td>
</tr>
<tr>
<td class='confluenceTd'> build.xml </td>
<td class='confluenceTd'> Ant build script to package the project (required only if changing Java code) </td>
</tr>
</tbody>
</table>


##Installation

![](images/emoticons/warning.gif) The HBase Server must [enable JMX metrics](http://hbase.apache.org/metrics.html).

1. Run 'ant package' from the hbase-monitoring-extension directory
2. Download the file HBaseMonitor.zip found in the 'dist' directory into \<machineagent install dir\>/monitors/
3. Unzip the downloaded file
4. Restart the machineagent
5. In the AppDynamics Metric Browser, look for: Application Infrastructure Performance | \<Tier\> | Custom Metrics | HBase | Status | Activity


##Metrics


<table class='confluenceTable'><tbody>
<tr>
<th align = 'left'> Metric Name </th>
<th align = 'left'> Description </th>
</tr>
<tr>
<td align = 'left'> Block Cache Count </td>
<td align = 'left'> Block cache item count in memory. This is the number of blocks of StoreFiles (HFiles) in the cache. </td>
</tr>
<tr>
<td align = 'left'> Block Cache Evicted Count </td>
<td align = 'left'> Number of blocks that had to be evicted from the block cache due to heap size constraints. </td>
</tr>
<tr>
<td align = 'left'> Block Cache Free </td>
<td align = 'left'> Block cache memory available (bytes). </td>
</tr>
<tr>
<td align = 'left'> Block Cache Hit Caching Ratio </td>
<td align = 'left'> Block cache hit caching ratio (0 to 100). The cache-hit ratio for reads configured to look in the cache (i.e., cacheBlocks=true). </td>
</tr>
<tr>
<td align = 'left'> Block Cache Hit Count </td>
<td align = 'left'> Number of blocks of StoreFiles (HFiles) read from the cache. </td>
</tr>
<tr>
<td align = 'left'> Block Cache Hit Ratio </td>
<td align = 'left'> Block cache hit ratio (0 to 100). Includes all read requests, although those with cacheBlocks=false will always read from disk and be counted as a "cache miss". </td>
</tr>
<tr>
<td align = 'left'> Block Cache Miss Count </td>
<td align = 'left'> Number of blocks of StoreFiles (HFiles) requested but not read from the cache. </td>
</tr>
<tr>
<td align = 'left'> Block Cache Size </td>
<td align = 'left'> Block cache size in memory (bytes). i.e., memory in use by the BlockCache </td>
</tr>
<tr>
<td align = 'left'> Compaction Queue Size </td>
<td align = 'left'> Size of the compaction queue. This is the number of Stores in the RegionServer that have been targeted for compaction. </td>
</tr>
<tr>
<td align = 'left'> Flush Queue Size </td>
<td align = 'left'> Number of enqueued regions in the MemStore awaiting flush. </td>
</tr>
<tr>
<td align = 'left'> Filesystem Read Latency Avg Time </td>
<td align = 'left'> Filesystem read latency (ms). This is the average time to read from HDFS. </td>
</tr>
<tr>
<td align = 'left'> Filesystem Read Latency Operations </td>
<td align = 'left'> Filesystem read operations. </td>
</tr>
<tr>
<td align = 'left'> Filesystem Sync Latency Avg Time </td>
<td align = 'left'> Filesystem sync latency (ms). Latency to sync the write-ahead log records to the filesystem. </td>
</tr>
<tr>
<td align = 'left'> Filesystem Sync Latency Operations </td>
<td align = 'left'> Number of operations to sync the write-ahead log records to the filesystem. </td>
</tr>
<tr>
<td align = 'left'> Filesystem Write Latency Avg Time </td>
<td align = 'left'> Filesystem write latency (ms). Total latency for all writers, including StoreFiles and write-head log. </td>
</tr>
<tr>
<td align = 'left'> Filesystem Write Latency Operations </td>
<td align = 'left'> Number of filesystem write operations, including StoreFiles and write-ahead log. </td>
</tr>
<tr>
<td align = 'left'> Memstore Size (MB) </td>
<td align = 'left'> Sum of all the memstore sizes in this RegionServer (MB) </td>
</tr>
<tr>
<td align = 'left'> Regions </td>
<td align = 'left'> Number of regions served by the RegionServer </td>
</tr>
<tr>
<td align = 'left'> Requests </td>
<td align = 'left'> Total number of read and write requests. Requests correspond to RegionServer RPC calls, thus a single Get will result in 1 request, but a Scan with caching set to 1000 will result in 1 request for each 'next' call (i.e., not each row). A bulk-load request will constitute 1 request per HFile. </td>
</tr>
<tr>
<td align = 'left'> Store File Index Size (MB) </td>
<td align = 'left'> Sum of all the StoreFile index sizes in this RegionServer (MB) </td>
</tr>
<tr>
<td align = 'left'> Stores </td>
<td align = 'left'> Number of Stores open on the RegionServer. A Store corresponds to a ColumnFamily. For example, if a table (which contains the column family) has 3 regions on a RegionServer, there will be 3 stores open for that column family. </td>
</tr>
<tr>
<td align = 'left'> Store Files </td>
<td align = 'left'> Number of StoreFiles open on the RegionServer. A store may have more than one StoreFile (HFile). </td>
</tr>
</tbody>
</table>


##Screen Shot


![](http://appsphere.appdynamics.com/t5/image/serverpage/image-id/71i2A4082FA8329124C/image-size/original?v=mpbl-1&px=-1)


##Contributing

Always feel free to fork and contribute any changes directly via GitHub.


##Support

For any support questions, please contact ace@appdynamics.com.
