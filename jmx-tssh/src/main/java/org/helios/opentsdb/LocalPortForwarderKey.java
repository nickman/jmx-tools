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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Title: LocalPortForwarderKey</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.opentsdb.LocalPortForwarderKey</code></p>
 */

public class LocalPortForwarderKey  {

	/** A cache of LocalPortForwarderKeys  */
	private static final Map<String, LocalPortForwarderKey> KEYS = new ConcurrentHashMap<String, LocalPortForwarderKey>();
	
	/** The default local port local forwards will listen on, which is zero, so it will be an ephemeral port */
	public static final int DEFAULT_PORT = 0;
	
	
	/** The requested local port */
	public final int localPort;
	/** The remote host to tunnel to */
	public final String remoteHost; 
	/** The remote port to tunnel to */
	public final int remotePort;
	
	/**
	 * Builds a cache key
	 * @param localPort The requested local port
	 * @param remoteHost The remote host to tunnel to 
	 * @param remotePort The remote port to tunnel to
	 * @return the key
	 */
	public static String key(final int localPort, final String remoteHost, final int remotePort) {
		return String.format("%s:%s:%s", localPort, remoteHost, remotePort);
	}
	
	/**
	 * Creates a new LocalPortForwarderKey
	 * @param localPort The requested local port
	 * @param remoteHost The remote host to tunnel to 
	 * @param remotePort The remote port to tunnel to
	 * @return the created or cached LocalPortForwarderKey 
	 */
	public static LocalPortForwarderKey getInstance(final int localPort, final String remoteHost, final int remotePort) {
		final String key = key(localPort, remoteHost, remotePort);
		LocalPortForwarderKey pf = KEYS.get(key);
		if(pf==null) {
			synchronized(KEYS) {
				pf = KEYS.get(key);
				if(pf==null) {
					pf = new LocalPortForwarderKey(localPort, remoteHost, remotePort);
					KEYS.put(key, pf);
				}
			}
		}
		return pf;
	}
	
	/**
	 * Creates a new LocalPortForwarderKey with an ephemeral local port
	 * @param remoteHost The remote host to tunnel to 
	 * @param remotePort The remote port to tunnel to
	 * @return the created or cached LocalPortForwarderKey 
	 */
	public static LocalPortForwarderKey getInstance(final String remoteHost, final int remotePort) {
		return getInstance(DEFAULT_PORT, remoteHost, remotePort);
	}
	
	
	/**
	 * Creates a new LocalPortForwarderKey
	 * @param localPort The requested local port
	 * @param remoteHost The remote host to tunnel to 
	 * @param remotePort The remote port to tunnel to
	 */
	private LocalPortForwarderKey(final int localPort, final String remoteHost, final int remotePort) {
		if(localPort<0) throw new IllegalArgumentException("Invalid local port [" + localPort + "]");
		if(remotePort<1) throw new IllegalArgumentException("Invalid remote port [" + remotePort + "]");
		if(remoteHost==null || remoteHost.trim().isEmpty()) throw new IllegalArgumentException("Remote host was null or empty"); 
		this.localPort = localPort;
		this.remoteHost = TunnelManager.getInstance().hostNameToAddress(remoteHost);
		this.remotePort = remotePort; 
	}
	
	/**
	 * Creates a new LocalPortForwarderKey with an ephemeral local port
	 * @param remoteHost The remote host to tunnel to 
	 * @param remotePort The remote port to tunnel to
	 */
	private LocalPortForwarderKey(final String remoteHost, final int remotePort) {
		this(DEFAULT_PORT, remoteHost, remotePort);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return key(localPort, remoteHost, remotePort);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		if(localPort!=0) {
			result = prime * result + localPort;
		}
		result = prime * result
				+ ((remoteHost == null) ? 0 : remoteHost.hashCode());
		result = prime * result + remotePort;
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LocalPortForwarderKey other = (LocalPortForwarderKey) obj;
		if (localPort != other.localPort && other.localPort != 0 && localPort != 0)
			return false;
		if (remoteHost == null) {
			if (other.remoteHost != null)
				return false;
		} else if (!remoteHost.equals(other.remoteHost))
			return false;
		if (remotePort != other.remotePort)
			return false;
		return true;
	}
	
	
	

}
