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

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import org.helios.jmx.remote.CloseListener;
import org.jboss.netty.util.internal.ConcurrentHashMap;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.ConnectionMonitor;
import ch.ethz.ssh2.LocalPortForwarder;
import ch.ethz.ssh2.transport.ClientTransportManager;

/**
 * <p>Title: ConnectionWrapper</p>
 * <p>Description: Indexable wrapper for connections and tunnels</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.remote.tunnel.ConnectionWrapper</code></p>
 */

public class ConnectionWrapper implements Closeable, ConnectionMonitor, CloseListener<LocalPortForwarderWrapper> {
	/** The connection to wrap */
	private final Connection connection;
	/** The host name */
	private final String host;
	/** The port */
	private final int port;
	/** The socket */
	private final Socket socket;
	/** Indicates if the tunnel should be closed when the usage drops to zero */
	protected final boolean closeOnZeroUsage;
	
	/** A map of tunnels keyed by keyed by <b><code>&lt;local-port&gt;:&lt;remote-host&gt;:&lt;remote-port&gt;</code> */
	protected final Map<String, LocalPortForwarderWrapper> tunnels = new ConcurrentHashMap<String, LocalPortForwarderWrapper>();
	
	/** A set of close listeners */
	protected final Set<CloseListener<ConnectionWrapper>> closeListeners = new CopyOnWriteArraySet<CloseListener<ConnectionWrapper>>();
	/** The usage count */
	protected final AtomicInteger usage = new AtomicInteger(0);
	
	
	/** The connection's transport manager field name */
	public static final String TRANSPORT_MANAGER_FIELD_NAME = "tm";
	/** The transport manager's socket field name */
	public static final String SOCKET_FIELD_NAME = "sock";
	
	/** The connection's transport manager field  */
	protected static final Field connectionTransportMgr;
	/** The transport manager's socket field */
	protected static final Field sock;
	
