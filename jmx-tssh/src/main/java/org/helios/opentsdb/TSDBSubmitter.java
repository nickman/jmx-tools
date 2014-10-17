/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2014, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package org.helios.opentsdb;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.helios.jmx.util.helpers.JMXHelper;
import org.helios.jmx.util.helpers.StringHelper;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.DirectChannelBufferFactory;

/**
 * <p>Title: TSDBSubmitter</p>
 * <p>Description: A Telnet TCP client for buffering and sending metrics to OpenTSDB</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.opentsdb.TSDBSubmitter</code></p>
 * TODO:
 * fluent metric builder
 * jmx response trace formatters
 * 		transforms
 * 		object name matcher
 * 		formatter cache and invoker
 * Delta Service
 * terminal output parser -> trace
 * disconnector & availability trace
 * root tags
 * dup checks mode
 * off line accumulator and flush on connect
 * option for TSDB time as seconds or milliseconds
 * option to log all traced metrics
 */

public class TSDBSubmitter {
	/** The OpenTSDB host */
	protected final String host;
	/** The OpenTSDB port */
	protected final int port;
	/** The socket connected to the host/port */
	protected Socket socket = null;
	/** The socket keep-alive flag */
	protected boolean keepAlive = true;
	/** The socket re-use address flag */
	protected boolean reuseAddress = true;
	/** The socket linger flag */
	protected boolean linger = false;
	/** The socket tcp nodelay flag */
	protected boolean tcpNoDelay = true;	
	/** Indicates if times are traced in seconds (true) or milliseconds (false) */
	protected boolean traceInSeconds = true;
	/** Indicates if traces should be logged */
	protected boolean logTraces = false;
	/** Indicates if traces are disabled */
	protected boolean disableTraces = false;
	
	/** The ObjectName transform cache */
	protected final TransformCache transformCache = new TransformCache();
	
	/** The root tags applied to all traced metrics */
	protected final Set<String> rootTags = new LinkedHashSet<String>();
	
	/** Deltas for long values */
	protected final NonBlockingHashMap<String, Long> longDeltas = new NonBlockingHashMap<String, Long>(); 
	/** Deltas for double values */
	protected final NonBlockingHashMap<String, Double> doubleDeltas = new NonBlockingHashMap<String, Double>(); 
	/** Deltas for int values */
	protected final NonBlockingHashMap<String, Integer> intDeltas = new NonBlockingHashMap<String, Integer>(); 
	
	/** The socket receive buffer size in bytes */
	protected int receiveBufferSize = DEFAULT_RECEIVE_BUFFER_SIZE;
	/** The socket send buffer size in bytes */
	protected int sendBufferSize = DEFAULT_SEND_BUFFER_SIZE;
	/** The socket linger time in seconds */
	protected int lingerTime = -1;
	/** The socket timeout in milliseconds */
	protected int timeout = 1000;
	
	/** The socket output stream */
	protected OutputStream os = null;
	/** The socket input stream */
	protected InputStream is = null;
	/** The buffer for incoming data */
	protected final ChannelBuffer dataBuffer = ChannelBuffers.dynamicBuffer(bufferFactory);
	
	/** The default socket send buffer size in bytes */
	public static final int DEFAULT_SEND_BUFFER_SIZE;
	/** The default socket receive buffer size in bytes */
	public static final int DEFAULT_RECEIVE_BUFFER_SIZE;
	
	/** The default character set */
	public static final Charset CHARSET = Charset.forName("UTF-8");
	
	/** The buffered data direct buffer factory */
	private static final DirectChannelBufferFactory bufferFactory = new DirectChannelBufferFactory(1024); 
	
