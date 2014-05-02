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
package org.helios.jmx.remote.tunnel;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.remote.JMXServiceURL;

import org.helios.jmx.remote.InetAddressCache;
import org.helios.jmx.util.helpers.ConfigurationHelper;
import org.helios.jmx.util.helpers.JMXHelper;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.ConnectionMonitor;
import ch.ethz.ssh2.KnownHosts;
import ch.ethz.ssh2.ServerHostKeyVerifier;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import ch.ethz.ssh2.crypto.PEMDecoder;
import ch.ethz.ssh2.signature.DSAPrivateKey;
import ch.ethz.ssh2.signature.RSAPrivateKey;

/**
 * <p>Title: SSHTunnelConnector</p>
 * <p>Description: Gathers and encapsulates the data required to establish an SSH connection</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.remote.tunnel.SSHTunnelConnector</code></p>
 */

public class SSHTunnelConnector implements ServerHostKeyVerifier, ConnectionMonitor {
	/** The SSH Host */
	protected String sshHost = null;
	/** The SSHD listening sshPort */
	protected int sshPort = 22;
	/** The target jmx connector server sshHost */
	protected String jmxConnectorHost = null;
	/** The target jmx connector server sshPort */
	protected int jmxConnectorPort = -1;
	
	/** The local listening sshPort */
	protected int localPort = 0;
	/** The SSH user name */
	protected String userName = null;
	/** The SSH user password */
	protected String userPassword = null;
	/** The private key passphrase */
	protected String passPhrase = null;
	/** The characters of the private key */
	protected char[] privateKey = null;
	/** The delegate protocol */
	protected String delegateProtocol = "jmxmp";
	/** The sub protocol */
	protected String subProtocol = "ssh";
	
	/** the key type */
	protected String keyType = null;
	
	/** The ssh known hosts file name */
	protected String knownHostsFile = null;
	/** The ssh known hosts repository */
	protected KnownHosts knownHosts = null;
	/** Validate server ssh key */
	protected boolean validateServer = true;

	/** The SSH connect timeout in ms. */
	protected int sshConnectTimeout = 0;
	/** The SSH key exchange timeout in ms. */
	protected int sshKeyExchangeTimeout = 0;
	
	
	
	/** A map of known sshHost instances keyed by the underlying file name */
	protected static final Map<String, KnownHosts> KNOWN_HOSTS = new ConcurrentHashMap<String, KnownHosts>();
	
	
	
	
	/** The message returned by the PEMDecoder if the pk is encrypted but we supplied no passphrase */
	public static final String NO_PASSPHRASE_MESSAGE = "PEM is encrypted, but no password was specified";
	
	/** The default DSA Private Key location and file name */
	public static final String DEFAULT_DSA = String.format("%s%s.ssh%sid_dsa", System.getProperty("user.home"), File.separator, File.separator);
	/** The default RSA Private Key location and file name */
	public static final String DEFAULT_RSA = String.format("%s%s.ssh%sid_rsa", System.getProperty("user.home"), File.separator, File.separator);
	/** The default TSSH property config location and file name */
	public static final String DEFAULT_PROPS = String.format("%s%s.ssh%stunnel.properties", System.getProperty("user.home"), File.separator, File.separator);
	
	/** Splitter for the SSH args */
	public static final Pattern SPLIT_TRIM_SSH = Pattern.compile("\\s*,\\s*");
	/** Splitter for the SSH key-val pairs */
	public static final Pattern SPLIT_SSH_ARG = Pattern.compile("\\s*=\\s*");
	/** The pattern of the TSSH URL Path */
	public static final Pattern TSSH_URL_PATH_PATTERN = Pattern.compile("/(.*?)/(.*?):(.*)");
	
