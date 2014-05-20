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

import javax.management.ObjectName;

import org.helios.jmx.util.helpers.JMXHelper;

import static org.helios.jmx.annotation.Reflector.*;

/**
 * <p>Title: ManagedResourceImpl</p>
 * <p>Description: A concrete bean representing an extracted {@link ManagedResource}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.annotation.ManagedResourceImpl</code></p>
 */

public class ManagedResourceImpl {
	/** Specifies the MBean's JMX ObjectName */
	protected final ObjectName objectName;
	/** The MBean description */
	protected final String description;
	/** A name assigned to instances of this object so they can be auto-managed by a managed object mbean */
	protected final String name;
	/** Indicates if a named managed object is popable */
	protected final boolean popable;
	
	/** An array of managed notifications emitted from the annotated type */
	protected final ManagedNotificationImpl[] notifications;
	
	/** empty const array */
	public static final ManagedResourceImpl[] EMPTY_ARR = {};
	
	/**
	 * Converts an array of ManagedOperations to an array of ManagedResourceImpls
	 * @param resources the array of ManagedOperations to convert
	 * @return a [possibly zero length] array of ManagedResourceImpls
	 */
	public static ManagedResourceImpl[] from(ManagedResource...resources) {
		if(resources==null || resources.length==0) return EMPTY_ARR;
		ManagedResourceImpl[] mopis = new ManagedResourceImpl[resources.length];
		for(int i = 0; i < resources.length; i++) {
			mopis[i] = new ManagedResourceImpl(resources[i]);
		}
		return mopis;		
	}
	
	
	/**
	 * Creates a new ManagedResourceImpl
	 * @param mr The ManagedResource annotation instance to extract from
	 */
	public ManagedResourceImpl(ManagedResource mr) {
		objectName = nws(mr.objectName())==null ? null : JMXHelper.objectName(mr.objectName().trim());
		description = nws(mr.description())==null ? "JMX Managed Resource" : mr.description().trim();
		notifications = ManagedNotificationImpl.from(mr.notifications()); 
		name = nws(mr.name());
		popable = mr.popable();
	}
	
	/**
	 * Creates a new ManagedResourceImpl
	 * @param objectName The object name specification
	 * @param description The mbean description
	 * @param notifications An array of managed notifications
	 */
	ManagedResourceImpl(CharSequence objectName, CharSequence description, ManagedNotification...notifications) {
		this.objectName = objectName==null ? null : JMXHelper.objectName(objectName);
		this.description = nws(description);
		this.notifications = ManagedNotificationImpl.from(notifications);
		name = null;
		popable = false;
	}
	
	/**
	 * Creates a new ManagedResourceImpl
	 * @param objectName The object name specification
	 * @param description The mbean description
	 * @param notifications An array of managed notifications
	 */
	ManagedResourceImpl(ObjectName objectName, CharSequence description, ManagedNotification...notifications) {
		this.objectName = objectName==null ? null : objectName;
		this.description = nws(description);
		this.notifications = ManagedNotificationImpl.from(notifications);
		name = null;
		popable = false;		
	}
	
	/**
	 * Creates a new ManagedResourceImpl
	 * @param objectName The object name specification
	 * @param description The mbean description
	 * @param notifications An array of managed notifications
	 */
	ManagedResourceImpl(CharSequence objectName, CharSequence description, ManagedNotificationImpl...notifications) {
		this.objectName = objectName==null ? null : JMXHelper.objectName(objectName);
		this.description = nws(description);
		this.notifications = notifications;
		name = null;
		popable = false;
	}
	
	/**
	 * Creates a new ManagedResourceImpl
	 * @param objectName The object name specification
	 * @param description The mbean description
	 * @param notifications An array of managed notifications
	 */
	ManagedResourceImpl(ObjectName objectName, CharSequence description, ManagedNotificationImpl...notifications) {
		this.objectName = objectName==null ? null : objectName;
		this.description = nws(description);
		this.notifications = notifications;
		name = null;
		popable = false;
	}
	
	
	
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("ManagedResourceImpl [objectName:%s, description:%s, name=%s, popable:%s]", objectName==null ? "none" : objectName.toString(), description==null ? "none" : description, name==null ? "null" : name, popable);
	}
	
	/**
	 * Returns the annotation specfied ObjectName or null if one was not defined
	 * @return the objectName
	 */
	public ObjectName getObjectName() {
		return objectName;
	}
	/**
	 * Returns the 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the array of managed notifications emitted from the annotated type
	 * @return the notifications
	 */
	public ManagedNotificationImpl[] getNotifications() {
		return notifications;
	}

	
	/**
	 * The name assigned to instances of this object so they can be auto-managed by a managed object mbean
	 * @return the managed object name
	 */
	public String getName() {
		return name;
	}


	/**
	 * Indicates if this managed object is popable
	 * @return true if popable, false otherwise
	 */
	public boolean isPopable() {
		return popable;
	}

}
