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

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import ch.ethz.ssh2.KnownHosts;

/**
 * <p>Title: SSHConnectionConfiguration</p>
 * <p>Description: A configuration and builder for holding the requirements to establish an SSH connection</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.opentsdb.SSHConnectionConfiguration</code></p>
 */

public class SSHConnectionConfiguration {
	
	/** The const name for the SSH target host */
	public static final String SSH_HOST = "ssh.host";
	/** The const name for the SSH target port */
	public static final String SSH_PORT = "ssh.port";
	/** The const name for the SSH user name */
	public static final String SSH_USER = "ssh.username";	
	/** The const name for the SSH user password */
	public static final String SSH_PW = "ssh.password";
	/** The const name for the SSH connection private key characters */
	public static final String SSH_PK = "ssh.privatekey";	
	/** The const name for the SSH connection private key file name */
	public static final String SSH_PKF = "ssh.privatekeyfile";
	/** The const name for the SSH connection private key passphrase */
	public static final String SSH_PP = "ssh.passphrase";
	/** The const name for the SSH known hosts file */
	public static final String SSH_KH = "ssh.knownhosts";
	/** The const name for the boolean indicating if a real host verifier should be used */
	public static final String SSH_VH = "ssh.verifyhosts";
	/** The const name for the SSH connection timeout */
	public static final String SSH_CT = "ssh.connectiontimeout";
	/** The const name for the SSH key exchange timeout */
	public static final String SSH_KT = "ssh.kextimeout";
	
	/** The default connection timeout in ms. */
	public static final int DEFAULT_CONNECT_TIMEOUT = 5000;
	/** The default key exchange timeout in ms. */
	public static final int DEFAULT_KEX_TIMEOUT = 5000;
	
	/** A cache of SSHConnectionConfiguration keyed by <b><code>IP ADDRESS:PORT</code></b> */
	private static final ConcurrentHashMap<String, SSHConnectionConfiguration> configs = new ConcurrentHashMap<String, SSHConnectionConfiguration>(); 
	
	/** The tunnel manager instance */
	private static final TunnelManager tm = TunnelManager.getInstance();

	/** The required host name */
	final String host;
	/** The required user name */
	final String userName;		
	/** The SSH port, defaulting to 22 */
	final int port;
	/** The user password */
	final String userPassword;		
	/** The private key characters */
	char[] privateKey;
	/** The private key file */
	final File privateKeyFile;		
	/** The private key decryption passphrase */
	final String passPhrase;
	/** The known hosts file to verify hosts with */
	final File knownHostsFile;
	/** Indicates if host keys should be verified */
	final boolean verifyHosts;
	/** The connect timeout in ms. */
	final int connectTimeout;
	/** The key exchange timeout in ms. */
	final int kexTimeout;
	
	/** The known hosts instance */
	final KnownHosts knownHosts;
	/**
	 * Returns the SSHConnectionConfiguration for the passed SSH target ip address and port
	 * @param ipAddress The ip address of the target SSH server
	 * @param port The port of the target SSH server
	 * @return The matching SSHConnectionConfiguration or null if one was not found
	 */
	public static SSHConnectionConfiguration getInstance(final String ipAddress, final int port) {
		return configs.get(ipAddress + ":" + port);
	}
	
	
	
