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
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;

import org.helios.rindle.util.helpers.ConfigurationHelper;

/**
 * <p>Title: OptionReaders</p>
 * <p>Description: SSHOption reader classes</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.sun.jmx.remote.tssh.OptionReaders</code></p>
 */

public class OptionReaders {
	
	/** Default charset */
	public static final Charset CHARSET = Charset.defaultCharset();
	
	/** Static shareable string reader instance */
	public static final StringReader STRING_READER = new StringReader();
	/** Static shareable Int reader instance */
	public static final IntReader INT_READER = new IntReader();
	/** Static shareable Char Array reader instance */
	public static final CharArrReader CHAR_ARR_READER = new CharArrReader();
	
	
	/**
	 * Reads properties in from the passed source
	 * @param source The source name which may be a file or a URL
	 * @return the read properties or null if not found or failed to read
	 */
	public static Properties getProperties(String source) {
		if(source==null || source.trim().isEmpty()) return null;
		String sourceName = source.trim().toLowerCase();
		File file = new File(source);
		Properties props = null;
		InputStream is = null;
		try {
			if(file.canRead()) {
				is = new FileInputStream(file);
				props = new Properties();
				if(sourceName.endsWith("xml")) {
					props.loadFromXML(is);
				} else {
					props.load(is);
				}
			} else {
				URL url = null;
				try { url = new URL(source.trim()); } catch (Exception x) { /* No Op */ }
				if(url!=null) {
					is = url.openStream();
					props = new Properties();
					if(sourceName.endsWith("xml")) {
						props.loadFromXML(is);
					} else {
						props.load(is);
					}					
				}
			}
		} catch (Exception ex) {
			/* No Op */
		} finally {
			if(is!=null) try { is.close(); } catch (Exception x) { /* No Op */ }
		}
		return props;
	}
	
	/**
	 * Reads the passed source as a char array
	 * @param source The source name which may be a file or a URL
	 * @return the read content as a char array or null if not found or failed to read
	 */
	public static char[] getChars(String source) {
		if(source==null || source.trim().isEmpty()) return null;
		String sourceName = source.trim().toLowerCase();
		File file = new File(source);
		char[] arr = null;
		InputStream is = null;
		InputStreamReader isr = null;
		FileChannel fileChannel = null;
		try {
			if(file.canRead()) {
				is = new FileInputStream(file);
				fileChannel = ((FileInputStream)is).getChannel();
				fileChannel.map(MapMode.READ_ONLY, 0, fileChannel.size());
				ByteBuffer bb = ByteBuffer.allocate((int) fileChannel.size());
				CharBuffer cb = bb.asCharBuffer();
				cb.
				
				props = new Properties();
				if(sourceName.endsWith("xml")) {
					props.loadFromXML(is);
				} else {
					props.load(is);
				}
			} else {
				URL url = null;
				try { url = new URL(source.trim()); } catch (Exception x) { /* No Op */ }
				if(url!=null) {
					
					is = url.openStream();
					isr = new InputStreamReader(is, CHARSET);
					isr.re
					props = new Properties();
					if(sourceName.endsWith("xml")) {
						props.loadFromXML(is);
					} else {
						props.load(is);
					}					
				}
			}
		} catch (Exception ex) {
			/* No Op */
		} finally {
			if(isr!=null) try { isr.close(); } catch (Exception x) { /* No Op */ }
			if(is!=null) try { is.close(); } catch (Exception x) { /* No Op */ }
		}
		return arr;
	}
	
	
	
	/**
	 * <p>Title: StringReader</p>
	 * <p>Description: String type SSH option reader</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.sun.jmx.remote.tssh.OptionReades.StringReader</code></p>
	 */
	public static class StringReader implements ISSHOptionReader<String> {
		/**
		 * {@inheritDoc}
		 * @see com.sun.jmx.remote.tssh.ISSHOptionReader#getOption(java.util.Map, java.lang.String, java.lang.Object)
		 */
		@Override
		public String getOption(Map env, String key, String defaultValue) {
			if(env==null || key==null || key.trim().isEmpty()) return defaultValue; 
			Object value = env.get(key);
			if(value==null) {
				value = getOption(SSHOption.PROPSPREF.propertyName + "." + key, null);
			}
			return value==null ? defaultValue : value.toString().trim();
		}

		/**
		 * {@inheritDoc}
		 * @see com.sun.jmx.remote.tssh.ISSHOptionReader#getOption(java.lang.String, java.lang.Object)
		 */
		@Override
		public String getOption(String key, String defaultValue) {
			String val = ConfigurationHelper.getSystemThenEnvProperty(key, defaultValue);
			return val==null ? null : val.trim();
		}
	}
	
	/**
	 * <p>Title: IntReader</p>
	 * <p>Description: Integer value option reader</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.sun.jmx.remote.tssh.OptionReaders.IntReader</code></p>
	 */
	public static class IntReader implements ISSHOptionReader<Integer> {

		/**
		 * {@inheritDoc}
		 * @see com.sun.jmx.remote.tssh.ISSHOptionReader#getOption(java.util.Map, java.lang.String, java.lang.Object)
		 */
		@Override
		public Integer getOption(Map env, String key, Integer defaultValue) {
			try {
				String val = STRING_READER.getOption(env, key, null);
				if(val!=null) return Integer.parseInt(val);
			} catch (Exception x) { /* No Op */ }
			return defaultValue;
		}

		/**
		 * {@inheritDoc}
		 * @see com.sun.jmx.remote.tssh.ISSHOptionReader#getOption(java.lang.String, java.lang.Object)
		 */
		@Override
		public Integer getOption(String key, Integer defaultValue) {
			String val = STRING_READER.getOption(key, null);
			try {
				if(val!=null) return Integer.parseInt(val);
			} catch (Exception x) { /* No Op */ }
			return defaultValue;
		}
		
	}
	
	/**
	 * <p>Title: CharArrReader</p>
	 * <p>Description: An option reader for char arrays</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.sun.jmx.remote.tssh.OptionReaders.CharArrReader</code></p>
	 */
	public static class CharArrReader implements ISSHOptionReader<char[]> {

		@Override
		public char[] getOption(Map env, String key, char[] defaultValue) {
			String val = STRING_READER.getOption(key, null);
			return null;
		}

		@Override
		public char[] getOption(String key, char[] defaultValue) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}