	/**
	 * Creates a new SSHTunnelConnector
	 * @param jmxServiceURL The JMXServiceURL requested to connect to 
	 * @param env An optional environment map
	 */
	public SSHTunnelConnector(JMXServiceURL jmxServiceURL, Map env) {
		if(jmxServiceURL==null) throw new IllegalArgumentException("The passed JMXServiceURL was null");

		Map<SSHOption, Object> options = gather(jmxServiceURL, env);
		for(Map.Entry<SSHOption, Object> entry: options.entrySet()) {
			SSHOption option = entry.getKey();
			Object optionValue = entry.getValue();
			switch(option) {
			case DELPROTO:
				delegateProtocol = optionValue.toString();
				break;
			case HOST:
				sshHost = optionValue.toString();
				break;
			case HOSTFILE:
				if(OptionReaders.isFile(optionValue.toString())) {
					knownHostsFile = optionValue.toString();
					buildKnownHosts();
				}
				break;
			case KEY:
				privateKey = (char[])optionValue;				 
				break;
			case KEYPHR:
				passPhrase = optionValue.toString();
				break;
			case LOCAL_PORT:
				int lp = (Integer)optionValue;
				if(lp!=0) {
					localPort = lp;
				}
				break;
			case PASS:
				userPassword = optionValue.toString();
				break;
			case PORT:
				sshPort = (Integer)optionValue;
				break;
			case PROPSPREF:
				break;
			case SSHPROPS:
				break;
			case SUBPROTO:
				subProtocol = optionValue.toString();
				break;
			case SVRKEY:
				validateServer = (Boolean)optionValue;
				break;
			case USER:
				userName = optionValue.toString();
				break;
			case SSHTO:
				sshConnectTimeout = (Integer)optionValue;
				break;
			case SSHKTO:
				sshKeyExchangeTimeout = (Integer)optionValue;
				break;
//			case JMXHOST:
//				jmxConnectorHost = optionValue.toString();
//				break;
//			case JMXPORT:
//				jmxConnectorPort = (Integer)optionValue;
//				break;
				
			default:
				break;
			
			}			
			setKeyType();
		}		
		try {
			jmxConnectorPort = jmxServiceURL.getPort();
		} catch (Exception ex) {/* No Op */}
		try {
			jmxConnectorHost = jmxServiceURL.getHost();
		} catch (Exception ex) {/* No Op */}
		if(sshHost==null) {
			sshHost = jmxConnectorHost;
		}
	}
	
	/*
	 * sshHost/sshPort: from jmxServiceURL.getHost() - The actual target endpoint, may also be the SSH sshHost to tunnel through (the bridge sshHost)
	 * jmxConnectorHost/jmxConnectorPort:  option [jmxh] specified, the SSH bridge sshHost. If not specified, is the same as the sshHost 
	 */
	
	public static Map tunnel(JMXServiceURL jmxServiceURL) {
		return tunnel(jmxServiceURL, null);
	}
	
	public static Map tunnel(JMXServiceURL jmxServiceURL, Map env) {
		SSHTunnelConnector tc = new SSHTunnelConnector(jmxServiceURL, env);
		TunnelRepository.getInstance().connect(tc);
		TunnelHandle tunnelHandle = TunnelRepository.getInstance().tunnel(tc);
		//TunnelRepository.getInstance().tunnel(bridgeHost, bridgePort, targetHost, targetPort, localPort)
		// TODO: 
		// add jmxopts to map
		// add connector to map
		if(env==null) {
			env = new HashMap();
		}
		JMXServiceURL serviceURL = JMXHelper.serviceUrl("service:jmx:%s://%s:%s", tc.getDelegateProtocol(), tc.getJmxConnectorHost(), tc.getJmxConnectorPort());
		env.put("JMXServiceURL", serviceURL);
		env.put("TunnelHandle", tunnelHandle);
		return env;
	}
	
