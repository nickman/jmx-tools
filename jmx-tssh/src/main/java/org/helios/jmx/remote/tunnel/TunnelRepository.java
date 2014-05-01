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
	
	/** A map of open tunnel connections keyed by <b><code>&lt;address&gt;:&lt;remote-sshPort&gt;</code></b> */
	private final Map<String, ConnectionWrapper> connectionsByAddress = new ConcurrentHashMap<String, ConnectionWrapper>();
	/** A map of open tunnels keyed by <b><code>&lt;address&gt;:&lt;local-sshPort&gt;:&lt;remote-sshPort&gt;</code></b> */
	private final Map<String, LocalPortForwarderWrapper> tunnelsByAddressWithLocal = new ConcurrentHashMap<String, LocalPortForwarderWrapper>();
	/** A map of open tunnels keyed by <b><code>&lt;address&gt;:&lt;remote-sshPort&gt;</code></b> */
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
	 * @param sshHost The sshHost to bridge through, defaults to the target jmxHost
	 * @param sshPort The sshPort to bridge through, defaults to 22
	 * @param jmxHost The JMX connector endpoint host
	 * @param jmxPort The JMX connector endpoint port
	 * @param localPort The local port. If zero is passed, a port will be auto assigned
	 * @return a tunnel handle used to close the tunnel and that provides the local port assignment
	 */
	protected TunnelHandle tunnel(String sshHost, int sshPort, String jmxHost, int jmxPort, int localPort) {
		if(jmxHost==null || jmxHost.trim().isEmpty()) throw new IllegalArgumentException("Target JMX Host was null or empty");
		if(jmxPort < 1 || jmxPort > 65535) throw new IllegalArgumentException("Target jmxPort was out of range [" + jmxPort + "]");		
		if(sshHost==null || sshHost.trim().isEmpty()) sshHost = jmxHost;
		if(sshPort < 0 || sshPort > 65535) sshPort = 22;		
		// Get a connection to the bridge sshHost
		
		// Get a tunnel to the target sshHost
		
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
	 * @param sshHost The remote sshHost name or address
	 * @param sshPort The remote sshPort
	 * @return true if the connection exists
	 */
	public boolean hasConnectionFor(String host, int port) {
		String[] aliases = InetAddressCache.getInstance().getAliases(host);
		String key = String.format("%s:%s", aliases[0], port);
		return connectionsByAddress.containsKey(key); 
	}
	
	/**
	 * Determines if an open connection exists for the passed tunnel connector
	 * @param tunnelConnector The tunnel connector specifying the requested connection
	 * @return true if a connection as specified exists, false otherwise
	 */
	public boolean hasConnectionFor(SSHTunnelConnector tunnelConnector) {
		return hasConnectionFor(tunnelConnector.getSSHHost(), tunnelConnector.getSSHPort());
	}
	
	/**
	 * Creates a connection for the passed tunnel connector if the specified connection does not exist
	 * @param tunnelConnector the connector specifying the target sshHost and sshPort to connect to
	 */
	public void connect(SSHTunnelConnector tunnelConnector) {
		if(!hasConnectionFor(tunnelConnector)) {
			synchronized(connectionsByAddress) {
				if(!hasConnectionFor(tunnelConnector)) {
					Connection connection = tunnelConnector.connectAndAuthenticate();
					ConnectionWrapper cw = new ConnectionWrapper(connection, true);
					registerConnection(cw);
				}
			}
		}
	}
	
	
	
	/**
	 * Determines if the repo has an open tunnel to this remote socket and the local sshPort
	 * @param sshHost The remote sshHost name or address
	 * @param sshPort The remote sshPort
	 * @param localPort The local sshPort
	 * @return true if the tunnel exists
	 */
	public boolean hasTunnelFor(String host, int port, int localPort) {
		try {
			String name = InetAddress.getByName(host).getHostAddress();
			String key = String.format("%s:%s:%s", name, localPort, port);
			return tunnelsByAddressWithLocal.containsKey(key);
		} catch (UnknownHostException e) {
			return false;
		}		
	}
	
	public boolean hasTunnelFor(SSHTunnelConnector tunnelConnector) {
		if(tunnelConnector.getLocalPort()==0) {
			
		} else {
			return hasTunnelFor(tunnelConnector.getSSHHost(), tunnelConnector.getSSHPort(), tunnelConnector.getLocalPort()); 
		}
		
	}
	
	
	
	
	/**
	 * Generates a sshHost name key for a connection
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
	 * Generates a sshHost name key for a tunnel
	 * @param conn The connection that established the tunnel
	 * @param localPort The local sshPort of the tunnel
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
	 * @param localPort The local sshPort of the tunnel
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
