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
package org.helios.jmx.opentypes.otenabled;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

/**
 * <p>Title: OpenTypeEnabledURLClassLoader</p>
 * <p>Description: OpenType enabled extension of {@link URLClassLoader}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.opentypes.otenabled.OpenTypeEnabledURLClassLoader</code></p>
 */

public class OpenTypeEnabledURLClassLoader extends URLClassLoader implements Serializable {
	/**  */
	private static final long serialVersionUID = 3737018141371390969L;
	/** The composite type */
	protected static final CompositeType CTYPE;
	
	/** The stringfied URLs */
	protected final Set<String> strUrls = new CopyOnWriteArraySet<String>();
	/** The composite type value map */
	protected final Map<String, Object> valueMap = new HashMap<String, Object>(3);
	
	static {
		try {
			CTYPE = new CompositeType(OpenTypeEnabledURLClassLoader.class.getName(), "OpenType enabled extension of java.net.URLClassLoader", 
					new String[]{"Name", "Parent", "URLs"}, // itemNames 
					new String[]{"The name of this classloader", "The name of the parent classloader", "The URLs of this classloader's paths"}, // itemDescriptions 
					new OpenType<?>[]{SimpleType.STRING, SimpleType.STRING, new ArrayType<Object>(1, SimpleType.STRING) }); // itemTypes
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Replaces this object with a CompositeData representation when serialized
	 * @return a CompositeData representation of this object
	 * @throws ObjectStreamException thrown on error writing the object to the object output stream 
	 */
	Object writeReplace() throws ObjectStreamException {
		try {
			valueMap.put("URLs", strUrls.toArray(new String[strUrls.size()]));
			return new CompositeDataSupport(CTYPE, valueMap);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Initializes the composite type value map 
	 * @param urls The URLs the classloader was initialized with
	 */
	protected void initValueMap(URL[] urls) {
		valueMap.put("Name", super.toString());
		valueMap.put("Parent", getParent().toString());		
		if(urls!=null) {
			for(URL url: urls) {
				strUrls.add(url.toString());
			}
		}
	}
	
	
	/**
	 * Creates a new OpenTypeEnabledURLClassLoader
	 * @param urls
	 */
	public OpenTypeEnabledURLClassLoader(URL[] urls) {
		super(urls);
		initValueMap(urls);
	}

	/**
	 * Creates a new OpenTypeEnabledURLClassLoader
	 * @param urls The URLs for this classloader
	 * @param parent the parent classloader
	 */
	public OpenTypeEnabledURLClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
		initValueMap(urls);
	}

	/**
	 * Creates a new OpenTypeEnabledURLClassLoader
	 * @param urls The URLs for this classloader
	 * @param parent the parent classloader
	 * @param factory the URLStreamHandlerFactory to use when creating URLs
	 */
	public OpenTypeEnabledURLClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
		super(urls, parent, factory);
		initValueMap(urls);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.net.URLClassLoader#addURL(java.net.URL)
	 */
	@Override
	protected void addURL(URL url) {		
		super.addURL(url);
		strUrls.add(url.toString());
	}

}