	/**
	 * Validates the most basic requirements to establish a connection 
	 */
	protected void validateRequirementsLevelOne() {
		String[] aliases = InetAddressCache.getInstance().getAliases(sshHost);
		if(sshPort < 1 || sshPort > 65534) throw new IllegalArgumentException("The configured sshPort is out of range [" + sshPort + "]");
		if(userName==null || userName.trim().isEmpty()) throw new IllegalArgumentException("The configured user name is null or empty");
	}
	

	
	/**
	 * Attempts to establish an authenticated connection based on the state of this connector
	 * @return a connected and authenticated connection
	 */
	public Connection connectAndAuthenticate() {
		Connection conn = null;
		Set<String> authFailures = new LinkedHashSet<String>();
		validateRequirementsLevelOne();
		boolean authenticated = false;
		try {
			log("Connecting to: [" + sshHost + ":" + sshPort + "]");
			conn = new Connection(sshHost, sshPort);
			
			if(validateServer) {
				conn.connect(this, sshConnectTimeout, sshKeyExchangeTimeout);
			} else {
				conn.connect(null, sshConnectTimeout, sshKeyExchangeTimeout);
			}
			authenticated = conn.authenticateWithNone(userName);
			if(authenticated) return conn;
			authFailures.add("authenticateWithNone failed for user [" + userName + "]");
			if(userPassword!=null) {
				authenticated = conn.authenticateWithPassword(userName, userPassword);
				if(authenticated) return conn;
				authFailures.add("authenticateWithPassword failed for user [" + userName + "]");
			} else {
				authFailures.add("authenticateWithPassword skipped for [" + userName + "] since password was null");
			}
			authenticated = conn.authenticateWithPublicKey(userName, privateKey, passPhrase);
			if(authenticated) return conn;
			authFailures.add("authenticateWithPublicKey failed for user [" + userName + "]");
			throw new RuntimeException("Unable to authenticate user [" + userName + "]. Attempted methods:" + authFailures.toString());
		} catch (Exception ex) {
			if(conn!=null) try { conn.close(); } catch (Exception x) {/* No Op */}
			if(ex instanceof RuntimeException) {
				throw (RuntimeException)ex;
			}
			throw new RuntimeException("Unexpected connection failure", ex);
		} finally {
//			if(conn!=null) try { conn.close(); } catch (Exception x) {/* No Op */}
		}
	}
	
	public static String dumpHostInfo(Connection conn) {
		Session sess = null;
		try {
			sess = conn.openSession();
			sess.execCommand("uname -a && date && uptime && who");
			InputStream stdout = new StreamGobbler(sess.getStdout());
			BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
			StringBuilder b = new StringBuilder();
			while (true) {
				String line = br.readLine();				
				if (line == null) {
					br.close();
					break;
				}
				b.append("\n").append(line);
			}
			return b.toString();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		} finally {
			if(sess!=null) try { sess.close(); } catch (Exception x) {/* No Op */}
		}
			
	}
	
	
	/**
	 * Builds the know sshHost instance or resets if fails
	 */
	private void buildKnownHosts() {
		if(knownHostsFile!=null && OptionReaders.isFile(knownHostsFile)) {
			knownHostsFile = knownHostsFile.trim();
			KnownHosts kh = KNOWN_HOSTS.get(knownHostsFile);
			if(kh==null) {
				synchronized(KNOWN_HOSTS) {
					kh = KNOWN_HOSTS.get(knownHostsFile);
					if(kh==null) {
						try {
							kh = new KnownHosts(new File(knownHostsFile));
							KNOWN_HOSTS.put(knownHostsFile, kh);
						} catch (Exception ex) {
							knownHostsFile = null;
							knownHosts = null;
							throw new RuntimeException("Failed to build a known hosts instance from file [" + knownHostsFile + "]");
						}
					}
				}
			}
			knownHosts = kh;
		}
		
	}



	public static void log(Object format, Object...args) {
		System.out.println(String.format("[SSHTunnelConnector]" + format.toString(),args));
	}
	
	public static enum KeyStatus {
		/** Key is ok */
		OK, 
		/** Key validation failed */
		NOWAY,
		/** Key requires passphrase */
		PASS;
	}
	/**
	 * <p>Title: OptionSource</p>
	 * <p>Description: Enumerates the sources where SSHOptions can be decoded from, in precdence order</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.jmx.remote.tunnel.SSHTunnelConnector.OptionSource</code></p>
	 */
	public static enum OptionSource {
		JMXServiceURL,
		EnvMap,
		PropFile,
		DefaultPropFile,
		ConfigHelper,
		DefaultKeys;
	}
	

	
	
