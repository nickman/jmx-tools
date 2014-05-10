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
package org.helios.jmx.managed;

import static org.helios.jmx.annotation.Reflector.from;
import static org.helios.jmx.annotation.Reflector.nvl;

import java.lang.invoke.MethodHandle;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.cliffc.high_scale_lib.NonBlockingHashMapLong;
import org.helios.jmx.util.helpers.StringHelper;

/**
 * <p>Title: ManagedObjectRepo</p>
 * <p>Description: Storage and indexing for MBean managed sub-objects</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.managed.ManagedObjectRepo</code></p>
 */

public class ManagedObjectRepo {
	/** The index of managed objects, keyed by the long hash coded object name */
	final NonBlockingHashMapLong<ManagedObject> objectsByNameId = new NonBlockingHashMapLong<ManagedObject>();
	/** The index of managed objects, keyed by the managed object */
	final NonBlockingHashMap<Object, ManagedObject> objectsByTargetObject = new NonBlockingHashMap<Object, ManagedObject>();

	/**
	 * Creates a new ManagedObjectRepo
	 */
	public ManagedObjectRepo() {

	}
	
	public ManagedObject put(Object objectToAdd, String name) {
		final long id = StringHelper.longHashCode(nvl(name, "Managed Object Name"));
		nvl(objectToAdd, "Managed Object");
		ManagedObject mo = objectsByNameId.get(id);
		if(mo==null) {
			synchronized(objectsByNameId) {
				mo = objectsByNameId.get(id);
				if(mo==null) {
					
				}
			}
		}
		return mo;
	}
	
	/**
	 * <p>Title: ManagedObject</p>
	 * <p>Description: Encapculates the managed object and related meta-data</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.jmx.managed.ManagedObjectRepo.ManagedObject</code></p>
	 */
	public class ManagedObject {
		/** The managed object */
		protected Object managedObject;		
		/** The logical name of the managed object */
		protected String name;		
		/** The extracted managed object's MBean info */
		protected MBeanInfo info;
		/** The extracted managed object's MBean attribute method handles */
		final NonBlockingHashMapLong<MethodHandle[]> attrInvokers = new NonBlockingHashMapLong<MethodHandle[]>();
		/** The extracted managed object's MBean operation method handles */
		final NonBlockingHashMapLong<MethodHandle> opInvokers = new NonBlockingHashMapLong<MethodHandle>();
		
		/**
		 * Creates a new ManagedObject
		 * @param objectToAdd The object to manage
		 * @param name The logical name
		 */
		private ManagedObject(Object objectToAdd, String name) {
			managedObject = objectToAdd;
			this.name = name;			
			info = from(managedObject.getClass(), attrInvokers, opInvokers, attrInvokers);
		}

	}

}
