import ch.ethz.ssh2.*;
import java.text.SimpleDateFormat;
import java.util.regex.*;
import java.util.concurrent.*;
import javax.management.remote.*;
import javax.management.*;
import java.lang.management.*;


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

record = { on, attrs, valueMap,  socket ->
   
    try {
        
        def now = TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);        
        on.getKeyPropertyList().each() { k,v ->
            attrs.put(k.replace(" ", ""), v.replace(" ", ""));
        }
        if(attrs.size() > 8) {
            println "Attr Map to big: [${attrs.size()}] $attrs";
        }
        def tags = attrs.toString().replace(",", "").replace("[", "").replace("]", "").replace(":", "=");
        def b = new StringBuilder();
        valueMap.each() { name, value -> 
            b.append("put $name $now $value $tags\n");
        }
        //println b.toString();
        socket << b.toString();
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

collectMemPools = { server, socket ->
    def compKeyType = "memType";
    def args = [new ObjectName("java.lang:type=MemoryPool,name=*"), null, null, ["Usage", "PeakUsage"] as String[]] as Object[];
    def map = server.invoke(batchService, "batchGetNumerics", args, genericSig);
    map.each() { k, v ->
        def attrs = jvmMap();
        cleanComposites(v, attrs, "memType");
        // record = { on, attrs, valueMap,  fmt ->
        record(k, attrs, v,  socket);
    }
}
collectGc = { server, socket ->
    def args = [new ObjectName("java.lang:type=GarbageCollector,name=*"), null, null, ["CollectionCount", "CollectionTime"] as String[]] as Object[];
    def map = server.invoke(batchService, "batchGetNumerics", args, genericSig);
    map.each() { k, v ->
        def attrs = jvmMap();
        // record = { on, attrs, valueMap,  fmt ->
        record(k, attrs, v,  socket);
    }
}
collectOs = { server, socket ->
    def args = [new ObjectName("java.lang:type=OperatingSystem"), null, null, ["OpenFileDescriptorCount", "CommittedVirtualMemorySize", "FreePhysicalMemorySize", "FreeSwapSpaceSize", "SystemLoadAverage", "SystemCpuLoad", "ProcessCpuLoad"] as String[]] as Object[];
    def map = server.invoke(batchService, "batchGetNumerics", args, genericSig);
    map.each() { k, v ->
        def attrs = jvmMap();
        // record = { on, attrs, valueMap,  fmt ->
        record(k, attrs, v,  socket);
    }
}

collectMem = { server, socket ->
    def compKeyType = "memType";
    def args = [new ObjectName("java.lang:type=Memory"), null, null, ["HeapMemoryUsage", "NonHeapMemoryUsage"] as String[]] as Object[];
    def map = server.invoke(batchService, "batchGetNumerics", args, genericSig);
    map.each() { k, v ->
        def attrs = jvmMap();
        cleanComposites(v, attrs, "memType");
        record(k, attrs, v,  socket);
    }
}
collectClassLoading = { server, socket ->
    def args = [new ObjectName("java.lang:type=ClassLoading"), null, null, ["TotalLoadedClassCount", "LoadedClassCount", "UnloadedClassCount"] as String[]] as Object[];
    def map = server.invoke(batchService, "batchGetNumerics", args, genericSig);
    map.each() { k, v ->
        def attrs = jvmMap();
        record(k, attrs, v,  socket);
    }
}
collectCompilation = { server, socket ->
    def args = [new ObjectName("java.lang:type=Compilation"), null, null, ["TotalCompilationTime"] as String[]] as Object[];
    def map = server.invoke(batchService, "batchGetNumerics", args, genericSig);
    map.each() { k, v ->
        def attrs = jvmMap();
        record(k, attrs, v,  socket);
    }
}
collectThreads = { server, socket ->
    def args = [new ObjectName("java.lang:type=Threading"), null, null, ["ThreadCount", "PeakThreadCount", "DaemonThreadCount", "TotalStartedThreadCount"] as String[]] as Object[];
    def map = server.invoke(batchService, "batchGetNumerics", args, genericSig);
    map.each() { k, v ->
        def attrs = jvmMap();
        // record = { on, attrs, valueMap,  fmt ->
        record(k, attrs, v,  socket);
    }
}

collectEwmas = { server, socket ->
    def args = ["com.cpex.clearing.fixml.matchoff.stats.unsafe.DirectEWMAMBean", null, null, ["*"] as String[]] as Object[];
    def map = server.invoke(batchService, "batchGetNumerics", args, ewmaSig);
    map.each() { k, v ->
        def attrs = jvmMap();
        // record = { on, attrs, valueMap,  fmt ->
        record(k, attrs, v,  socket);
    }    
}

session = null;
connection = null;


try {
    connection = connect(bridgeHost, port);
    server = connection.getMBeanServerConnection();
    try { server.createMBean("sun.management.HotspotInternal", new ObjectName("sun.management:type=HotspotInternal")); } catch (e) {}
    socket = new Socket("pdk-pt-cupas-01", 4242);
    println "Connected";
    while(true) {
        long start = System.currentTimeMillis();
        
        collectMem(server, socket);
        collectMemPools(server, socket);
        collectClassLoading(server, socket);
        collectCompilation(server, socket);
        collectGc(server, socket);
        collectOs(server, socket);
        collectThreads(server, socket);
        collectEwmas(server, socket);
        long elapsed = System.currentTimeMillis()-start;
        println "\tCollection Time: $elapsed ms.";
        Thread.sleep(5000);
    }
} catch (InterruptedException ie) {
    println "Collection Stopped";
} finally {
    try { connection.close(); println "Connection Closed"; } catch (e) {}
    try { socket.close(); println "Socket Closed"; } catch (e) {}
}



return null;