	/**
	 * Extracts the JMXServiceURL encoded SSH options into a map
	 * @param jmxServiceURL The JMXServiceURL to extract from
	 * @return the map of extracted options
	 */
	public static Map extractJMXServiceURLOpts(JMXServiceURL jmxServiceURL) {
		if(jmxServiceURL==null) return Collections.EMPTY_MAP;
		Map map = new HashMap();
		String urlArgs = jmxServiceURL.getURLPath();		
		
		Matcher m = TSSH_URL_PATH_PATTERN.matcher(urlArgs);
		if(!m.matches()) {
			throw new RuntimeException("Failed to recognize URL Path pattern [" + urlArgs + "]");
		} 
		String subProtocol = m.group(1);
		map.put(SSHOption.SUBPROTO, subProtocol);
		String delegateProtocol = m.group(2);
		map.put(SSHOption.DELPROTO, delegateProtocol);
		urlArgs = m.group(3);
//		log("Extracted: protocolPrefix:[%s], delegateProtocol:[%s], Args:[%s]", subProtocol, delegateProtocol, urlArgs);
		if(urlArgs==null || urlArgs.trim().isEmpty()) return Collections.EMPTY_MAP;
		for(String pair: SPLIT_TRIM_SSH.split(urlArgs.trim())) {
//			log("\tPair:[%s]", pair);
			String[] kv = SPLIT_SSH_ARG.split(pair);
			
			if(kv!=null && kv.length==2) {
//				log("\tSplit Pair:[%s:%s]", kv[0], kv[1]);
				SSHOption option = SSHOption.decode(kv[0]);
				if(option==null) continue;
				Object optionValue = option.optionReader.convert(kv[1], null);
				if(option!=null && optionValue != null) {
					map.put(option, optionValue);
				}
			}
		}
		return map;
	}
	
	
	protected void setKeyType() {
		if(privateKey!=null) {
			try {
				Object key = PEMDecoder.decode(privateKey, passPhrase);
				if(key instanceof DSAPrivateKey) {
					keyType = "dsa";
				} if(key instanceof RSAPrivateKey) {
					keyType = "rsa";
				}
			} catch (IOException iex) {
				//privateKey = null;
			}
			
		}
	}
	
	/**
	 * Validates an SSH key
	 * @return the status of the key validation
	 */
	protected KeyStatus validateKey() {
		if(privateKey==null) return KeyStatus.NOWAY;
		Object key = null;
		try {
			key = PEMDecoder.decode(privateKey, passPhrase);
		} catch (IOException iex) {
			return KeyStatus.NOWAY;
		}
		if(key instanceof DSAPrivateKey || key instanceof RSAPrivateKey) {
			try {
				PEMDecoder.decode(privateKey, passPhrase);
				return KeyStatus.OK;
			} catch (Exception ex) {
				if(NO_PASSPHRASE_MESSAGE.equals(ex.getMessage())) {
					return KeyStatus.PASS;
				}
				return KeyStatus.NOWAY;
			}
		}
		return KeyStatus.NOWAY;
	}
	
