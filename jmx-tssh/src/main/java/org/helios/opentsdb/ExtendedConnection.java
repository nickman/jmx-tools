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
package org.helios.opentsdb;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import org.cliffc.high_scale_lib.NonBlockingHashMap;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.ConnectionInfo;
import ch.ethz.ssh2.ConnectionMonitor;
import ch.ethz.ssh2.ServerHostKeyVerifier;

/**
 * <p>Title: ExtendedConnection</p>
 * <p>Description: An extension of {@link Connection} with some additional meta-data and event handling.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.opentsdb.ExtendedConnection</code></p>
 */

public class ExtendedConnection extends Connection implements ConnectionMonitor {
	/** The SSH connection configuration */
	final SSHConnectionConfiguration sshConfig;
	/** The connection info for this connection */
	ConnectionInfo connInfo = null;
	/** Indicates if this connection is connected */
	final AtomicBoolean connected = new AtomicBoolean(false);
	/** Indicates if this connection is authenticated */
	final AtomicBoolean authenticated = new AtomicBoolean(false);
	
	/** This connection's local port forwarders */
	final NonBlockingHashMap<LocalPortForwarderKey, WrappedLocalPortForwarder> localPortForwarders = new NonBlockingHashMap<LocalPortForwarderKey, WrappedLocalPortForwarder>(); 
	
	 
	
	/**
	 * Creates a new ExtendedConnection
	 * @param sshConfig The SSH connection configuration
	 */
	public ExtendedConnection(final SSHConnectionConfiguration sshConfig) {
		super(sshConfig.host, sshConfig.port);
		this.sshConfig = sshConfig;
		try {
			super.setTCPNoDelay(sshConfig.noDelay);
		} catch (Exception ex) {}
	}
	
	/**
	 * Connects a new port forwarder
	 * @param localIface The local iface to bind to
	 * @param localPort The local port to bind to
	 * @param remoteHost The remote host to connect to
	 * @param remotePort The remote port to connect to
	 * @return the port forward
	 */
	public WrappedLocalPortForwarder localPortForward(final String localIface, final int localPort, final String remoteHost, final int remotePort) {
		final TunnelManager tm = TunnelManager.getInstance();
		final String _remoteHost = tm.hostNameToAddress(remoteHost);
		final LocalPortForwarderKey pfKey = LocalPortForwarderKey.getInstance(localPort, _remoteHost, remotePort);
		WrappedLocalPortForwarder portForwarder = tm.localPortForwarders.get(pfKey);
		if(portForwarder==null) {
			synchronized(tm.localPortForwarders) {
				portForwarder = tm.localPortForwarders.get(pfKey);
				if(portForwarder==null) {
					final InetSocketAddress socketAddress = new InetSocketAddress(localIface, localPort);					
					try {
						if(!isConnected()) {
							fullAuth();
						}
						portForwarder = new WrappedLocalPortForwarder(createLocalPortForwarder(socketAddress, _remoteHost, remotePort), this);
						tm.localPortForwarders.put(pfKey, portForwarder);
						localPortForwarders.put(pfKey, portForwarder);
					} catch (Exception e) {
						throw new RuntimeException("Failed to create PortForwarder to [" + pfKey + "]", e);
					}
				}
			}
		}
		return portForwarder;
	}
	
	/**
	 * Opens a new session on this connection
	 * @return a new session
	 */
	public WrappedSession createSession() {
		try {
			return new WrappedSession(super.openSession(), this);
		} catch (Exception e) {
			throw new RuntimeException("Failed to open new session", e);
		}
	}
	
	/**
	 * Connects a new port forwarder
	 * @param remoteHost The remote host to connect to
	 * @param remotePort The remote port to connect to
	 * @return the port forward
	 */
	public WrappedLocalPortForwarder localPortForward(final String remoteHost, final int remotePort) {
		return localPortForward("127.0.0.1", 0, remoteHost, remotePort);
	}	
	
	/**
	 * {@inheritDoc}
	 * @see ch.ethz.ssh2.Connection#connect()
	 */
	@Override
	public synchronized ConnectionInfo connect() throws IOException {
		return connect(sshConfig, sshConfig.connectTimeout, sshConfig.kexTimeout);
	}
	
	/**
	 * {@inheritDoc}
	 * @see ch.ethz.ssh2.Connection#connect(ch.ethz.ssh2.ServerHostKeyVerifier)
	 */
	@Override
	public synchronized ConnectionInfo connect(ServerHostKeyVerifier verifier) throws IOException {
		return connect(sshConfig, sshConfig.connectTimeout, sshConfig.kexTimeout);
	}
	
	/**
	 * {@inheritDoc}
	 * @see ch.ethz.ssh2.Connection#connect(ch.ethz.ssh2.ServerHostKeyVerifier, int, int)
	 */
	@Override
	public synchronized ConnectionInfo connect(ServerHostKeyVerifier verifier, int connectTimeout, int kexTimeout) throws IOException {
		if(connected.get() && connInfo!=null) return connInfo;
		connInfo = super.connect(verifier, connectTimeout, kexTimeout);
		addConnectionMonitor(this);
		connected.set(true);
		return connInfo;
	}
	
	/**
	 * Executes a full authentication, connecting if not already connected
	 * @return true if authenticated, false otherwise
	 */
	public boolean fullAuth() {
		if(!connected.get()) {
			try {
				connect();
			} catch (Exception e) {
				throw new RuntimeException("Failed to connect before full auth", e);
			}
		}
		authenticated.set(sshConfig.auth(this));		
		return authenticated.get();
	}
	
	/**
	 * Indicates if this connection is connected
	 * @return true if this connection is connected, false otherwise
	 */
	public boolean isConnected() {
		return connected.get();
	}

	/**
	 * Indicates if this connection is authenticated
	 * @return true if this connection is authenticated, false otherwise
	 */
	public boolean isAuthenticated() {
		return authenticated.get();
	}

	/**
	 * {@inheritDoc}
	 * @see ch.ethz.ssh2.ConnectionMonitor#connectionLost(java.lang.Throwable)
	 */
	@Override
	public void connectionLost(Throwable reason) {
		removeConnectionMonitor(this);
		connected.set(false);
		authenticated.set(false);		
	}


}