	static {
		try {
			connectionTransportMgr = Connection.class.getDeclaredField(TRANSPORT_MANAGER_FIELD_NAME);
			connectionTransportMgr.setAccessible(true);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		try {
			sock = ClientTransportManager.class.getDeclaredField(SOCKET_FIELD_NAME);
			sock.setAccessible(true);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}		
	}
	
	
	/**
	 * Creates a new ConnectionWrapper
	 * @param connection The connection to wrap
	 * @param closeOnZeroUsage  Indicates if the tunnel should be closed when the usage drops to zero 
	 */
	public ConnectionWrapper(Connection connection, boolean closeOnZeroUsage) {
		this.connection = connection;
		host = connection.getHostname();
		port = connection.getPort();
		this.closeOnZeroUsage = closeOnZeroUsage;
		try {
			ClientTransportManager ctm = (ClientTransportManager)connectionTransportMgr.get(connection);
			socket = (Socket)sock.get(ctm);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to get connection socket", ex);
		}
	}
	
	/**
	 * Creates a new tunnel
	 * @param host The host to tunnel to
	 * @param port The port to tunnel to
	 * @return the tunnel handle
	 */
	public TunnelHandle tunnel(String host, int port) {
		return tunnel(host, port, 0);
	}
	
	
	/**
	 * Creates a new tunnel
	 * @param host The host to tunnel to
	 * @param port The port to tunnel to
	 * @param localPort the local port
	 * @return the tunnel handle
	 */
	@SuppressWarnings("resource")
	public TunnelHandle tunnel(String host, int port, int localPort) {
		if(host==null || host.trim().isEmpty()) throw new IllegalArgumentException("Host was null or empty");
		if(port < 1 || port > 65534) throw new IllegalArgumentException("Port number [" + port + "] out of range");
		if(port < 0 || port > 65534) throw new IllegalArgumentException("Local Port number [" + port + "] out of range");
		String address = getAliases(host)[0];
		final String key = String.format("%s:%s", address, port);
		LocalPortForwarderWrapper localPortForwarderWrapper = tunnels.get(key);
		if(localPortForwarderWrapper==null) {
			try {
				LocalPortForwarder localPortForwarder = connection.createLocalPortForwarder(localPort, address, port);
				localPortForwarderWrapper = new LocalPortForwarderWrapper(localPortForwarder, address, port, closeOnZeroUsage);
				localPortForwarderWrapper.addCloseListener(this);
				tunnels.put(key, localPortForwarderWrapper);
			} catch (Exception ex) {
				throw new RuntimeException("Failed to create tunnel [" + String.format("%s:%s:%s", localPort, host, port) + "]", ex);
			}			
		}
		final ConnectionWrapper self = this;
		final LocalPortForwarderWrapper lpfw = localPortForwarderWrapper;
		this.incrUsage();
		lpfw.incrUsage();
		return new TunnelHandle() {
			@Override
			public void close() throws IOException {
				lpfw.decrUsage();
				self.decrUsage();					
			}
			@Override
			public int getLocalPort() {
				return lpfw.getLocalPort();
			}
		};
		
	}
	
	/**
	 * Returns the address and host name for the passed string
	 * @param hostName The host string or address to resolve
	 * @return a string array with the address and host name
	 */
	public static String[] getAliases(String hostName) {
		try {
			InetAddress ia = InetAddress.getByName(hostName);
			return new String[] {ia.getHostAddress(), ia.getHostName()};
		} catch (Exception ex) {
			throw new RuntimeException("Failed to resolve host [" + hostName + "]", ex);
		}
	}
	
	/**
	 * Increments the usage
	 * @return the new usage
	 */
	public int incrUsage() {
		return usage.incrementAndGet();
	}
	
	/**
	 * Decrements the usage
	 * @return the new usage
	 */
	public int decrUsage() {
		int u = usage.decrementAndGet();
		if(u < 1 && closeOnZeroUsage) {
			close();
		}
		return u;
	}
	
	/**
	 * Returns the current usage
	 * @return the current usage
	 */
	public int getUsage() {
		return usage.get();
	}
	
	
	/**
	 * Indicates if the connection is open and authenticated
	 * @return true if the connection is open and authenticated, false otherwise
	 */
	public boolean isOpen() {
		return connection!=null && connection.isAuthenticationComplete();
	}


	/**
	 * {@inheritDoc}
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() {
		if(isOpen()) {
			try {
				connection.close();
			} catch (Exception ex) {/* No Op */}
		}		
	}
	
	/**
	 * Registers a close listener on this connection
	 * @param listener the listener to be notified when this connection closes
	 */
	public void addCloseListener(CloseListener<ConnectionWrapper> listener) {
		if(listener!=null) {
			closeListeners.add(listener);
		}
	}
	
	/**
	 * Removes a close listener from this connection
	 * @param listener the listener to be removed
	 */
	public void removeCloseListener(CloseListener<ConnectionWrapper> listener) {
		if(listener!=null) {
			closeListeners.remove(listener);
		}
	}
	
	/**
	 * Adds a {@link ConnectionMonitor} to the wrapped connection
	 * @param cmon the connection monitor to add
	 */
	public void addConnectionMonitor(ConnectionMonitor cmon) {
		if(cmon!=null) connection.addConnectionMonitor(cmon);
	}
	
    /**
     * Remove a {@link ConnectionMonitor} from the wrapped connection.
     * @param cmon the connection monitor to add
     * @return whether the monitor could be removed
     */
    public boolean removeConnectionMonitor(ConnectionMonitor cmon) {
    	if(cmon!=null) return connection.removeConnectionMonitor(cmon);
    	return false;
    }
	
	/**
	 * Fires the closed callback on all registered listeners
	 */
	protected void fireClosed() {
		for(CloseListener<ConnectionWrapper> listener: closeListeners) {
			listener.onClosed(this);
		}
	}

	@Override
	public void connectionLost(Throwable reason) {
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.remote.CloseListener#onClosed(java.io.Closeable)
	 */
	@Override
	public void onClosed(LocalPortForwarderWrapper closeable) {		
		for(String key: closeable.getKeys()) {
			tunnels.remove(key);
		}
	}

	/**
	 * @return
	 */
	public String getHostname() {
		return connection.getHostname();
	}

	/**
	 * @return
	 */
	public Object getPort() {
		return connection.getPort();
	}
	
	
	
}
