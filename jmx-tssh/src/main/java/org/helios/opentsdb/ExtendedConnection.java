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

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.ServerHostKeyVerifier;

/**
 * <p>Title: ExtendedConnection</p>
 * <p>Description: An extension of {@link Connection} with some additional meta-data and event handling.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.opentsdb.ExtendedConnection</code></p>
 */

public class ExtendedConnection extends Connection implements ServerHostKeyVerifier {
	/** The SSH connection configuration */
	final SSHConnectionConfiguration sshConfig;
	
	/**
	 * Creates a new ExtendedConnection
	 * @param sshConfig The SSH connection configuration
	 */
	public ExtendedConnection(final SSHConnectionConfiguration sshConfig) {
		super(sshConfig.host, sshConfig.port);
		this.sshConfig = sshConfig;
	}
	
	boolean doConnect() {
		try {
			connect(this, sshConfig.connectTimeout, sshConfig.kexTimeout);
			authenticateWithNone(sshConfig.userName);
			if(isAuthMethodAvailable(sshConfig.userName, "publickey")) {
				//authenticateWithAgent(user, proxy)
				
			}
			return true;
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 * @see ch.ethz.ssh2.ServerHostKeyVerifier#verifyServerHostKey(java.lang.String, int, java.lang.String, byte[])
	 */
	@Override
	public boolean verifyServerHostKey(final String hostname, final int port, final String serverHostKeyAlgorithm, final byte[] serverHostKey) throws Exception {
		if(!sshConfig.verifyHosts) return true;
		if(sshConfig.knownHosts!=null) {
			sshConfig.knownHosts.verifyHostkey(hostname, serverHostKeyAlgorithm, serverHostKey);
		}
		return false;
	}


}
