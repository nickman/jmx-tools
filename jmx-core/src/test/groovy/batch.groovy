import javax.management.*;
import javax.management.remote.*;

JMXServiceURL surl = new JMXServiceURL("service:jmx:jmxmp://localhost:8024");
JMXConnector connector = null;
ObjectName batchService = new ObjectName("com.cpex.clearing.fixml.matchoff.stats.batch:service=BatchAttributeService");
String iface = "com.cpex.clearing.fixml.matchoff.stats.unsafe.DirectEWMAMBean";
//String[] ewmaAttrs = ["Average", "Count", "Maximum", "Mean", "Minimum"] as String[];
String[] ewmaAttrs = ["*"] as String[];
String[] ewmaSig = [String.class.getName(), String.class.getName(), String.class.getName(), String[].class.getName()] as String[];
String[] genericSig = [ObjectName.class.getName(), String.class.getName(), String.class.getName(), String[].class.getName()] as String[];

//batchGetNumerics(ObjectName filter, String preOp, String postOp, String...attrNames) {

try {
    connector = JMXConnectorFactory.connect(surl);
    println "Connected";
    server = connector.getMBeanServerConnection();
    println server.getDefaultDomain();
    args = [iface, null, "reset", ewmaAttrs] as Object[];
    
    Map<ObjectName, Map<String, Number>> map = server.invoke(batchService, "batchGetNumerics", args, ewmaSig);
    map.each() { k, v ->
        println "[$k] : $v";
    }
    args = [new ObjectName("java.lang:type=GarbageCollector,name=*"), null, null, ["CollectionCount", "CollectionTime"] as String[]] as Object[];

    map = server.invoke(batchService, "batchGetNumerics", args, genericSig);
    map.each() { k, v ->
        println "[$k] : $v";
    }
    args = [new ObjectName("java.lang:type=Memory"), null, null, ["HeapMemoryUsage", "NonHeapMemoryUsage"] as String[]] as Object[];
    //args = [new ObjectName("java.lang:type=Memory"), null, null, ["*"] as String[]] as Object[];
    map = server.invoke(batchService, "batchGetNumerics", args, genericSig);
    map.each() { k, v ->
        println "[$k] : $v";
    }
    args = [new ObjectName("java.lang:type=MemoryPool,name=*"), null, null, ["Usage", "PeakUsage"] as String[]] as Object[];
    map = server.invoke(batchService, "batchGetNumerics", args, genericSig);
    map.each() { k, v ->
        println "[$k] : $v";
    }
    
    println "===========================================================================================================";
    
    // public Map<ObjectName, Map<String, Object>> batchGet(String attributeFilter, QueryExp queryExp, ObjectName...mbeanFilters) {
    sig = [String.class.getName(), "javax.management.QueryExp", ObjectName[].class.getName()] as String[];
    println sig;
    args = ["n:", Query.isInstanceOf(new StringValueExp(Object.class.getName())), [new ObjectName("*:*")] as ObjectName[]] as Object[];
    map = server.invoke(batchService, "batchGet", args, sig);
    map.each() { k, v ->
        println "[$k] : $v";
    }
    
    
} finally {
    try { connector.close(); } catch (e) {}
}

return null;