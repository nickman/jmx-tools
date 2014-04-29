/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
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
package org.helios.jmx.remote.tunnel;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.remote.JMXServiceURL;

import org.helios.jmx.remote.InetAddressCache;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.ConnectionMonitor;

/**
 * <p>Title: TunnelRepository</p>
 * <p>Description: A repository of open SSH tunneling connections</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.remote.tunnel.TunnelRepository</code></p>
 */

public class TunnelRepository {
	/** The singleton instance */
	private static volatile TunnelRepository instance = null;
	/** The singleton ctor lock */
	private static final Object lock = new Object();
	
	/** A map of open tunnel connections keyed by <b><code>&lt;address&gt;:&lt;remote-port&gt;</code></b> */
	private final Map<String, ConnectionWrapper> connectionsByAddress = new ConcurrentHashMap<String, ConnectionWrapper>();
	/** A map of open tunnels keyed by <b><code>&lt;address&gt;:&lt;remote-port&gt;</code></b> */
	private final Map<String, LocalPortForwarderWrapper> tunnelsByAddress = new ConcurrentHashMap<String, LocalPortForwarderWrapper>();
	
	
	/**
	 * Acquires the TunnelRepository singleton instance
	 * @return the TunnelRepository singleton instance
	 */
	public static TunnelRepository getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new TunnelRepository();
				}
			}
		}
		return instance;
	}
	
	
	
	/**
	 * Creates a new tunnel
	 * @param bridgeHost The host to bridge through, defaults to the target host
	 * @param bridgePort The port to bridge through, defaults to 22 or the target port if the bridge host is null
	 * @param targetHost The host to tunnel to
	 * @param targetPort The port to tunnel to
	 * @param localPort the local port, which can be 0 for a dynamically assigned port
	 * @return the tunnel handle which provides the local port the tunnel is accessible through
	 */
	
	protected TunnelHandle tunnel(String bridgeHost, int bridgePort, String targetHost, int targetPort, int localPort) {
		if(targetHost==null || targetHost.trim().isEmpty()) throw new IllegalArgumentException("Target host was null or empty");
		if(targetPort < 1 || targetPort > 65535) throw new IllegalArgumentException("Target port was out of range [" + targetPort + "]");		
		if(bridgePort < 1) {
			if(bridgeHost==null || bridgeHost.trim().isEmpty()) {
				bridgePort = targetPort;
			} else {
				bridgePort = 22;
			}
		}
		if(bridgeHost==null || bridgeHost.trim().isEmpty()) bridgeHost = targetHost;
		// Get a connection to the bridge host
		
		// Get a tunnel to the target host
		
		return null;
	}
	
	public Map tunnel(JMXServiceURL jmxServiceURL, Map env) {
		if(env==null) env = new HashMap();
		
		return env;
	}
	
	
	
	
	/**
	 * Registers a newly established connection
	 * @param conn the connection to register
	 */
	protected void registerConnection(ConnectionWrapper conn) {
		connectionsByAddress.put(addressKey(conn), conn);
		conn.addConnectionMonitor(new ConnectionMonitor() {
			@Override
			public void connectionLost(Throwable reason) {
				// TODO Auto-generated method stub
				
			}
		});
	}
	
	/**
	 * Determines if the repo has an open connection to this remote socket
	 * @param host The remote host name or address
	 * @param port The remote port
	 * @return true if the connection exists
	 */
	public boolean hasConnectionFor(String host, int port) {
		String[] aliases = InetAddressCache.getInstance().getAliases(host);
		String key = String.format("%s:%s", aliases[0], port);
		return connectionsByAddress.containsKey(key); 
	}
	
	/**
	 * Determines if the repo has an open tunnel to this remote socket and the local port
	 * @param host The remote host name or address
	 * @param port The remote port
	 * @param localPort The local port
	 * @return true if the tunnel exists
	 */
	public boolean hasTunnelFor(String host, int port, int localPort) {
		try {
			String name = InetAddress.getByName(host).getHostAddress();
			String key = String.format("%s:%s:%s", name, localPort, port);
			return tunnelsByAddress.containsKey(key);
		} catch (UnknownHostException e) {
			return false;
		}		
	}
	
	
	/**
	 * Generates a host name key for a connection
	 * @param conn The connection to get a key for
	 * @return the key
	 */
	public static String nameKey(Connection conn) {
		try {
			String name = InetAddress.getByName(conn.getHostname()).getHostName();
			return String.format("%s:%s", name, conn.getPort());
		} catch (UnknownHostException e) {
			return null;
		}
	}
	
	/**
	 * Generates an address name key for a connection
	 * @param conn The connection to get a key for
	 * @return the key
	 */
	public static String addressKey(ConnectionWrapper conn) {
		try {
			String address = InetAddress.getByName(conn.getHostname()).getHostAddress();
			return String.format("%s:%s", address, conn.getPort());
		} catch (UnknownHostException e) {
			return null;
		}
	}
	
	/**
	 * Generates a host name key for a tunnel
	 * @param conn The connection that established the tunnel
	 * @param localPort The local port of the tunnel
	 * @return the key
	 */
	public static String nameKey(Connection conn, int localPort) {
		try {
			String name = InetAddress.getByName(conn.getHostname()).getHostName();
			return String.format("%s:%s:%s", name, localPort, conn.getPort());
		} catch (UnknownHostException e) {
			return null;
		}
	}
	
	/**
	 * Generates an address name key for a tunnel
	 * @param conn The connection to get a key for
	 * @param localPort The local port of the tunnel
	 * @return the key 
	 */
	public static String addressKey(Connection conn, int localPort) {
		try {
			String address = InetAddress.getByName(conn.getHostname()).getHostAddress();
			return String.format("%s:%s:%s", address, localPort, conn.getPort());
		} catch (UnknownHostException e) {
			return null;
		}
	}
	
	
	private TunnelRepository() {
		
	}
}