	public static void main(String[] args) {
		try {
			log("Testing Gather");
			// u h k pt
			JMXServiceURL[] urls = new JMXServiceURL[] {
					new JMXServiceURL("service:jmx:tunnel://localhost:8006/ssh/jmxmp:u=nwhitehead,h=pdk-pt-ceas-03,pt=22,k=c,jmxu=admin,kp=helios"),
					new JMXServiceURL("service:jmx:tunnel://localhost:8006/ssh/jmxmp:u=nwhitehead,h=pdk-pt-ceas-03,pt=22,k=c:/users/nwhitehe/.ssh/id_dsa"),
					new JMXServiceURL("service:jmx:tunnel://localhost:8006/ssh/jmxmp:u=nwhitehead,h=pdk-pt-ceas-03,pt=22,k=c:/users/nwhitehe/.ssh/id_rsa_2048"),
					new JMXServiceURL("service:jmx:tunnel://localhost:8006/ssh/jmxmp:u=nwhitehead,h=pdk-pt-ceas-03,pt=22,k=c:/users/nwhitehe/.ssh/id_rsa_8092")
			};
			//JMXServiceURL url = new JMXServiceURL("service:jmx:tunnel://localhost:8006/ssh/jmxmp:u=nwhitehead,h=pdk-pt-ceas-03,pt=22,k=c:/users/nwhitehe/.ssh/id_dsa");
			//gather(url, null);
			for(JMXServiceURL url: urls) {
				SSHTunnelConnector stc = new SSHTunnelConnector(url, null);
				log(stc.toString());
			}
			
			log("Testing Connect....");
			Connection conn = null;
			try {
				//JMXServiceURL surl = new JMXServiceURL("service:jmx:tunnel://localhost:8006/ssh/jmxmp:u=nwhitehead,h=pdk-pt-ceas-03,pt=22,k=c,jmxu=admin,kp=helios");
				JMXServiceURL surl = new JMXServiceURL("service:jmx:tunnel://10.12.114.48:8006/ssh/jmxmp:u=nwhitehe,h=10.12.114.48,pt=22,k=c,jmxu=admin,p=jer1029");
				
				SSHTunnelConnector connector = new SSHTunnelConnector(surl, null);
				conn = connector.connectAndAuthenticate();
				log("Connection: authed:" + conn.isAuthenticationComplete());
				conn.forceKeyExchange();				
				log(dumpHostInfo(conn));
				log(new ConnectionInfoWrapper(conn.getConnectionInfo()));
			} finally {
				if(conn!=null) try { conn.close(); } catch (Exception x) {}
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	/**
	 * Gathers the SSHOptions from the different possible sources and then trims them down by the precedence rules
	 * @param serviceURL The JMXServiceURL being connected to
	 * @param env The optional JMXServiceURL environment map
	 * @return a map of final options
	 */
	public static Map<SSHOption, Object> gather(JMXServiceURL serviceURL, Map env) {
		Map<SSHOption, Object> top = new EnumMap<SSHOption, Object>(SSHOption.class);
		merge(gatherDefaults(), top);
		merge(gatherConfigHelper(), top);
		merge(gatherFromSource(DEFAULT_PROPS), top);
		merge(gatherFromSource(ConfigurationHelper.getSystemThenEnvProperty(SSHOption.SSHPROPS.propertyName, "")), top);
		merge(gatherFromMap(env), top);
		merge(extractJMXServiceURLOpts(serviceURL), top);
//		if(!top.isEmpty()) {
//			StringBuilder b = new StringBuilder("\n\tFinal SSHoptions:\n\t===================================================");
//			for(Map.Entry<SSHOption, Object> entry: top.entrySet()) {
//				b.append("\n\t").append(entry.getKey()).append(":").append(entry.getValue());
//			}
//			b.append("\n\t===================================================\n");
//			log(b.toString());
//		} else {
//			log("Empty ????");
//		}
		
		return top;
	}	
	
	
	/**
	 * Gathers SSHOptions from the passed map
	 * @param map The map to gather from
	 * @return a possibly empty map of SSHOptions and values
	 */
	public static Map<SSHOption, Object> gatherFromMap(Map map) {
		if(map==null || map.isEmpty()) return Collections.emptyMap();
		Map<SSHOption, Object> config = new EnumMap<SSHOption, Object>(SSHOption.class);
		for(Object e: map.entrySet()) {
			Map.Entry<Object, Object> entry = (Map.Entry<Object, Object>)e;
			String key = entry.getKey().toString();
			SSHOption option = SSHOption.decode(key);
			if(option==null) continue;
			Object value = option.optionReader.getRawOption(option.propertyName, option.defaultValue);
			if(value!=null) {
				config.put(option, value);
			}
		}
		return config;
	}
	
	/**
	 * Gathers SSHOptions from the passed file or URL source
	 * @param source A file name or URL to read properties from
	 * @return a possibly empty map of SSHOptions
	 */
	public static Map<SSHOption, Object> gatherFromSource(String source) {
		Map<SSHOption, Object> config = new EnumMap<SSHOption, Object>(SSHOption.class);
		String prefix = ConfigurationHelper.getSystemThenEnvProperty(SSHOption.PROPSPREF.propertyName, "");
		if(!prefix.isEmpty()) {
			prefix = prefix + ".";
		}
		Properties props = OptionReaders.getProperties(source);
		if(props!=null && !props.isEmpty()) {
			for(SSHOption option: SSHOption.values()) {
				String optionValue = System.getProperty(prefix + option.propertyName, null);
				if(optionValue!=null) {
					Object value = option.optionReader.getRawOption(option.propertyName, option.defaultValue);
					if(value!=null) {
						config.put(option, value);
					}
				}
			}			
		}
		return config;
	}
	
	
	/**
	 * Gathers SSH options from the system properties and environmental variables
	 * @return a possibly empty map of SSHOptions
	 */
	public static Map<SSHOption, Object> gatherConfigHelper() {
		Map<SSHOption, Object> config = new EnumMap<SSHOption, Object>(SSHOption.class);
		String prefix = ConfigurationHelper.getSystemThenEnvProperty(SSHOption.PROPSPREF.propertyName, "");
		if(!prefix.isEmpty()) {
			prefix = prefix + ".";
		}
		for(SSHOption option: SSHOption.values()) {
			String optionValue = ConfigurationHelper.getSystemThenEnvProperty(prefix + option.propertyName, null);
			if(optionValue!=null) {
				Object value = option.optionReader.getRawOption(option.propertyName, option.defaultValue);
				if(value!=null) {
					config.put(option, value);
				}
			}
		}
		return config;
	}
	
	/**
	 * Looks for the default keys (<b><code>${user.home}/.ssh/id_dsa</code></b> and <b><code>${user.home}/.ssh/id_rsa</code></b>) 
	 * @return a map that may contain a {@link SSHOption#KEY} option and the key characters
	 */
	public static Map<SSHOption, Object> gatherDefaults() {
		Map<SSHOption, Object> keys = new EnumMap<SSHOption, Object>(SSHOption.class);
		char[] key = null;
		if(OptionReaders.isFile(DEFAULT_DSA)) {
			key = OptionReaders.CHAR_ARR_READER.getOption(DEFAULT_DSA, null);
			if(key!=null) keys.put(SSHOption.KEY, key);
		}
		if(OptionReaders.isFile(DEFAULT_RSA)) {
			key = OptionReaders.CHAR_ARR_READER.getOption(DEFAULT_RSA, null);
			if(key!=null) keys.put(SSHOption.KEY, key);
		}		
		if(OptionReaders.isFile(SSHOption.HOSTFILE.defaultValue.toString())) {
			keys.put(SSHOption.HOSTFILE, SSHOption.HOSTFILE.defaultValue.toString());
		}
		return keys;
	}
	
	
	
	/**
	 * Merges a map of SSHOptions
	 * @param from The source map
	 * @param into The target map
	 */
	public static void merge(final Map<SSHOption, Object> from, final Map<SSHOption, Object> into) {
		if(from!=null && !from.isEmpty() && into!=null) {
			for(Map.Entry<SSHOption, Object> entry: from.entrySet()) {
				if(into.containsKey(entry.getKey())) {
//					log("Replacing [%s/%s] with [%s/%s]", entry.getKey(), into.get(entry.getKey()), entry.getKey(), entry.getValue());
				} else {
//					log("Initing [%s/%s]", entry.getKey(), entry.getValue()); 
				}
				into.put(entry.getKey(), entry.getValue());
			}
		}
	}



	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final int maxLen = 10;
		StringBuilder builder = new StringBuilder();
		builder.append("SSHTunnelConnector [");
		if (sshHost != null) {
			builder.append("sshHost=");
			builder.append(sshHost);
			builder.append(", ");
		}
		builder.append("sshPort=");
		builder.append(sshPort);
		builder.append(", localPort=");
		builder.append(localPort);
		builder.append(", ");
		if (userName != null) {
			builder.append("userName=");
			builder.append(userName);
			builder.append(", ");
		}
		if (userPassword != null) {
			builder.append("userPassword=");
			builder.append(userPassword);
			builder.append(", ");
		}
		if (passPhrase != null) {
			builder.append("passPhrase=");
			builder.append(passPhrase);
			builder.append(", ");
		}
		if (privateKey != null) {
			builder.append("privateKey=(").append(keyType).append(") char[").append(privateKey.length).append("], ");
		}
		if (jmxConnectorHost != null) {
			builder.append("jmxConnectorHost=");
			builder.append(jmxConnectorHost);
			builder.append(", ");
		}
		builder.append("jmxConnectorPort=");
		builder.append(jmxConnectorPort);
		builder.append(", ");
		
		if (delegateProtocol != null) {
			builder.append("delegateProtocol=");
			builder.append(delegateProtocol);
			builder.append(", ");
		}
		if (subProtocol != null) {
			builder.append("subProtocol=");
			builder.append(subProtocol);
			builder.append(", ");
		}
		if (knownHostsFile != null) {
			builder.append("knownHostsFile=");
			builder.append(knownHostsFile);
			builder.append(", ");
		}
		builder.append("validateServer=");
		builder.append(validateServer);
		
		
		
		
		builder.append("]");
		return builder.toString();
	}



	/**
	 * {@inheritDoc}
	 * @see ch.ethz.ssh2.ServerHostKeyVerifier#verifyServerHostKey(java.lang.String, int, java.lang.String, byte[])
	 */
	@Override
	public boolean verifyServerHostKey(String hostname, int port, String serverHostKeyAlgorithm, byte[] serverHostKey) throws Exception {
		if(knownHosts!=null && validateServer) {
			int result = knownHosts.verifyHostkey(hostname, serverHostKeyAlgorithm, serverHostKey);
			switch(result) {
				case KnownHosts.HOSTKEY_HAS_CHANGED:
					return false;
				case KnownHosts.HOSTKEY_IS_NEW:
					return false;
				case KnownHosts.HOSTKEY_IS_OK:
					return true;
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 * @see ch.ethz.ssh2.ConnectionMonitor#connectionLost(java.lang.Throwable)
	 */
	@Override
	public void connectionLost(Throwable reason) {
		// TODO Auto-generated method stub
		
	}

	public String getSSHHost() {
		return sshHost==null ? jmxConnectorHost : sshHost;
	}

	public int getSSHPort() {
		return sshPort;
	}

	public int getLocalPort() {
		return localPort;
	}

	public String getUserName() {
		return userName;
	}

	public String getUserPassword() {
		return userPassword;
	}

	public String getPassPhrase() {
		return passPhrase;
	}

	public char[] getPrivateKey() {
		return privateKey;
	}

	public String getDelegateProtocol() {
		return delegateProtocol;
	}

	public String getSubProtocol() {
		return subProtocol;
	}

	public String getKeyType() {
		return keyType;
	}

	public String getKnownHostsFile() {
		return knownHostsFile;
	}

	public boolean isValidateServer() {
		return validateServer;
	}

	public String getJmxConnectorHost() {
		return jmxConnectorHost;
	}

	public int getJmxConnectorPort() {
		return jmxConnectorPort;
	}

	
	
	/*
	 * Ways to find key:
	 * Default location
	 * Env Map
	 * JMXServiceURL encoded
	 * SystemProp / EnvVar
	 * Properties file
	 * 
	 * Prescedence:
	 * ============
		JMXServiceURL,
		EnvMap,
		PropFile,
		DefaultPropFile,
		ConfigHelper,
		DefaultKeys;
	 * 
	 */

}


/*
import javax.management.remote.*;
import java.util.regex.*;

surl = new JMXServiceURL("service:jmx:rmi://localhost:1099/ssh:nwhitehe[@localhost[:22]]], pw=XXXX ,key=c:/tmp/key_dsa,pp=YYYYY");
p = Pattern.compile('(?:\\s*(.*?)=(.*?)\\s*(?:,\\s-$|$|,))')
// 

println surl
urlPath = surl.getURLPath().replace("/ssh:", "").trim();
println "URLPath: [$urlPath]";
m = p.matcher(urlPath);
while(m.find()) {
    println "${m.group(1)}=${m.group(2)}";
}


println urlPath.split("\\s*,\\s*");
 */
