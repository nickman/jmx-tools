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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * <p>Title: TSDBSubmitter</p>
 * <p>Description: A Telnet TCP client for buffering and sending metrics to OpenTSDB</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.opentsdb.TSDBSubmitter</code></p>
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
	/** The gzip output stream */
	protected GZIPOutputStream gzip = null;
	/** The gzip input stream */
	protected GZIPInputStream gzipIn = null;
	
	/** The default socket send buffer size in bytes */
	public static final int DEFAULT_SEND_BUFFER_SIZE;
	/** The default socket receive buffer size in bytes */
	public static final int DEFAULT_RECEIVE_BUFFER_SIZE;
	
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
		TSDBSubmitter submitter = new TSDBSubmitter("opentsdb", 8080);
		submitter.setTimeout(2000).connect();
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
			
			//gzipIn = new GZIPInputStream(is, receiveBufferSize*2);			
			log("Version: %s", getVersion());
			gzip = new GZIPOutputStream(os, sendBufferSize*2);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to connect to [" + host + ":" + port + "]", ex);
		}
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
		}
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
