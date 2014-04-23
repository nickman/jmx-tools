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
import java.util.Map;
import java.util.regex.Pattern;

import javax.management.remote.JMXServiceURL;

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
	
	
	public static enum KeyStatus {
		/** Key is ok */
		OK, 
		/** Key validation failed */
		NOWAY,
		/** Key requires passphrase */
		PASS;
	}
	
	
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
	
	/*
	 * Ways to find key:
	 * Default location
	 * Env Map
	 * JMXServiceURL encoded
	 * SystemProp / EnvVar
	 * Properties file
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
