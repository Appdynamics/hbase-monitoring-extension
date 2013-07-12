# AppDynamics HBase Monitoring Extension

##Use case

The HBase custom monitor captures HBase statistics from the JMX server and displays them in the AppDynamics Metric Browser.

##Files and folders included


<table><tbody>
<tr>
<th align = 'left'> Directory/File </th>
<th align = 'left'> Description </th>
</tr>
<tr>
<td align = 'left'> bin </td>
<td align = 'left'> Contains class files </td>
</tr>
<tr>
<td align = 'left'> conf </td>
<td align = 'left'> Contains the monitor.xml </td>
</tr>
<tr>
<td align = 'left'> lib </td>
<td align = 'left'> Contains third-party project references </td>
</tr>
<tr>
<td align = 'left'> src </td>
<td align = 'left'> Contains source code to HBase Custom Monitor </td>
</tr>
<tr>
<td align = 'left'> dist </td>
<td align = 'left'> Contains the distribution package (monitor.xml and jar) </td>
</tr>
<tr>
<td align = 'left'> build.xml </td>
<td align = 'left'> Ant build script to package the project (required only if changing Java code) </td>
</tr>
</tbody>
</table>


![](images/emoticons/information.gif) Main Java File: **src/com/appdynamics/monitors/hbase/HBaseMonitor.java**  -> This file contains the metric parsing and printing.

##Installation


![](images/emoticons/warning.gif) The HBase Server must [enable JMX metrics](http://hbase.apache.org/metrics.html).

1. In the \<machine agent home\>/monitors directory, create a new folder for the HBase monitor.
2. Copy the contents in the 'dist' folder to the folder created in step 1.
3. Restart the Machine Agent.
4.  In the AppDynamics Metric Browser, look for: Application Infrastructure
    Performance | \<Tier\> | Custom Metrics | HBase | Status | Activity

##Rebuilding the project


1.  At the command line, go to the root directory (where all the files are located).
2.  Type "ant" (without the quotes) and press Return.

    'dist' will be updated with the monitor.xml and hbase.jar

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


##Screen shot


![](images/hbase_01.png)


##Contributing

Always feel free to fork and contribute any changes directly via GitHub.


##Support

For any support questions, please contact ace@appdynamics.com.
