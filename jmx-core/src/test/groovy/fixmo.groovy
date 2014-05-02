import ch.ethz.ssh2.*;
import java.text.SimpleDateFormat;
import java.util.regex.*;
import java.util.concurrent.*;
import javax.management.remote.*;
import javax.management.*;
import java.lang.management.*;
import com.aphyr.riemann.client.*;


pk = new File("C:\\Users\\nwhitehe\\ssh\\id_rsa");


jmxHost = "pdk-pt-ceas-03"
bridgeHost = "localhost";
port = 8006;
user = "nwhitehe";
ObjectName batchService = new ObjectName("com.cpex.clearing.fixml.matchoff.stats.batch:service=BatchAttributeService");
String iface = "com.cpex.clearing.fixml.matchoff.stats.unsafe.DirectEWMAMBean";
String[] ewmaSig = [String.class.getName(), String.class.getName(), String.class.getName(), String[].class.getName()] as String[];
def  genericSig = [ObjectName.class.getName(), String.class.getName(), String.class.getName(), String[].class.getName()] as String[];

class ConnListener implements NotificationListener {
    public void handleNotification(Notification notification, Object handback) {
      //println "Notification: $notification";
      def type = notification.getType();
      if(type.equals("jmx.remote.connection.closed") || type.equals("jmx.remote.connection.failed")) {
          //println "DISCONNECTED !";
      }    
    }
}



connect = { host, port ->
    def serviceURL = new JMXServiceURL("service:jmx:jmxmp://$host:$port");
    def connection = JMXConnectorFactory.newJMXConnector(serviceURL, null);
    connection.addConnectionNotificationListener(new ConnListener(), null, null);
    connection.connect();
    println "Added notification listener for [${connection.getConnectionId()}]";
    return connection;
}

prepare = { on, attrs, valueMap,  buff ->
    try {
        def ts = System.currentTimeMillis();
        def now = TimeUnit.SECONDS.convert(ts, TimeUnit.MILLISECONDS);        
        on.getKeyPropertyList().each() { k,v ->
            attrs.put(k.replace(" ", ""), v.replace(" ", ""));
        }
        if(attrs.size() > 8) {
            println "Attr Map to big: [${attrs.size()}] $attrs";
        }
        def tags = attrs.toString().replace(",", "").replace("[", "").replace("]", "").replace(":", "=");
        svc = attrs.remove("service");
        if(svc!=null) {
            attrs.put("svc", svc);
        }
        tags = [];
        attrs.each() { k,v ->
            tags.add("$k/$v".toString());
        }

        def b = new StringBuilder();
        valueMap.each() { name, value -> 
            buff.append("put $name $now $value $tags\n");
            rman.event().
            service("fixml").
            host("pdk-pt-ceas-03").
            state("running").
            metric(value).
            attributes(attrs).
            tags(name).
            tags(tags).
            time(ts).
            send();
        }
        
    } catch (e) {
        println "Error preparing metrics:$e";
        e.printStackTrace(System.err);
    } finally {
        //try { socket.close(); } catch (e) {}
    }
}

record = { buff,  socket ->
   
    try {        
        socket << buff.toString();
    } catch (e) {
        println "Error submitting metrics:$e";
    } finally {
        //try { socket.close(); } catch (e) {}
    }
}

jvmMap = {
    return [host: jmxHost, app: "fixml", component: "jvm"] as TreeMap;
}
appMap = {
    return [host: jmxHost, app: "fixml", component: "app"]  as TreeMap;
}

cleanComposites = { v, attrs, compKeyType ->
    cmap = new HashMap(v);
    cmap.entrySet().each() {
        key = it.getKey();
        value = it.getValue();
        if(key.contains("/")) {
            split = key.split("/");
            attrs.put(compKeyType, split[0]);
            v.remove(key);
            v.put(split[1], value);
        }
    }
}

