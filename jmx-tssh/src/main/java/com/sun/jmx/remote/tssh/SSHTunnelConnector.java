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
package com.sun.jmx.remote.tssh;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.management.remote.JMXServiceURL;

import org.helios.rindle.util.helpers.ConfigurationHelper;

import ch.ethz.ssh2.crypto.PEMDecoder;
import ch.ethz.ssh2.signature.DSAPrivateKey;
import ch.ethz.ssh2.signature.RSAPrivateKey;

/**
 * <p>Title: SSHTunnelConnector</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.sun.jmx.remote.tssh.SSHTunnelConnector</code></p>
 */

public class SSHTunnelConnector {
	/** The SSH Host */
	protected String host = null;
	/** The SSHD listening port */
	protected int port = 22;
	/** The SSH user name */
	protected String userName = null;
	/** The SSH user password */
	protected String userPassword = null;
	/** The private key passphrase */
	protected String passPhrase = null;
	/** The characters of the private key */
	protected char[] privateKey = null;
	
	
	
	/** The message returned by the PEMDecoder if the pk is encrypted but we supplied no passphrase */
	public static final String NO_PASSPHRASE_MESSAGE = "PEM is encrypted, but no password was specified";
	
	/** The default DSA Private Key location and file name */
	public static final String DEFAULT_DSA = String.format("%s%s.ssh%sid_dsa", System.getProperty("user.home"), File.separator, File.separator);
	/** The default RSA Private Key location and file name */
	public static final String DEFAULT_RSA = String.format("%s%s.ssh%sid_rsa", System.getProperty("user.home"), File.separator, File.separator);
	/** The default TSSH property config location and file name */
	public static final String DEFAULT_PROPS = String.format("%s%s.ssh%stssh.properties", System.getProperty("user.home"), File.separator, File.separator);
	
	/** Splitter for the SSH args */
	public static final Pattern SPLIT_TRIM_SSH = Pattern.compile("\\s*,\\s*");
	/** Splitter for the SSH key-val pairs */
	public static final Pattern SPLIT_SSH_ARG = Pattern.compile("\\s*=\\s*");
	
	/**
	 * Creates a new SSHTunnelConnector
	 * @param jmxServiceURL 
	 * @param env 
	 */
	public SSHTunnelConnector(JMXServiceURL jmxServiceURL, Map env) {
		if(jmxServiceURL==null) throw new IllegalArgumentException("The passed JMXServiceURL was null");
		// "service:jmx:rmi://localhost:1099/ssh:nwhitehe[@localhost[:22]]], pw=XXXX ,key=c:/tmp/key_dsa,pp=YYYYY"
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
	 * <p><code>com.sun.jmx.remote.tssh.SSHTunnelConnector.OptionSource</code></p>
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
		if(urlArgs==null || urlArgs.trim().isEmpty()) return Collections.EMPTY_MAP;
		for(String pair: SPLIT_TRIM_SSH.split(urlArgs.trim())) {
			String[] kv = SPLIT_SSH_ARG.split(pair);
			if(kv!=null && kv.length==2) {
				map.put(kv[0], kv[1]);
			}
		}
		return map;
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
			JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://localhost:3000/ssh/rmi://localhost:9000/server");
			gather(url, null);
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
		merge(gatherDefaultKeys(), top);
		merge(gatherConfigHelper(), top);
		merge(gatherFromSource(DEFAULT_PROPS), top);
		merge(gatherFromSource(ConfigurationHelper.getSystemThenEnvProperty(SSHOption.SSHPROPS.propertyName, "")), top);
		merge(gatherFromMap(env), top);
		merge(gatherFromMap(extractJMXServiceURLOpts(serviceURL)), top);
		if(!top.isEmpty()) {
			StringBuilder b = new StringBuilder("\n\tFinal SSHoptions:\n\t===================================================");
			for(Map.Entry<SSHOption, Object> entry: top.entrySet()) {
				b.append("\n\t").append(entry.getKey()).append(":").append(entry.getValue());
			}
			b.append("\n\t===================================================\n");
			log(b.toString());
		} else {
			log("Empty ????");
		}
		
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
	public static Map<SSHOption, Object> gatherDefaultKeys() {
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
					log("Replacing [%s/%s] with [%s/%s]", entry.getKey(), into.get(entry.getKey()), entry.getKey(), entry.getValue());
				} else {
					log("Initing [%s/%s]", entry.getKey(), entry.getValue()); 
				}
				into.put(entry.getKey(), entry.getValue());
			}
		}
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
