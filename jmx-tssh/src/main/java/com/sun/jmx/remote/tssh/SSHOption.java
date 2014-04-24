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
package com.sun.jmx.remote.tssh;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: SSHOption</p>
 * <p>Description: Functional enumeration of the SSH connection parameters</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.sun.jmx.remote.tssh.SSHOption</code></p>
 */

public enum SSHOption {
	/** The SSH user name */
	USER("u", "tssh.user", System.getProperty("user.name")),
	/** The SSH user password */
	PASSWORD("p", "tssh.password", null),
	/** The SSH private key (file, URL, char[] or CharSequence) */
	KEY("k", "tssh.privatekey", String.format("%s%s.ssh%sid_dsa", System.getProperty("user.home"), File.separator, File.separator)),
	/** The private key passphrase */
	KEYPHR("kp", "tssh.passphrase", null),
	/** The SSH host to connect to */
	HOST("h", "tssh.host", "localhost"),
	/** The listening port of the SSH host to connect to */
	PORT("pt", "tssh.port", "22"),
	/** Indicates if the server key should be validated */
	SVRKEY("sk", "tssh.serverkey", "true"),
	/** The local hosts file to use if server keys are validated */
	HOSTFILE("hf", "tssh.hostfile", String.format("%s%s.ssh%sknown_hosts", System.getProperty("user.home"), File.separator, File.separator)),
	/** A URL or file name to load ssh params from as properties */
	SSHPROPS("pr", "tssh.propfile", String.format("%s%s.ssh%sjmx.tssh", System.getProperty("user.home"), File.separator, File.separator)),
	/** The property prefix to use when reading from properties */
	PROPSPREF("pref", "tssh.proppref", null);
	
	/** A map of SSHOptions keyed by the short code */
	public static final Map<String, SSHOption> CODE2ENUM;
	/** A map of SSHOptions keyed by the ordinal */
	public static final Map<Integer, SSHOption> ORD2ENUM;
	/** A map of SSHOptions keyed by the property name */
	public static final Map<String, SSHOption> PROP2ENUM;
	
	static {
		SSHOption[] options = SSHOption.values();
		Map<String, SSHOption> tmpByCode = new HashMap<>(options.length);
		Map<String, SSHOption> tmpByProp = new HashMap<>(options.length);
		Map<Integer, SSHOption> tmpByOrd = new HashMap<>(options.length);
		for(SSHOption opt: options) {
			tmpByCode.put(opt.shortCode, opt);
			tmpByProp.put(opt.propertyName, opt);
			tmpByOrd.put(opt.ordinal(), opt);
		}
		CODE2ENUM = Collections.unmodifiableMap(tmpByCode);
		PROP2ENUM = Collections.unmodifiableMap(tmpByProp);
		ORD2ENUM = Collections.unmodifiableMap(tmpByOrd);
	}
	
	/**
	 * Creates a new SSHOption
	 * @param shortCode The short code for the SSHOption
	 * @param propertyName The system property or env variable name to get this value from
	 * @param defaultValue The default value if no value can be found, null meaning no default
	 */
	private SSHOption(String shortCode, String propertyName, String defaultValue) {
		this.shortCode = shortCode;
		this.propertyName = propertyName;
		this.defaultValue = defaultValue;
	}
	
	/** The short code for the SSHOption */
	public final String shortCode;
	/** The system property or env variable name to get this value from */
	public final String propertyName;
	/** The default value if no value can be found, null meaning no default */
	public final String defaultValue;
	

	
}


/*
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;

raf = null;
fc = null;
try {
    raf = new RandomAccessFile("/home/nwhitehead/.ssh/id_dsa", "r");
    fc = raf.getChannel();
    mbb  = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
    mbb.load();
    decoder = Charset.defaultCharset().newDecoder();
    println decoder.decode(mbb).toString()
    
     
} finally {
    try { raf.close(); } catch (e) {}
    try { fc.close(); } catch (e) {}
}
 */ 
