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
package org.helios.jmx.annotation;

import static org.helios.jmx.annotation.Reflector.nvl;
import static org.helios.jmx.annotation.Reflector.nws;

import java.util.HashMap;
import java.util.Map;

import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.modelmbean.DescriptorSupport;

/**
 * <p>Title: ManagedAttributeImpl</p>
 * <p>Description: A concrete bean representing an extracted {@link ManagedAttribute}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.annotation.ManagedAttributeImpl</code></p>
 */

public class ManagedAttributeImpl {
	/** The managed attribute name. Defaults to the attributized method name */
	protected final String name;
	/** The managed attribute description */
	protected final String description;
	/** An array of managed notifications that may be emitted by the annotated managed attribute */
	protected final ManagedNotificationImpl[] notifications;

	/** empty const array */
	public static final ManagedAttributeImpl[] EMPTY_ARR = {};
	/** empty const array */
	public static final MBeanAttributeInfo[] EMPTY_INFO_ARR = {};
	

	
	/**
	 * Converts an array of ManagedAttributes to an array of ManagedAttributeImpls
	 * @param attrs the array of ManagedAttributes to convert
	 * @return a [possibly zero length] array of ManagedAttributeImpls
	 */
	public static ManagedAttributeImpl[] from(ManagedAttribute...attrs) {
		if(attrs==null || attrs.length==0) return EMPTY_ARR;
		ManagedAttributeImpl[] mopis = new ManagedAttributeImpl[attrs.length];
		for(int i = 0; i < attrs.length; i++) {
			mopis[i] = new ManagedAttributeImpl(attrs[i]);
		}
		return mopis;		
	}
	
	/**
	 * Generates an array of MBeanAttributeInfos for the passed array of ManagedAttributeImpls
	 * @param attrTypes An array of metric types, one for each managed ManagedAttributeImpls
	 * @param attributes The ManagedAttributeImpls to convert
	 * @return a [possibly zero length] array of MBeanAttributeInfos
	 */
	public static MBeanAttributeInfo[] from(Class<?>[] attrTypes, ManagedAttributeImpl...attributes) {
		if(attributes==null || attributes.length==0 || attrTypes==null || attrTypes.length==0) return EMPTY_INFO_ARR;
		if(attrTypes.length != attributes.length) {
			throw new IllegalArgumentException("Type/Attribute Array Size Mismatch. Types:" + attrTypes.length + ", Metrics:" + attributes.length);
		}		
		MBeanAttributeInfo[] infos = new MBeanAttributeInfo[attributes.length];
		for(int i = 0; i < infos.length; i++) {
			infos[i] = attributes[i].toMBeanInfo(attrTypes[i]);
		}		
		return infos;		
	}
	
	
	/**
	 * Creates a new ManagedAttributeImpl
	 * @param ma The managed attribute to extract from
	 */
	public ManagedAttributeImpl(ManagedAttribute ma) {
		name = nws(nvl(ma, "Managed Attribute").name());
		description = nws(ma.description());
		this.notifications = ManagedNotificationImpl.from(ma.notifications());
	}
	
	/**
	 * Creates a new ManagedAttributeImpl
	 * @param name The attribute name specification
	 * @param description The attribute description
	 * @param notifications An array of managed notifications that may be emitted by the annotated managed attribute
	 */
	ManagedAttributeImpl(CharSequence name, CharSequence description, ManagedNotification...notifications) {
		this.name = nws(name);
		this.description = nws(description);
		this.notifications = ManagedNotificationImpl.from(notifications);
	}
	
	/**
	 * Creates a new ManagedAttributeImpl
	 * @param name The attribute name specification
	 * @param description The attribute description
	 * @param notifications An array of managed notifications that may be emitted by the annotated managed attribute
	 */
	ManagedAttributeImpl(CharSequence name, CharSequence description, ManagedNotificationImpl...notifications) {
		this.name = nws(name);
		this.description = nws(description);
		this.notifications = notifications;
	}	
	

	/**
	 * Returns the managed attribute name. Defaults to the attributized method name 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the managed attribute description
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * Returns the array of managed notifications that may be emitted by the annotated managed attribute
	 * @return the notifications
	 */
	public ManagedNotificationImpl[] getNotifications() {
		return notifications;
	}
	
	
	/**
	 * Returns an MBeanAttributeInfo rendered form this ManagedAttributeImpl.
	 * The readable and writable flags are set to true, and the isIs is set to false.
	 * @param attrType The type of the attribute
	 * @return MBeanAttributeInfo rendered form this ManagedAttributeImpl
	 */
	public MBeanAttributeInfo toMBeanInfo(Class<?> attrType) {
		return new MBeanAttributeInfo(
				name, attrType.getName(), description, true, true, false, toDescriptor()
		);
	}
	
	/**
	 * Generates a mutable MBean descriptor for this ManagedAttributeImpl
	 * @return a MBean descriptor
	 */
	public Descriptor toDescriptor() {
		return toDescriptor(false);
	}
	
	/**
	 * Generates a MBean descriptor for this ManagedAttributeImpl
	 * @param immutable true for an immutable descriptor, false otherwise
	 * @return a MBean descriptor
	 */
	public Descriptor toDescriptor(boolean immutable) {
		Map<String, Object> map = new HashMap<String, Object>();
		// FIXME: 
		return !immutable ?  new ImmutableDescriptor(map) : new DescriptorSupport(map.keySet().toArray(new String[map.size()]), map.values().toArray(new Object[map.size()]));
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("ManagedResourceImpl [name:%s, description:%s]", name==null ? "none" : name, description==null ? "none" : description);
	}	

	
}
