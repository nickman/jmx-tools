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
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.management.ObjectName;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
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
//		TSDBSubmitter submitter = new TSDBSubmitter("opentsdb", 8080);
		TSDBSubmitter submitter = new TSDBSubmitter("localhost");
		submitter.setTimeout(2000).connect();
		while(true) {
			final long start = System.currentTimeMillis();
			for(final MemoryPoolMXBean pool: ManagementFactory.getMemoryPoolMXBeans()) {
				final MemoryUsage mu = pool.getUsage();			
				final String poolName = pool.getName();
				submitter.trace("used", mu.getUsed(), "type", "MemoryPool", "name", poolName);
				submitter.trace("max", mu.getMax(), "type", "MemoryPool", "name", poolName);
				submitter.trace("committed", mu.getCommitted(), "type", "MemoryPool", "name", poolName);
			}
			for(final GarbageCollectorMXBean gc: ManagementFactory.getGarbageCollectorMXBeans()) {
				final ObjectName on = gc.getObjectName();
				final String gcName = gc.getName();
				final Long count = submitter.longDelta(gc.getCollectionCount(), on.toString(), gcName, "count");
				final Long time = submitter.longDelta(gc.getCollectionTime(), on.toString(), gcName, "time");
				if(count!=null) {
					submitter.trace("collectioncount", count, "type", "GarbageCollector", "name", gcName);
				}
				if(time!=null) {
					submitter.trace("collectiontime", time, "type", "GarbageCollector", "name", gcName);
				}
			}
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
	 * @throws RuntimeException if connection fails
	 */
	public void connect() {
		try {
			if(socket.isConnected()) return;
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
	 * Traces a double metric
	 * @param metric The metric name
	 * @param value The value
	 * @param tags The metric tags
	 */
	public void trace(final String metric, final double value, final Map<String, String> tags) {
		// put $metric $now $value host=$HOST "
		StringBuilder b = getSB();
		b.append("put ").append(clean(metric)).append(" ").append(System.currentTimeMillis()/1000).append(" ").append(value).append(" ");
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
		// put $metric $now $value host=$HOST "
		StringBuilder b = getSB();
		b.append("put ").append(clean(metric)).append(" ").append(System.currentTimeMillis()/1000).append(" ").append(value).append(" ");
		for(Map.Entry<String, String> entry: tags.entrySet()) {
			b.append(clean(entry.getKey())).append("=").append(clean(entry.getValue())).append(" ");
		}
		final String s = b.deleteCharAt(b.length()-1).append("\n").toString();
//		log("Traced: [%s]", s);
		final byte[] trace = s.getBytes(CHARSET);
		synchronized(dataBuffer) {
			dataBuffer.writeBytes(trace);
		}
	}
	
	private static String clean(final String s) {
		return s.replace(" ", "");
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

}