	static {
		@SuppressWarnings("resource")
		Socket t = new Socket();
		try {
			DEFAULT_SEND_BUFFER_SIZE = t.getSendBufferSize();
			DEFAULT_RECEIVE_BUFFER_SIZE = t.getReceiveBufferSize();
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * System out logger
	 * @param fmt Message format
	 * @param args Message tokens
	 */
	public static void log(final Object fmt, final Object...args) {
		System.out.println(String.format(fmt.toString(), args));
	}
	
	
	/**
	 * Creates a new TSDBSubmitter
	 * @param host The OpenTSDB host or ip address
	 * @param port The OpenTSDB listening port 
	 */
	public TSDBSubmitter(final String host, final int port) {
		this.host = host;
		this.port = port;
		socket = new Socket();
	}
	
	/**
	 * Creates a new TSDBSubmitter using the default OpenTSDB port
	 * @param host The OpenTSDB host or ip address
	 */
	public TSDBSubmitter(final String host) {
		this(host, 4242);
	}
	
	public static void main(String[] args) {
		log("Submitter Test");
		final MBeanServer ser = ManagementFactory.getPlatformMBeanServer();
//		TSDBSubmitter submitter = new TSDBSubmitter("opentsdb", 8080);
		TSDBSubmitter submitter = new TSDBSubmitter("localhost").addRootTag("host", "nicholas").addRootTag("app", "MyApp").setLogTraces(true).setTimeout(2000).connect();
		log(submitter);
		while(true) {
			final long start = System.currentTimeMillis();
			final Map<ObjectName, Map<String, Object>> mbeanMap = new HashMap<ObjectName, Map<String, Object>>(ser.getMBeanCount());
			for(ObjectName on: ser.queryNames(null, null)) {
				try {
					MBeanInfo minfo = ser.getMBeanInfo(on);
					MBeanAttributeInfo[] ainfos = minfo.getAttributes();					
					String[] attrNames = new String[ainfos.length];
					for(int i = 0; i < ainfos.length; i++) {
						attrNames[i] = ainfos[i].getName();
					}
					AttributeList attrList = ser.getAttributes(on, attrNames);
					Map<String, Object> attrValues = new HashMap<String, Object>(attrList.size());
					for(Attribute a: attrList.asList()) {
						attrValues.put(a.getName(), a.getValue());
					}
					mbeanMap.put(on, attrValues);
				} catch (Exception ex) {
					ex.printStackTrace(System.err);
				}
			}
			log("Tracing All Attributes for [%s] MBeans", mbeanMap.size());
			submitter.trace(mbeanMap);
			
			
//			for(final MemoryPoolMXBean pool: ManagementFactory.getMemoryPoolMXBeans()) {
//				final MemoryUsage mu = pool.getUsage();			
//				final String poolName = pool.getName();
//				submitter.trace("used", mu.getUsed(), "type", "MemoryPool", "name", poolName);
//				submitter.trace("max", mu.getMax(), "type", "MemoryPool", "name", poolName);
//				submitter.trace("committed", mu.getCommitted(), "type", "MemoryPool", "name", poolName);
//			}
//			for(final GarbageCollectorMXBean gc: ManagementFactory.getGarbageCollectorMXBeans()) {
//				final ObjectName on = gc.getObjectName();
//				final String gcName = gc.getName();
//				final Long count = submitter.longDelta(gc.getCollectionCount(), on.toString(), gcName, "count");
//				final Long time = submitter.longDelta(gc.getCollectionTime(), on.toString(), gcName, "time");
//				if(count!=null) {
//					submitter.trace("collectioncount", count, "type", "GarbageCollector", "name", gcName);
//				}
//				if(time!=null) {
//					submitter.trace("collectiontime", time, "type", "GarbageCollector", "name", gcName);
//				}
//			}
			submitter.flush();
			final long elapsed = System.currentTimeMillis() - start;
			log("Completed flush in %s ms", elapsed);
			System.gc();
			try { Thread.currentThread().join(5000); } catch (Exception x) {/* No Op */}
		}		
	}
	
	/**
	 * Indicates if the submitter is connected
	 * @return true if connected, false otherwise
	 */
	public boolean isConnected() {
		return socket.isConnected();
	}
	
	/**
	 * Connects this submitter.
	 * Does nothing if already connected.
	 * @return this submitter
	 * @throws RuntimeException if connection fails
	 */
	public TSDBSubmitter connect() {
		try {
			if(socket.isConnected()) return this;
			log("Connecting to [%s:%s]....", host, port);
			socket = new Socket();
			socket.setKeepAlive(keepAlive);
			socket.setReceiveBufferSize(receiveBufferSize);
			socket.setSendBufferSize(sendBufferSize);
			socket.setReuseAddress(reuseAddress);
			socket.setSoLinger(linger, lingerTime);
			socket.setSoTimeout(timeout);
			socket.setTcpNoDelay(tcpNoDelay);
			socket.connect(new InetSocketAddress(host, port));
			log("Connected to [%s:%s]", host, port);
			os = socket.getOutputStream();
			is = socket.getInputStream();
			log("Version: %s", getVersion());
			return this;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to connect to [" + host + ":" + port + "]", ex);
		}
	}
	
	private final Set<StringBuilder> SBs = new CopyOnWriteArraySet<StringBuilder>();
	
	private final ThreadLocal<StringBuilder> SB = new ThreadLocal<StringBuilder>() {
		@Override
		protected StringBuilder initialValue() {
			final StringBuilder b = new StringBuilder(1024);
			SBs.add(b);
			return b;
		}
	};
	
	private StringBuilder getSB() {
		StringBuilder b = SB.get();
		b.setLength(0);
		return b;
	}
	
	/**
	 * Returns the current time.
	 * @return the current time in seconds if {@link #traceInSeconds} is true, otherwise in milliseconds
	 */
	public long time() {
		return traceInSeconds ? System.currentTimeMillis()/1000 : System.currentTimeMillis(); 
	}
	
	/**
	 * Traces a double metric
	 * @param metric The metric name
	 * @param value The value
	 * @param tags The metric tags
	 */
	public void trace(final String metric, final double value, final Map<String, String> tags) {
		// put $metric $now $value host=$HOST "
		StringBuilder b = getSB();
		b.append("put ").append(clean(metric)).append(" ").append(time()).append(" ").append(value).append(" ");
		appendRootTags(b);
		for(Map.Entry<String, String> entry: tags.entrySet()) {
			b.append(clean(entry.getKey())).append("=").append(clean(entry.getValue())).append(" ");
		}
		final byte[] trace = b.deleteCharAt(b.length()-1).append("\n").toString().getBytes(CHARSET);
		synchronized(dataBuffer) {
			dataBuffer.writeBytes(trace);
		}
	}
	
	/**
	 * Traces a double metric
	 * @param metric The full metric as a JMX {@link ObjectName}
	 * @param value The value
	 */
	public void trace(final ObjectName metric, final double value) {
		if(metric==null) throw new IllegalArgumentException("The passed ObjectName was null");
		if(metric==null || metric.isPattern()) return;
		StringBuilder b = getSB();
		b.append("put ").append(clean(metric.getDomain())).append(" ").append(time()).append(" ").append(value).append(" ");
		appendRootTags(b);
		for(Map.Entry<String, String> entry: metric.getKeyPropertyList().entrySet()) {
			b.append(clean(entry.getKey())).append("=").append(clean(entry.getValue())).append(" ");
		}
		final String s = b.deleteCharAt(b.length()-1).append("\n").toString();
		if(logTraces) log("Trace: [%s]", s.trim());
		final byte[] trace = s.getBytes(CHARSET);		
		synchronized(dataBuffer) {
			dataBuffer.writeBytes(trace);
		}
	}
	
	
	/**
	 * Traces a double metric
	 * @param metric The metric name
	 * @param value The value
	 * @param tags The metric tags
	 */
	public void trace(final String metric, final double value, final String...tags) {
		if(tags==null) return;
		if(tags.length%2!=0) throw new IllegalArgumentException("The tags varg " + Arrays.toString(tags) + "] has an odd number of values");
		final int pairs = tags.length/2;
		final Map<String, String> map = new LinkedHashMap<String, String>(pairs);
		for(int i = 0; i < tags.length; i++) {
			map.put(tags[i], tags[++i]);
		}
		trace(metric, value, map);
	}
	
	
	/**
	 * Traces a long metric
	 * @param metric The metric name
	 * @param value The value
	 * @param tags The metric tags
	 */
	public void trace(final String metric, final long value, final Map<String, String> tags) {
		StringBuilder b = getSB();
		b.append("put ").append(clean(metric)).append(" ").append(time()).append(" ").append(value).append(" ");
		appendRootTags(b);
		for(Map.Entry<String, String> entry: tags.entrySet()) {
			b.append(clean(entry.getKey())).append("=").append(clean(entry.getValue())).append(" ");
		}
		final String s = b.deleteCharAt(b.length()-1).append("\n").toString();
		if(logTraces) log("Trace: [%s]", s.trim());
		final byte[] trace = s.getBytes(CHARSET);
		synchronized(dataBuffer) {
			dataBuffer.writeBytes(trace);
		}
	}
	
	/**
	 * Traces a long metric
	 * @param metric The full metric as a JMX {@link ObjectName}
	 * @param value The value
	 */
	public void trace(final ObjectName metric, final long value) {
		if(metric==null || metric.isPattern()) return;
		StringBuilder b = getSB();
		b.append("put ").append(clean(metric.getDomain())).append(" ").append(time()).append(" ").append(value).append(" ");
		appendRootTags(b);
		for(Map.Entry<String, String> entry: metric.getKeyPropertyList().entrySet()) {
			b.append(clean(entry.getKey())).append("=").append(clean(entry.getValue())).append(" ");
		}
		final String s = b.deleteCharAt(b.length()-1).append("\n").toString();
		if(logTraces) log("Trace: [%s]", s.trim());
		final byte[] trace = s.getBytes(CHARSET);
		synchronized(dataBuffer) {
			dataBuffer.writeBytes(trace);
		}
	}
	
	/**
	 * Traces raw JMX BatchService lookup results
	 * @param batchResults A map of JMX attribute values keyed by the attribute name within a map keyed by the ObjectName
	 */
	public void trace(final Map<ObjectName, Map<String, Object>> batchResults) {
		if(batchResults==null || batchResults.isEmpty()) return;
		for(Map.Entry<ObjectName, Map<String, Object>> entry: batchResults.entrySet()) {
			final ObjectName on = entry.getKey();
			TSDBJMXResultTransformer transformer = transformCache.getTransformer(on);
			if(transformer!=null) {
				 Map<ObjectName, Number> transformed = transformer.transform(on, entry.getValue());
				 for(Map.Entry<ObjectName, Number> t: transformed.entrySet()) {
					 final Number v = t.getValue();
					 if(v==null) continue;
					 if(v instanceof Double) {
						 trace(t.getKey(), v.doubleValue());
					 } else {
						 trace(t.getKey(), v.longValue());
					 }
				 }
			} else {
				for(Map.Entry<String, Object> attr: entry.getValue().entrySet()) {
					final Object v = attr.getValue();
					if(v==null) continue;
					if(v instanceof Number) {
						if(v instanceof Double) {
							trace(JMXHelper.objectName(clean(new StringBuilder(on.toString()).append(",metric=").append(attr.getKey()))), ((Number)v).doubleValue());
						} else {
							trace(JMXHelper.objectName(clean(new StringBuilder(on.toString()).append(",metric=").append(attr.getKey()))), ((Number)v).longValue());
						}
					} else if(v instanceof CompositeData) {
						Map<ObjectName, Number> cmap = fromOpenType(on, (CompositeData)v);
						for(Map.Entry<ObjectName, Number> ce: cmap.entrySet()) {
							final Number cv = ce.getValue();
							if(v instanceof Double) {
								trace(ce.getKey(), cv.doubleValue());
							} else {
								trace(ce.getKey(), cv.longValue());
							}
						}
					} 
				}
			}
		}
	}
	
	
	/**
	 * Decomposes a composite data type so it can be traced
	 * @param objectName The ObjectName of the MBean the composite data came from
	 * @param cd The composite data instance
	 * @return A map of values keyed by synthesized ObjectNames that represent the structure down to the numeric composite data items
	 */
	protected Map<ObjectName, Number> fromOpenType(final ObjectName objectName, final CompositeData cd) {
		if(objectName==null) throw new IllegalArgumentException("The passed ObjectName was null");
		if(cd==null) throw new IllegalArgumentException("The passed CompositeData was null");
		final Map<ObjectName, Number> map = new HashMap<ObjectName, Number>();
		final CompositeType ct = cd.getCompositeType();
		for(final String key: ct.keySet()) {
			final Object value = cd.get(key);
			if(value==null || !(value instanceof Number)) continue;
			StringBuilder b = new StringBuilder(objectName.toString());
			b.append(",ctype=").append(simpleName(ct.getTypeName()));
			b.append(",metric=").append(key);			
			ObjectName on = JMXHelper.objectName(clean(b));
			map.put(on, (Number)value);
			
		}
		return map;
	}
	
	private static String clean(final CharSequence s) {
		if(s==null) return null;
		return s.toString().trim().replace(" ", "");
	}
	
	private static String simpleName(final CharSequence s) {
		if(s==null) return null;
		String str = clean(s);
		final int index = str.lastIndexOf('.');
		return index==-1 ? str : str.substring(index+1);
	}
	

	
	/**
	 * Traces a long metric
	 * @param metric The metric name
	 * @param value The value
	 * @param tags The metric tags
	 */
	public void trace(final String metric, final long value, final String...tags) {
		if(tags==null) return;
		if(tags.length%2!=0) throw new IllegalArgumentException("The tags varg " + Arrays.toString(tags) + "] has an odd number of values");
		final int pairs = tags.length/2;
		final Map<String, String> map = new LinkedHashMap<String, String>(pairs);
		for(int i = 0; i < tags.length; i++) {
			map.put(tags[i], tags[++i]);
		}
		trace(metric, value, map);
	}
	
	

	/**
	 * Flushes the databuffer to the socket output stream and on success, clears the data buffer
	 * @return the number of bytes flushed
	 */
	public int[] flush() {
		final int[] bytesWritten = new int[]{0, 0};
//		GZIPOutputStream gzip = null;
		synchronized(dataBuffer) {
			if(dataBuffer.readableBytes()<1) return bytesWritten;
			int pos = -1;
			try {				
				final int r = dataBuffer.readableBytes();
//				gzip = new GZIPOutputStream(os, r * 2);
				pos = dataBuffer.readerIndex();
				dataBuffer.readBytes(os, dataBuffer.readableBytes());
//				gzip.finish();
//				gzip.flush();
				os.flush();
				dataBuffer.clear();
				bytesWritten[0] = r;
				log("Flushed %s bytes", r);
			} catch (Exception ex) {
				log("Failed to flush. Stack trace follows...");
				ex.printStackTrace(System.err);
				if(pos!=-1) dataBuffer.readerIndex(pos);
			} finally {
//				if(gzip!=null) try { gzip.close(); } catch (Exception x) {/* No Op */}
			}
		}
		return bytesWritten;
	}
	/**
	 * Returns the connected OpenTSDB version
	 * @return the connected OpenTSDB version
	 */
	public String getVersion() {
		if(!socket.isConnected()) {
			return "Not Connected";
		}
		try {
			os.write("version\n".getBytes());
			os.flush();
			byte[] b = new byte[1024];
			int bytesRead = is.read(b);
			return new String(b, 0, bytesRead);				
		} catch (Exception x) {
			return "Failed to get version from [" + host + ":" + port + "]" + x;
		}
	}
	
	/**
	 * Closes the submitter's connection
	 */
	public void close() {
		try {
			socket.close();
		} catch (Exception x) {
			/* No Op */
		} finally {
			for(StringBuilder b: SBs) {
				b.setLength(0);
				b.trimToSize();
			}
		}
	}
	
	/**
	 * Computes the positive delta between the submitted value and the prior value for the same id
	 * @param value The value to compute the delta for
	 * @param id The id of the delta, a compound array of names which will be concatenated
	 * @return the delta value, or null if this was the first submission for the id, or the delta was reset
	 */
	public Double doubleDelta(final double value, final String...id) {
		Double state = null;
		if(id==null || id.length==0) return null;
		if(id.length==1) {
			state = doubleDeltas.put(id[0], value);
		} else {
			state = doubleDeltas.put(StringHelper.fastConcat(id), value);			
		}
		if(state!=null) {
			double delta = value - state;
			if(delta<0) {
				return null;
			}
			return delta;
		}
		return null;		
	}
	
	/**
	 * Computes the positive delta between the submitted value and the prior value for the same id
	 * @param value The value to compute the delta for
	 * @param id The id of the delta, a compound array of names which will be concatenated
	 * @return the delta value, or null if this was the first submission for the id, or the delta was reset
	 */
	public Long longDelta(final long value, final String...id) {
		Long state = null;
		if(id==null || id.length==0) return null;
		if(id.length==1) {
			state = longDeltas.put(id[0], value);
		} else {
			state = longDeltas.put(StringHelper.fastConcat(id), value);			
		}
		if(state!=null) {
			long delta = value - state;
			if(delta<0) {
				return null;
			}
			return delta;
		}
		return null;		
	}
	
	/**
	 * Computes the positive delta between the submitted value and the prior value for the same id
	 * @param value The value to compute the delta for
	 * @param id The id of the delta, a compound array of names which will be concatenated
	 * @return the delta value, or null if this was the first submission for the id, or the delta was reset
	 */
	public Integer longInteger(final int value, final String...id) {
		Integer state = null;
		if(id==null || id.length==0) return null;
		if(id.length==1) {
			state = intDeltas.put(id[0], value);
		} else {
			state = intDeltas.put(StringHelper.fastConcat(id), value);			
		}
		if(state!=null) {
			int delta = value - state;
			if(delta<0) {
				return null;
			}
			return delta;
		}
		return null;		
	}
	
	
	/**
	 * Flushes all the delta states
	 */
	public void flushDeltas() {
		longDeltas.clear();
		intDeltas.clear();
		doubleDeltas.clear();
	}
	
	

	/**
	 * Sets the keep alive flag on the socket 
	 * @param keepAlive true to enable SO_KEEPALIVE, false otherwise
	 * @return this submitter
	 */
	public TSDBSubmitter setKeepAlive(boolean keepAlive) {
		this.keepAlive = keepAlive;
		return this;
	}

	/**
	 * Sets the socket address reuse
	 * @param reuseAddress true to reuse address, false otherwise
	 * @return this submitter
	 */
	public TSDBSubmitter setReuseAddress(final boolean reuseAddress) {
		this.reuseAddress = reuseAddress;
		return this;
	}



	/**
	 * Sets the socket tcp no delay
	 * @param tcpNoDelay true to enable no-delay, false to disable (and enable Nagle's algorithm)
	 * @return this submitter
	 */
	public TSDBSubmitter setTcpNoDelay(final boolean tcpNoDelay) {
		this.tcpNoDelay = tcpNoDelay;
		return this;
	}

	/**
	 * Sets the socket receive buffer size in bytes
	 * @param receiveBufferSize the receive buffer size to set
	 * @return this submitter
	 */
	public TSDBSubmitter setReceiveBufferSize(final int receiveBufferSize) {
		this.receiveBufferSize = receiveBufferSize;
		return this;
	}

	/**
	 * Sets the socket send buffer size in bytes
	 * @param sendBufferSize the send buffer size to set
	 * @return this submitter
	 */
	public TSDBSubmitter setSendBufferSize(final int sendBufferSize) {
		this.sendBufferSize = sendBufferSize;
		return this;
	}

	/**
	 * Indicates if a socket should linger after close, and if so, for how long in seconds.
	 * @param linger true to linger, false otherwise
	 * @param lingerTime the lingerTime to set
	 * @return this submitter
	 */
	public TSDBSubmitter setLingerTime(final boolean linger, final int lingerTime) {
		this.linger = linger;
		this.lingerTime = linger ? lingerTime : -1;
		return this;		
	}

	/**
	 * Sets the socket timeout in ms.
	 * @param timeout the timeout to set
	 * @return this submitter
	 */
	public TSDBSubmitter setTimeout(final int timeout) {
		this.timeout = timeout;
		return this;
	}
	
	
	
	
	
	/**
	 * Returns the target host
	 * @return the target host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Returns the target port
	 * @return the target port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Returns the socket keep alive
	 * @return the keepAlive
	 */
	public boolean isKeepAlive() {
		return keepAlive;
	}

	/**
	 * Returns the socket address reuse flag
	 * @return the reuseAddress
	 */
	public boolean isReuseAddress() {
		return reuseAddress;
	}

	/**
	 * Returns the socket linger flag
	 * @return the linger
	 */
	public boolean isLinger() {
		return linger;
	}

	/**
	 * Returns the tcp no delay
	 * @return the tcpNoDelay
	 */
	public boolean isTcpNoDelay() {
		return tcpNoDelay;
	}

	/**
	 * Returns the socket receive buffer size in bytes
	 * @return the receiveBufferSize
	 */
	public int getReceiveBufferSize() {
		return receiveBufferSize;
	}

	/**
	 * Returns the socket send buffer size in bytes
	 * @return the sendBufferSize
	 */
	public int getSendBufferSize() {
		return sendBufferSize;
	}

	/**
	 * Returns the socket linger time in seconds
	 * @return the lingerTime
	 */
	public int getLingerTime() {
		return lingerTime;
	}

	/**
	 * Returns the socket timeout in milliseconds
	 * @return the timeout
	 */
	public int getTimeout() {
		return timeout;
	}


	/**
	 * Indicates if TSDB times are traced in seconds, or milliseconds
	 * @return true if TSDB times are traced in seconds, false if in milliseconds 
	 */
	public boolean isTraceInSeconds() {
		return traceInSeconds;
	}


	/**
	 * Sets the tracing time unit
	 * @param traceInSeconds true to trace in seconds, false to trace in milliseconds
	 * @return this submitter
	 */
	public TSDBSubmitter setTraceInSeconds(final boolean traceInSeconds) {
		this.traceInSeconds = traceInSeconds;
		return this;
	}


	/**
	 * Returns the root tags
	 * @return the rootTags
	 */
	public Set<String> getRootTags() {
		return Collections.unmodifiableSet(rootTags);
	}
	
	/**
	 * Adds a root tag
	 * @param key The root tag key
	 * @param value The root tag value
	 * @return this submitter
	 */
	public TSDBSubmitter addRootTag(final String key, final String value) {
		if(key==null || key.trim().isEmpty()) throw new IllegalArgumentException("The passed key was null or empty");
		if(value==null || value.trim().isEmpty()) throw new IllegalArgumentException("The passed value was null or empty");
		rootTags.add(clean(key) + "=" + clean(value));
		return this;
	}
	
	/**
	 * Appends the root tags to the passed buffer
	 * @param b The buffer to append to
	 * @return the same buffer
	 */
	protected StringBuilder appendRootTags(final StringBuilder b) {
		if(b==null) throw new IllegalArgumentException("The passed string builder was null");
		if(!rootTags.isEmpty()) {
			for(String tag: rootTags) {
				b.append(tag).append(" ");
			}
		}
		return b;
	}
	
	/**
	 * Indicates if traces are being logged 
	 * @return true if traces are being logged, false otherwise
	 */
	public boolean isLogTraces() {
		return logTraces;
	}


	/**
	 * Sets the trace logging
	 * @param logTraces true to trace logs, false otherwise
	 * @return this submitter
	 */
	public TSDBSubmitter setLogTraces(final boolean logTraces) {
		this.logTraces = logTraces;
		return this;
	}
	
	/**
	 * Indicates if tracing is disabled
	 * @return true if tracing is disabled, false otherwise
	 */
	public boolean isTracingDisabled() {
		return disableTraces;
	}


	/**
	 * Enables or disables actual tracing. To view what would be traced,
	 * without actually tracing, set {@link #setLogTraces(boolean)} to true
	 * and this to false;
	 * @param disableTraces true to disable, false otherwise
	 * @return this submitter
	 */
	public TSDBSubmitter setTracingDisabled(final boolean disableTraces) {
		this.disableTraces = disableTraces;
		return this;
	}
	


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TSDBSubmitter [");
		if (host != null) {
			builder.append("\n\thost=");
			builder.append(host);
		}
		builder.append("\n\tport=");
		builder.append(port);
		builder.append("\n\tlogTraces=");
		builder.append(logTraces);
		builder.append("\n\ttracingDisabled=");
		builder.append(disableTraces);		
		builder.append("\n\tkeepAlive=");
		builder.append(keepAlive);
		builder.append("\n\treuseAddress=");
		builder.append(reuseAddress);
		builder.append("\n\tlinger=");
		builder.append(linger);
		builder.append("\n\ttcpNoDelay=");
		builder.append(tcpNoDelay);
		builder.append("\n\ttraceInSeconds=");
		builder.append(traceInSeconds);
		if (rootTags != null) {
			builder.append("\n\trootTags=");
			builder.append(rootTags);
		}
		builder.append("\n\treceiveBufferSize=");
		builder.append(receiveBufferSize);
		builder.append("\n\tsendBufferSize=");
		builder.append(sendBufferSize);
		builder.append("\n\tlingerTime=");
		builder.append(lingerTime);
		builder.append("\n\ttimeout=");
		builder.append(timeout);
		if (dataBuffer != null) {
			builder.append("\n\tdataBufferCapacity=");
			builder.append(dataBuffer.capacity());
		}
		builder.append("]");
		return builder.toString();
	}




	

}