	/**
	 * Returns the SSHConnectionConfiguration for the passed properties
	 * @param sshConfig Properties containing the ssh config properties
	 * @return the SSHConnectionConfiguration
	 */
	public static SSHConnectionConfiguration getInstance(final Properties sshConfig) {
		final String key = tm.resolveToAddress(trim(sshConfig.getProperty(SSH_HOST, "localhost"))) 
				+ ":" + toint(sshConfig.getProperty(SSH_PORT), 22);
		SSHConnectionConfiguration sshc = configs.get(key);
		if(sshc==null) {
			synchronized(sshc) {
				sshc = configs.get(key);
				if(sshc==null) {
					sshc = new SSHConnectionConfiguration(sshConfig);
				}
			}
		}
		return sshc;				
	}
	
	
	/**
	 * Creates a new SSHConnectionConfiguration
	 * @param sshConfig A configuration properties of SSH config items keyed by the <b><code>SSH_*</code></b> const values
	 */
	private SSHConnectionConfiguration(final Properties sshConfig) {
		host = tm.resolveToAddress(trim(sshConfig.getProperty(SSH_HOST, "localhost")));
		port = toint(sshConfig.getProperty(SSH_PORT), 22);
		userName = trim(sshConfig.getProperty(SSH_USER, System.getProperty("user.name")));		
		userPassword = trim(sshConfig.getProperty(SSH_PW));		
		privateKey = chars(sshConfig.getProperty(SSH_PK));	
		privateKeyFile = file(sshConfig.getProperty(SSH_PKF));
		passPhrase = trim(sshConfig.getProperty(SSH_PP));		
		knownHostsFile = file(sshConfig.getProperty(SSH_KH));	
		verifyHosts = bool(sshConfig.getProperty(SSH_VH));
		connectTimeout = toint(sshConfig.getProperty(SSH_CT), DEFAULT_CONNECT_TIMEOUT);
		kexTimeout = toint(sshConfig.getProperty(SSH_KT), DEFAULT_KEX_TIMEOUT);
		knownHosts = knowHosts();
	}
	
	
	/**
	 * Creates a new SSHConnectionConfiguration from the passed builder
	 * @param builder the builder to build this config from
	 */
	private SSHConnectionConfiguration(final Builder builder) {
		host = builder.host;
		userName = builder.userName;		
		port = builder.port;
		userPassword = builder.userPassword;		
		privateKey = builder.privateKey;
		privateKeyFile = builder.privateKeyFile;		
		passPhrase = builder.passPhrase;
		knownHostsFile = builder.knownHostsFile;
		verifyHosts = builder.verifyHosts;
		connectTimeout = builder.connectTimeout;
		kexTimeout = builder.kexTimeout;
		knownHosts = knowHosts();
	}
	
	/**
	 * Creates the known hosts file
	 * @return the known hosts file or null if a file was not configured
	 */
	KnownHosts knowHosts() {
		if(knownHostsFile!=null && knownHostsFile.canRead()) {
			try {
				return new KnownHosts(knownHostsFile);
			} catch (IOException e) {
				throw new RuntimeException("Failed to read KnownHosts file [" + knownHostsFile + "]", e);
			}
		}
		return null;
	}
	
	/**
	 * Returns the SSH connection cache key
	 * @return the SSH connection cache key
	 */
	public String key() {
		return tm.hostNameToAddress(host) + ":" + port;
	}

	
	/**
	 * Creates a new builder
	 * @param host The required host or ip address
	 * @param userName The required user name
	 * @return the new builder
	 */
	public static Builder newBuilder(final String host, final String userName) {
		return new Builder(host, userName);
	}
	
	
	/**
	 * <p>Title: Builder</p>
	 * <p>Description: Fluent style builder for SSHConnection specs.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.opentsdb.SSHConnectionConfiguration.Builder</code></p>
	 */
	public static class Builder {
		/** The required host name */
		final String host;
		/** The required user name */
		final String userName;		
		/** The SSH port, defaulting to 22 */
		int port = 22;
		/** The user password */
		String userPassword = null;		
		/** The private key characters */
		char[] privateKey = null;
		/** The private key file */
		File privateKeyFile = null;		
		/** The private key decryption passphrase */
		String passPhrase = null;
		/** The known hosts file to verify hosts with */
		File knownHostsFile = null;
		/** Indicates if host keys should be verified */
		boolean verifyHosts = true;
		/** The connect timeout in ms. */
		int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
		/** The key exchange timeout in ms. */
		int kexTimeout = DEFAULT_KEX_TIMEOUT;
		
		
		
		/**
		 * Creates a new Builder
		 * @param host The required host or ip address
		 * @param userName The required user name
		 */
		Builder(final String host, final String userName) {
			this.host = tm.resolveToAddress(host);
			this.userName = userName;
		}
		
		/**
		 * Returns the SSH connection cache key
		 * @return the SSH connection cache key
		 */
		String key() {
			return tm.hostNameToAddress(host) + ":" + port;
		}
		
		/**
		 * Builds and returns a new SSHConnectionConfiguration
		 * @return the new SSHConnectionConfiguration
		 */
		public SSHConnectionConfiguration build() {
			final String key = key();
			SSHConnectionConfiguration sshc = configs.get(key);
			if(sshc==null) {
				synchronized(sshc) {
					sshc = configs.get(key);
					if(sshc==null) {
						sshc = new SSHConnectionConfiguration(this);
					}
				}
			}
			return sshc;
		}
		
		/**
		 * Sets the connection timeout in ms,
		 * @param timeout the connection timeout in ms,
		 * @return this builder
		 */
		public final Builder setConnectTimeout(final int timeout) {
			connectTimeout = timeout;
			return this;
		}
		
