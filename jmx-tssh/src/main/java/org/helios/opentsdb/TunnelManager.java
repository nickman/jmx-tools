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
import java.io.FileReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.cliffc.high_scale_lib.NonBlockingHashMap;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.KnownHosts;
import ch.ethz.ssh2.crypto.PEMDecoder;
import ch.ethz.ssh2.signature.DSAPrivateKey;
import ch.ethz.ssh2.signature.RSAPrivateKey;

/**
 * <p>Title: TunnelManager</p>
 * <p>Description: Creates and manages SSH port tunnels</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.opentsdb.TunnelManager</code></p>
 */

public class TunnelManager {
	/** The singleton instance */
	private static volatile TunnelManager instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** IPV4 Pattern */
	public static final Pattern IP4_ADDRESS_PATTERN = Pattern.compile( 
			"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
			"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
			"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
			"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
	
	/** IPV6 Pattern */
	public static final Pattern IP6_ADDRESS_PATTERN = Pattern.compile( 
			"^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");

	/** The message returned by the PEMDecoder if the pk is encrypted but we supplied no passphrase */
	public static final String NO_PASSPHRASE_MESSAGE = "PEM is encrypted, but no password was specified";	
	/** The default DSA Private Key location and file name */
	public static final String DEFAULT_DSA = String.format("%s%s.ssh%sid_dsa", System.getProperty("user.home"), File.separator, File.separator);
	/** The default RSA Private Key location and file name */
	public static final String DEFAULT_RSA = String.format("%s%s.ssh%sid_rsa", System.getProperty("user.home"), File.separator, File.separator);
	/** The default TSSH property config location and file name */
	public static final String DEFAULT_PROPS = String.format("%s%s.ssh%stunnel.properties", System.getProperty("user.home"), File.separator, File.separator);
	
	
	/** Instance logger */
	private static final Logger log = Logger.getLogger(TunnelManager.class);
	
	/** A map of SSH connections keyed by the <b><code>IP-ADDRESS:PORT</code></b> */
	private final NonBlockingHashMap<String, ExtendedConnection> connections = new NonBlockingHashMap<String, ExtendedConnection>(); 
	/** A map of local port forwarders keyed by the port fowarder key */
	final NonBlockingHashMap<LocalPortForwarderKey, WrappedLocalPortForwarder> localPortForwarders = new NonBlockingHashMap<LocalPortForwarderKey, WrappedLocalPortForwarder>(); 
	
	/** A map of ip addresses keyed by their host names */
	private final NonBlockingHashMap<String, String> hostNameToIpAddress = new NonBlockingHashMap<String, String>();  
	/** A map of host names keyed by their ip addresses */
	private final NonBlockingHashMap<String, String> ipAddressToHostName = new NonBlockingHashMap<String, String>();  

	/** A map of KnownHosts objects keyed by the file name where it was loaded from */
	private final NonBlockingHashMap<String, KnownHosts> knownHosts = new NonBlockingHashMap<String, KnownHosts>();  

	/** A map of RSA private keys keyed by the key file name */
	private final NonBlockingHashMap<String, RSAPrivateKey> rsaKeys = new NonBlockingHashMap<String, RSAPrivateKey>(); 
	/** A map of DSA private keys keyed by the key file name */
	private final NonBlockingHashMap<String, DSAPrivateKey> dsaKeys = new NonBlockingHashMap<String, DSAPrivateKey>(); 
	
	/**
	 * Acquires the TunnelManager singleton instance
	 * @return the TunnelManager singleton instance
	 */
	public static TunnelManager getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new TunnelManager();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new TunnelManager
	 */
	private TunnelManager() {
		try {
			loadDefaultKnownHosts();
		} catch (Exception ex) {
			/* No Op */
		}
		try {
			loadDefaultKeys();
		} catch (Exception ex) {
			/* No Op */
		}

		log.info("\n\t========================================\n\tTunnelManager Created\n\t========================================\n");
	}
	
	public static void main(String[] args) {
		BasicConfigurator.configure();
		log.info("TunnelManager Test");
		final TunnelManager tm = TunnelManager.getInstance();
		SSHConnectionConfiguration config = SSHConnectionConfiguration.
				newBuilder("localhost", "oracle")
				.setUserPassword("larry")
				.setKeyExchangeTimeout(0)
				.setVerifyHosts(false)
				.build();
		
		ExtendedConnection conn = tm.getConnection(config);
		try {			
			if(conn.fullAuth()) {
				SocketAddress sa = tm.localPortForward(conn, "tpmint", 80);
				log.info("Port Forward Created at [" + sa + "]");
				CommandTerminal ct = conn.createCommandTerminal();
				log.info("Acquired command terminal [" + ct.getTty() + "]");
				CharSequence cs = ct.execWithDelim("========================\n", "ls -l", "sar -P ALL 1 1", "vmstat", "iostat");
				log.info("Command Terminal Exit Codes:" + Arrays.toString(ct.getExitStatuses()));
				log.info("Command Terminal Output:\n" + cs);
//				WrappedSession ws = conn.createSession();
//				ws.startShell();
				/*
				 * 1. Return Options:
				 * 		- Stringy
				 * 		- 
				 * 2. Initialize with pty, shell and prompt
				 */
//				ws.execShellCommand("ls -l --color=never");
//				ws.execShellCommand2("ls -l");
//				ws.execShellCommand2("cat foobar.txt");
				
				//ws.execShellCommand("echo $HOSTNAME");
				
				
//				Thread.currentThread().join();
				log.info("Closing Connection....");
				conn.close();
				log.info("Connection Closed:  Is Connected:" + conn.isConnected());
//				Thread.currentThread().join();
			} else {
				throw new Exception("Failed to connect using [" + config + "]");
			}
		} catch (Exception ex) {
			log.error("Failed to process connection", ex);
		}
		
	}
	
	/**
	 * Determines if the passed stringy is an ip address
	 * @param value The stringy to test
	 * @return true if the passed stringy is an ip address, false otherwise
	 */
	public static boolean isIPAddress(final CharSequence value) {
		if(value==null) return false;
		if(IP4_ADDRESS_PATTERN.matcher(value).matches()) return true;
		if(IP6_ADDRESS_PATTERN.matcher(value).matches()) return true;
		return false;
	}
	
	/**
	 * Returns the connection for the passed SSH host and port
	 * @param host The target host
	 * @param port The target port
	 * @return the matching connection or null if one was not found
	 */
	public ExtendedConnection getConnection(final String host, final int port) {
		return connections.get(hostNameToAddress(host) + ":" + port);
	}
	
	/**
	 * Returns the connection for the passed SSH host and port 22
	 * @param host The target host
	 * @return the matching connection or null if one was not found
	 */
	public ExtendedConnection getConnection(final String host) {
		return getConnection(host, 22);
	}
	
	/**
	 * Returns a connection for the passed config.
	 * If an existing connection for the same host/port exists, that will be returned.
	 * @param sshConfig The connection SSH configuration
	 * @return an SSH connection
	 */
	public ExtendedConnection getConnection(final SSHConnectionConfiguration sshConfig) {
		ExtendedConnection conn = connections.get(sshConfig.key());
		if(conn==null) {
			synchronized(connections) {
				conn = connections.get(sshConfig.key());
				if(conn==null) {
					conn = connect(sshConfig);
					connections.put(sshConfig.key(), conn);
				}
			}
		}
		return conn;
	}

	/**
	 * Returns a connection for the passed config.
	 * If an existing connection for the same host/port exists, that will be returned.
	 * @param sshConfig The connection SSH configuration
	 * @return an SSH connection
	 */
	public ExtendedConnection getConnection(final Properties sshConfig) {
		return getConnection(SSHConnectionConfiguration.getInstance(sshConfig));
	}
	
	ExtendedConnection connect(final SSHConnectionConfiguration sshConfig) {
		final ExtendedConnection conn = new ExtendedConnection(sshConfig);		
		return conn;
	}
	
	
	
	/**
	 * Returns an SSH KnownHosts file for the passed file
	 * @param file The KnownHosts file
	 * @return the KnownHosts
	 */
	public KnownHosts getKnownHosts(final File file) {
		if(file==null) throw new IllegalArgumentException("The passed file was null or empty");
		return getKnownHosts(file.getAbsolutePath());
	}
	
	/**
	 * Connects a new port forwarder
	 * @param conn The connection to port forward through
	 * @param localIface The local iface to bind to
	 * @param localPort The local port to bind to
	 * @param remoteHost The remote host to connect to
	 * @param remotePort The remote port to connect to
	 * @return the socket address of the port forward
	 */
	public SocketAddress localPortForward(final ExtendedConnection conn, final String localIface, final int localPort, final String remoteHost, final int remotePort) {
		final String _remoteHost = hostNameToAddress(remoteHost);
		final LocalPortForwarderKey pfKey = LocalPortForwarderKey.getInstance(localPort, _remoteHost, remotePort);
		WrappedLocalPortForwarder portForwarder = localPortForwarders.get(pfKey);
		if(portForwarder==null) {
			synchronized(localPortForwarders) {
				portForwarder = localPortForwarders.get(pfKey);
				if(portForwarder==null) {
					try {
						if(!conn.isConnected()) {
							conn.fullAuth();
						}
						portForwarder = conn.localPortForward(localIface, localPort, _remoteHost, remotePort);
						localPortForwarders.put(pfKey, portForwarder);
					} catch (Exception e) {
						log.error("Failed to create PortForwarder to [" + pfKey + "]", e);
						throw new RuntimeException("Failed to create PortForwarder to [" + pfKey + "]", e);
					}
				}
			}
		}
		return portForwarder.getLocalSocketAddress();
	}
	
	/**
	 * Connects a new port forwarder
	 * @param conn The connection to port forward through
	 * @param remoteHost The remote host to connect to
	 * @param remotePort The remote port to connect to
	 * @return the socket address of the port forward
	 */
	public SocketAddress localPortForward(final ExtendedConnection conn, final String remoteHost, final int remotePort) {
		return localPortForward(conn, "127.0.0.1", 0, remoteHost, remotePort);
	}
	
	
	
	/**
	 * Returns an SSH KnownHosts file for the passed file name
	 * @param fileName The KnownHosts file name
	 * @return the KnownHosts
	 */
	public KnownHosts getKnownHosts(final CharSequence fileName) {
		if(fileName==null || fileName.toString().trim().isEmpty()) throw new IllegalArgumentException("The passed file name was null or empty");
		final String _fileName = fileName.toString().trim();
		KnownHosts kh = knownHosts.get(_fileName);
		if(kh==null) {
			synchronized(knownHosts) {
				kh = knownHosts.get(_fileName);
				if(kh==null) {
					File f = new File(_fileName);
					if(f.exists() && f.isFile() && f.canRead()) {
						try {
							kh = new KnownHosts(f);
							knownHosts.put(_fileName, kh);
						} catch (Exception ex) {
							throw new RuntimeException("Failed to create known hosts for file [" + _fileName + "]", ex);
						}
					} else {
						throw new RuntimeException("Invalid Known Hosts file [" + _fileName + "]");
					}
				}
			}
		}
		return kh;
	}
	
	
	/**
	 * Resolves the passed value to an address
	 * @param value The value to resolve
	 * @return The value if it is an ip address, or the host's address if the value is a resolvable host name
	 */
	public String resolveToAddress(final String value) {
		if(value==null || value.trim().isEmpty()) throw new IllegalArgumentException("The passed value was null or empty");
		final String _value = value.trim().toLowerCase();
		if(isIPAddress(_value)) {
			return _value;
		} else {
			return hostNameToAddress(_value);
		}
	}

	
	/**
	 * Returns the ip address for the passed name
	 * @param hostName The host name to lookup the address for
	 * @return the ip address
	 */
	public final String hostNameToAddress(final String hostName) {
		if(hostName==null || hostName.trim().isEmpty()) throw new IllegalArgumentException("The passed host name was null or empty");
		try {
			if(isIPAddress(hostName)) return hostName;
			final String _hostName = hostName.trim().toLowerCase();
			String caddr = hostNameToIpAddress.get(_hostName);
			if(caddr!=null) return caddr;			
			final InetAddress[] inets = InetAddress.getAllByName(_hostName);
			if(inets==null || inets.length==0) throw new Exception("No inets returned");
			for(final InetAddress inet: inets) {
				try {
					final String addr = inet.getHostAddress();					
					if(!_hostName.equals(addr)) {
						caddr = hostNameToIpAddress.get(_hostName);
						if(caddr==null) {
							synchronized(hostNameToIpAddress) {
								caddr = hostNameToIpAddress.get(_hostName);
								if(caddr==null) {
									caddr = addr;
									hostNameToIpAddress.put(_hostName, caddr);
								}
							}
						}
						return caddr;
					}
				} catch (Exception ex) {
					log.error("Failed to process inet [" + inet + "]", ex);
				}
			}
			throw new Exception("Found no matches");
		} catch (Exception ex) {
			throw new RuntimeException("Failed to get address for name [" + hostName + "]", ex);
		}	
	}
	
	/**
	 * Returns the canonical host name for the passed address
	 * @param address The address to lookup the host name for
	 * @return the host name
	 */
	public final String addressToHostName(final String address) {
		if(address==null || address.trim().isEmpty()) throw new IllegalArgumentException("The passed address was null or empty");
		try {
			final String _address = address.trim().toLowerCase();
			String cname = ipAddressToHostName.get(_address);
			if(cname!=null) return cname;			
			final InetAddress[] inets = InetAddress.getAllByName(_address);
			if(inets==null || inets.length==0) throw new Exception("No inets returned");
			for(final InetAddress inet: inets) {
				try {
					final String canonical = inet.getCanonicalHostName().trim().toLowerCase();					
					if(!_address.equals(canonical)) {
						cname = ipAddressToHostName.get(_address);
						if(cname==null) {
							synchronized(ipAddressToHostName) {
								cname = ipAddressToHostName.get(_address);
								if(cname==null) {
									cname = canonical;
									ipAddressToHostName.put(_address, cname);
								}
							}
						}
						return cname;
					}
				} catch (Exception ex) {
					log.error("Failed to process inet [" + inet + "]", ex);
				}
			}
			throw new Exception("Found no matches");
		} catch (Exception ex) {
			throw new RuntimeException("Failed to get host names for address [" + address + "]", ex);
		}
	}
	
	private void loadDefaultKeys() {
		final File[] defaultFiles = {
			new File(DEFAULT_RSA),
			new File(DEFAULT_DSA)
		};
		for(final File f : defaultFiles) {
			if(f.exists() && f.canRead()) {
				try {
					char[] chars = readKey(f);
					Object key = PEMDecoder.decode(chars, null);
					if(key instanceof RSAPrivateKey) {
						rsaKeys.put(f.getAbsolutePath(), (RSAPrivateKey)key);
						log.info("Loaded RSA Private Key from [" + f + "]");
					} else if(key instanceof DSAPrivateKey) {
						dsaKeys.put(f.getAbsolutePath(), (DSAPrivateKey)key);
						log.info("Loaded DSA Private Key from [" + f + "]");
					} else {
						log.warn("Unrecognized key type [" + key.getClass().getName() + "] in file [" + f + "]");
					}					
					chars = null;
				} catch (Exception ex) {
					log.warn("Could not load key [" + f + "]");
				}
			}
		}
	}
	
	/**
	 * Loads a private key from the passed file name
	 * @param fileName The name of the file containing the private key
	 * @return the loaded and decrypted key
	 */
	public Object getKey(final String fileName) {
		return getKey(fileName, null);
	}
	
	/**
	 * Loads a private key from the passed file name
	 * @param fileName The name of the file containing the private key
	 * @param passphrase An optional private key passphrase
	 * @return the loaded and decrypted key
	 */
	public Object getKey(final String fileName, final String passphrase) {
		return getKey(new File(fileName), passphrase);
	}
	
	/**
	 * Loads a private key from the passed file
	 * @param f The file containing the private key
	 * @return the loaded and decrypted key
	 */
	public Object getKey(final File f) {
		return getKey(f, null);
	}
	
	/**
	 * Loads a private key from the passed file
	 * @param f The file containing the private key
	 * @param passphrase An optional private key passphrase
	 * @return the loaded and decrypted key
	 */
	public Object getKey(final File f, final String passphrase) {
		return getKey(readKey(f), passphrase);
	}
	
	/**
	 * Decodes the private DSA or RSA key represented in the passed char array
	 * @param chars A character array containing the characters of a RSA or DSA private key
	 * @return the decoded key
	 */
	private Object getKey(final char[] chars) {
		return getKey(chars, null);
	}
	
	/**
	 * Decodes the private DSA or RSA key represented in the passed char array
	 * @param chars A character array containing the characters of a RSA or DSA private key
	 * @param passphrase An optional private key passphrase
	 * @return the decoded key
	 */
	private Object getKey(final char[] chars, final String passphrase) {
		try {
			return PEMDecoder.decode(chars, passphrase);
		} catch (Exception ex) {
			throw new RuntimeException("Could not load key from char array", ex);
		}		
	}
	
	private char[] readKey(final File keyFile) {
		if(keyFile.exists() && keyFile.canRead()) {
			StringBuilder b = new StringBuilder();
			FileReader fr = null;
			char[] chars = new char[1024];
			int charsRead = -1;
			int totalChars = 0;
			try {
				fr = new FileReader(keyFile);
				while((charsRead = fr.read(chars))!=-1) {
					totalChars += charsRead;
					b.append(chars, 0, charsRead);
				}
				chars = new char[totalChars];
				b.getChars(0, totalChars-1, chars, 0);
				b.setLength(0);
				return chars;
			} catch (Exception ex) {
				throw new RuntimeException("Failure reading the file [" + keyFile + "]", ex);
			} finally {
				if(fr!=null) try { fr.close(); } catch (Exception x) {/* No Op */}
			}
		}
		throw new RuntimeException("Cannot read the file [" + keyFile + "]");
	}
	
	private void loadDefaultKnownHosts() {
		final File f = new File(System.getProperty("user.home") + File.separator + ".ssh" + File.separator + "known_hosts");
		getKnownHosts(f);
		log.info("Loaded default KnownHosts file at [" + f.getAbsolutePath() + "]");
	}
	

}
