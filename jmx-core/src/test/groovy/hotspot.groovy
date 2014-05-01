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


//sun.management.counter.perf.StringCounterSnapshot : 13
//sun.management.counter.perf.LongCounterSnapshot : 108


connect = { host, port ->
    def serviceURL = new JMXServiceURL("service:jmx:jmxmp://$host:$port");
    def connection = JMXConnectorFactory.newJMXConnector(serviceURL, null);
    connection.connect();
    return connection;
}

try {
    connection = connect(bridgeHost, port);
    server = connection.getMBeanServerConnection();
    println "Connected";
    counters = server.getAttribute(new ObjectName("sun.management:type=HotspotMemory"), "InternalMemoryCounters");
    counters.each() { println it; }
} catch (InterruptedException ie) {
    println "Collection Stopped";
} finally {
    try { connection.close(); println "Connection Closed"; } catch (e) {}
}