		/**
		 * Sets the key exchange timeout in ms,
		 * @param timeout the key exchange timeout in ms,
		 * @return this builder
		 */
		public final Builder setKeyExchangeTimeout(final int timeout) {
			kexTimeout = timeout;
			return this;
		}
		
		
		/**
		 * Sets the SSH port
		 * @param port The SSH port to connect to
		 * @return this builder
		 */
		public final Builder setPort(final int port) {
			this.port = port;
			return this;
		}


		/**
		 * Sets the user password
		 * @param userPassword the userPassword to set
		 * @return this builder
		 */
		public final Builder setUserPassword(final String userPassword) {
			this.userPassword = userPassword;
			return this;
		}

		/**
		 * Sets the private key characters
		 * @param privateKey the privateKey to set
		 * @return this builder
		 */
		public final Builder setPrivateKey(final char[] privateKey) {
			this.privateKey = privateKey;
			return this;
		}
		
		/**
		 * Sets the private key file name
		 * @param privateKey the privateKey file name to set
		 * @return this builder
		 */
		public final Builder setPrivateKey(final String privateKey) {
			return setPrivateKey(new File(privateKey));
		}
		
		/**
		 * Sets the private key file
		 * @param privateKey the privateKey file to set
		 * @return this builder
		 */
		public final Builder setPrivateKey(final File privateKey) {
			this.privateKeyFile = privateKey;
			return this;
		}
		

		/**
		 * Sets the passphrase used to decrypt the private key
		 * @param passPhrase the passPhrase to set
		 * @return this builder
		 */
		public final Builder setPassPhrase(final String passPhrase) {
			this.passPhrase = passPhrase;
			return this;
		}

		/**
		 * Sets the KnownHosts file to use to verify host keys
		 * @param knownHostsFile the knownHostsFile to set
		 * @return this builder
		 */
		public final Builder setKnownHostsFile(final File knownHostsFile) {
			this.knownHostsFile = knownHostsFile;
			return this;
		}
		
		/**
		 * Sets the KnownHosts file name to use to verify host keys
		 * @param knownHostsFile the knownHostsFile to set
		 * @return this builder
		 */
		public final Builder setKnownHostsFile(final String knownHostsFile) {
			return setKnownHostsFile(new File(knownHostsFile));
		}
		

		/**
		 * Specifies if host keys should be verified
		 * @param verifyHosts true to verify hosts, false to accept any key presented by a target host
		 * @return this builder
		 */
		public final Builder setVerifyHosts(final boolean verifyHosts) {
			this.verifyHosts = verifyHosts;
			return this;
		}
	}

	
	private static int toint(final String cfg, final int defaultValue) {
		if(cfg==null || cfg.trim().isEmpty()) return defaultValue;
		try {
			return Integer.parseInt(cfg.trim());
		} catch (Exception x) {
			return defaultValue;
		}		
	}
	
	
	private static String trim(final String cfg) {
		if(cfg==null || cfg.trim().isEmpty()) return null;
		return cfg.trim();		
	}
	
	private static char[] chars(final String cfg) {
		if(cfg==null || cfg.trim().isEmpty()) return null;
		return cfg.trim().toCharArray();		
	}
	
	
	private static File file(final String cfg) {
		if(cfg==null || cfg.trim().isEmpty()) return null;
		return new File(cfg.trim());
	}
	
	private static boolean bool(final String cfg) {
		if(cfg==null || cfg.trim().isEmpty()) return true;
		return !cfg.trim().toLowerCase().equals("false");
	}



	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + port;
		result = prime * result
				+ ((userName == null) ? 0 : userName.hashCode());
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
		SSHConnectionConfiguration other = (SSHConnectionConfiguration) obj;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (port != other.port)
			return false;
		if (userName == null) {
			if (other.userName != null)
				return false;
		} else if (!userName.equals(other.userName))
			return false;
		return true;
	}



	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder2 = new StringBuilder();
		builder2.append("SSHConnectionConfiguration [host=");
		builder2.append(host);
		builder2.append(", port=");
		builder2.append(port);
		builder2.append(", userName=");
		builder2.append(userName);
		builder2.append(", privateKeyFile=");
		builder2.append(privateKeyFile);
		builder2.append(", knownHostsFile=");
		builder2.append(knownHostsFile);
		builder2.append(", verifyHosts=");
		builder2.append(verifyHosts);
		builder2.append(", connectTimeout=");
		builder2.append(connectTimeout);
		builder2.append(", kexTimeout=");
		builder2.append(kexTimeout);		
		builder2.append("]");
		return builder2.toString();
	}

	
}
