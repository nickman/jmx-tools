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
package org.helios.jmx.metrics;

import java.lang.reflect.AccessibleObject;
import java.util.Map;
import java.util.regex.Pattern;

import org.cliffc.high_scale_lib.NonBlockingHashMap;

/**
 * <p>Title: AccumulatorKeyFactory</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.metrics.AccumulatorKeyFactory</code></p>
 */

public class AccumulatorKeyFactory {
	/** A cache of accumulator keys keyed by the accessible object they were generated for */
	protected static final Map<AccessibleObject, String> KEY_CACHE = new NonBlockingHashMap<AccessibleObject, String>();
	
	/** The default mask if one is not provided */
	public static final String DEFAULT_MASK = "";
	
	/** The pattern to parse the specified mask */
	public static final Pattern KEY_FRAG_PATTERN = Pattern.compile("((?:\\{(a):(\\d+?)\\})|(?:\\{p\\})|(?:\\{c\\})|(?:\\{m\\}))");
	
	public static String getAccumulatorKey(AccessibleObject ao, String mask) {
		if(ao==null) throw new IllegalArgumentException("The passed accessible object was null");
		String key = KEY_CACHE.get(ao);
		if(key==null) {
			synchronized(KEY_CACHE) {
				key = KEY_CACHE.get(ao);
				if(key==null) {
					if(mask==null || mask.isEmpty()) mask = DEFAULT_MASK;
				}
			}
		}
		
		
		return null;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	public static void log(String format, Object...args) {
		System.out.println(String.format(format, args));
	}	

}