collectMemPools = { server, buff ->
    def compKeyType = "memType";
    def args = [new ObjectName("java.lang:type=MemoryPool,name=*"), null, null, ["Usage", "PeakUsage"] as String[]] as Object[];
    def map = server.invoke(batchService, "batchGetNumerics", args, genericSig);
    map.each() { k, v ->
        def attrs = jvmMap();
        cleanComposites(v, attrs, "memType");
        // record = { on, attrs, valueMap,  fmt ->
        prepare(k, attrs, v,  buff);
    }
}
collectGc = { server, buff ->
    def args = [new ObjectName("java.lang:type=GarbageCollector,name=*"), null, null, ["CollectionCount", "CollectionTime"] as String[]] as Object[];
    def map = server.invoke(batchService, "batchGetNumerics", args, genericSig);
    map.each() { k, v ->
        def attrs = jvmMap();
        // record = { on, attrs, valueMap,  fmt ->
        prepare(k, attrs, v,  buff);
    }
}
collectOs = { server, buff ->
    def args = [new ObjectName("java.lang:type=OperatingSystem"), null, null, ["OpenFileDescriptorCount", "CommittedVirtualMemorySize", "FreePhysicalMemorySize", "FreeSwapSpaceSize", "SystemLoadAverage", "SystemCpuLoad", "ProcessCpuLoad"] as String[]] as Object[];
    def map = server.invoke(batchService, "batchGetNumerics", args, genericSig);
    map.each() { k, v ->
        def attrs = jvmMap();
        // record = { on, attrs, valueMap,  fmt ->
        prepare(k, attrs, v,  buff);
    }
}

collectMem = { server, buff ->
    def compKeyType = "memType";
    def args = [new ObjectName("java.lang:type=Memory"), null, null, ["HeapMemoryUsage", "NonHeapMemoryUsage"] as String[]] as Object[];
    def map = server.invoke(batchService, "batchGetNumerics", args, genericSig);
    map.each() { k, v ->
        def attrs = jvmMap();
        cleanComposites(v, attrs, "memType");
        prepare(k, attrs, v,  buff);
    }
}
collectClassLoading = { server, buff ->
    def args = [new ObjectName("java.lang:type=ClassLoading"), null, null, ["TotalLoadedClassCount", "LoadedClassCount", "UnloadedClassCount"] as String[]] as Object[];
    def map = server.invoke(batchService, "batchGetNumerics", args, genericSig);
    map.each() { k, v ->
        def attrs = jvmMap();
        prepare(k, attrs, v,  buff);
    }
}
collectCompilation = { server, buff ->
    def args = [new ObjectName("java.lang:type=Compilation"), null, null, ["TotalCompilationTime"] as String[]] as Object[];
    def map = server.invoke(batchService, "batchGetNumerics", args, genericSig);
    map.each() { k, v ->
        def attrs = jvmMap();
        prepare(k, attrs, v,  buff);
    }
}
collectThreads = { server, buff ->
    def args = [new ObjectName("java.lang:type=Threading"), null, null, ["ThreadCount", "PeakThreadCount", "DaemonThreadCount", "TotalStartedThreadCount"] as String[]] as Object[];
    def map = server.invoke(batchService, "batchGetNumerics", args, genericSig);
    map.each() { k, v ->
        def attrs = jvmMap();
        // record = { on, attrs, valueMap,  fmt ->
        prepare(k, attrs, v,  buff);
    }
}

collectEwmas = { server, buff ->
    def args = ["com.cpex.clearing.fixml.matchoff.stats.unsafe.DirectEWMAMBean", null, null, ["*"] as String[]] as Object[];
    def map = server.invoke(batchService, "batchGetNumerics", args, ewmaSig);
    map.each() { k, v ->
        def attrs = jvmMap();
        // record = { on, attrs, valueMap,  fmt ->
        prepare(k, attrs, v,  buff);
    }    
}

collectDataSources = { server, buff ->    
    def args = ["com.cpex.clearing.fixml.matchoff.stats.DataSourceMonitorMBean", null, null, ["NumIdle", "NumActive"] as String[]] as Object[];
    def map = server.invoke(batchService, "batchGetNumerics", args, ewmaSig);
    map.each() { k, v ->
        def attrs = jvmMap();
        // record = { on, attrs, valueMap,  fmt ->
        prepare(k, attrs, v,  buff);
    }        
}

