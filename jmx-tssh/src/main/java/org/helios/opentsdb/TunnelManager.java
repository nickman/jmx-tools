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
import java.nio.CharBuffer;
import java.util.regex.Matcher;
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
	private final NonBlockingHashMap<String, Connection> connections = new NonBlockingHashMap<String, Connection>(); 
	
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
		TunnelManager.getInstance();
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
	 * Indicates if the passed value pattern matches a V4 or v6 IP address
	 * @param value The value to test
	 * @return true if matched, false otherwise
	 */
	public static boolean isIPAddress(CharSequence value) {
		if(value==null) return false;
		Matcher m = IP4_ADDRESS_PATTERN.matcher(value);
		if(m.matches()) return true;
		m = IP6_ADDRESS_PATTERN.matcher(value);
		return m.matches();		
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
			final String _hostName = hostName.trim().toLowerCase();
			String caddr = hostNameToIpAddress.get(_hostName);
			if(caddr!=null) return caddr;			
			final InetAddress[] inets = InetAddress.getAllByName(caddr);
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
					log.error("Could not load key [" + f + "]", ex);
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
