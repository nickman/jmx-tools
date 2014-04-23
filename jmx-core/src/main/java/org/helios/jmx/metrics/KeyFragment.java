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

import java.util.regex.Pattern;

/**
 * <p>Title: KeyFragment</p>
 * <p>Description: Functional enumeration of the tokens supported for creating an accumulator key</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.metrics.KeyFragment</code></p>
 */

public enum KeyFragment {
	/** The package name, not including the class */
	PACKAGE("p", Pattern.compile("\\{p\\}")),
	/** The simple class name */
	CLASS("c", Pattern.compile("\\{c\\}")),
	/** The method or constructor name */
	METHOD("m", Pattern.compile("\\{m\\}")),
	/** The string value of an argument */
	ARG("a", Pattern.compile("\\{a:\\d+?\\}"));
	
	private KeyFragment(String code, Pattern pattern) {
		this.code = code;
		this.pattern = pattern;
	}
	
	public final String code;
	public final Pattern pattern;
}