collectThreadPools = { server, buff ->    
    def args = ["com.cpex.clearing.fixml.matchoff.util.JMXEnabledThreadPoolTaskExecutorMBean", null, null, ["CompletedTasks", "CreatedThreadCount", "ActiveCount", "PoolSize", "LargestPoolSize", "QueueDepth", "UncaughtExceptionCount", "RejectedExecutionCount"] as String[]] as Object[];
    def map = server.invoke(batchService, "batchGetNumerics", args, ewmaSig);
    map.each() { k, v ->
        def attrs = jvmMap();
        // record = { on, attrs, valueMap,  fmt ->
        prepare(k, attrs, v,  buff);        
    }        
}

collectJobStats = { server, buff ->    
    def args = [new ObjectName("com.cpex.clearing.fixml.matchoff:service=JobStats,name=FixmlJobStats"), null, null, ["NumAssignmentsWritten","NumPositionsWritten","NumTradesWritten","NumFilesSent","EcsAssignmentsProcessed","EcsPositionsProcessed","EcsTradesProcessed"] as String[]] as Object[];
    def map = server.invoke(batchService, "batchGetNumerics", args, genericSig);
    map.each() { k, v ->
        def attrs = jvmMap();
        // record = { on, attrs, valueMap,  fmt ->
        prepare(k, attrs, v,  buff);
    }
}

collectFileWriters = { server, buff ->    
    def args = [new ObjectName("com.cpex.clearing.fixml.matchoff:service=XMLFileBufferFactory"), null, null, ["DestroyedBufferCount","CurrentBufferCount","CreatedBufferCount","ValidationFailures"] as String[]] as Object[];
    def map = server.invoke(batchService, "batchGetNumerics", args, genericSig);
    map.each() { k, v ->
        def attrs = jvmMap();
        // record = { on, attrs, valueMap,  fmt ->
        prepare(k, attrs, v,  buff);
        //println "$k : $v";
    }
}

collectFileWriters = { server, buff ->    
    def args = [new ObjectName("com.cpex.clearing.fixml.matchoff:service=XMLFileBufferFactory"), null, null, ["DestroyedBufferCount","CurrentBufferCount","CreatedBufferCount","ValidationFailures"] as String[]] as Object[];
    def map = server.invoke(batchService, "batchGetNumerics", args, genericSig);
    map.each() { k, v ->
        def attrs = jvmMap();
        // record = { on, attrs, valueMap,  fmt ->
        prepare(k, attrs, v,  buff);
        //println "$k : $v";
    }
}



try {
    rman = RiemannClient.tcp("10.12.114.48", 5555);
    rman.connect();
    connection = connect(bridgeHost, port);
    server = connection.getMBeanServerConnection();
    try { server.createMBean("sun.management.HotspotInternal", new ObjectName("sun.management:type=HotspotInternal")); } catch (e) {}
    socket = new Socket("pdk-pt-cupas-01", 4242);
    println "Connected";
    int buffSize = 1024;
    while(true) {
        long start = System.currentTimeMillis();
        buff = new StringBuilder(buffSize);
        collectMem(server, buff);
        collectMemPools(server, buff);
        collectClassLoading(server, buff);
        collectCompilation(server, buff);
        collectGc(server, buff);
        collectOs(server, buff);
        collectThreads(server, buff);
        collectEwmas(server, buff);
        collectDataSources(server, buff);
        collectThreadPools(server, buff);
        collectJobStats(server, buff);
        collectFileWriters(server, buff);        
        collectFileWriters(server, buff);
        //record(buff, socket);
        int bsize = buff.length();
        if(bsize > buffSize) {
            println "Raising buffer size to [$bsize]";
            buffSize = bsize;
        }
        long elapsed = System.currentTimeMillis()-start;
        println "\tCollection Time: $elapsed ms.";
        Thread.sleep(5000);
    }
} catch (InterruptedException ie) {
    println "Collection Stopped";
} finally {
    try { connection.close(); println "Connection Closed"; } catch (e) {}
    try { socket.close(); println "Socket Closed"; } catch (e) {}
    try { rman.disconnect(); println "RMan Client Closed"; } catch (e) {}
}



return null;