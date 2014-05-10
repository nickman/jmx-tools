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
import java.util.Arrays;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.cliffc.high_scale_lib.NonBlockingHashMapLong;
import org.cliffc.high_scale_lib.NonBlockingHashMapLong.IteratorLong;
import org.helios.jmx.annotation.Reflector;
import org.helios.jmx.annotation.Reflector.MBeanInfoMerger;
import org.helios.jmx.util.helpers.StringHelper;

/**
 * <p>Title: ManagedObjectRepo</p>
 * <p>Description: Storage and indexing for MBean managed sub-objects</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.managed.ManagedObjectRepo</code></p>
 */

public class ManagedObjectRepo<T> {
	/** The index of managed objects, keyed by the long hash coded object name */
	final NonBlockingHashMapLong<ManagedObject<T>> objectsByNameId = new NonBlockingHashMapLong<ManagedObject<T>>();
	/** The index of managed objects, keyed by the managed object */
	final NonBlockingHashMap<Object, ManagedObject<T>> objectsByTargetObject = new NonBlockingHashMap<Object, ManagedObject<T>>();
	
	/** All of the extracted managed object's MBean attribute method handles */
	final NonBlockingHashMapLong<MethodHandle[]> globalAttrInvokers = new NonBlockingHashMapLong<MethodHandle[]>();
	/** All of the extracted managed object's MBean operation method handles */
	final NonBlockingHashMapLong<MethodHandle> globalOpInvokers = new NonBlockingHashMapLong<MethodHandle>();
	/** All of the invocation target objects */
	final NonBlockingHashMapLong<Object> globalTargetObjects = new NonBlockingHashMapLong<Object>();

	
	/** The id of the owning object */
	final long ownerId;
	
	/**
	 * Returns the id of the owning object
	 * @return the ownerId
	 */
	public long getOwnerId() {
		return ownerId;
	}


	/** empty long arr const */
	private static final long[] EMPTY_ID_ARR = {};
	
	/**
	 * Creates a new ManagedObjectRepo
	 * @param id The id of the owning object
	 */
	public ManagedObjectRepo(long id) {
		this.ownerId = id;
	}
	
	private long lhc(String name) {
		if(name==null || name.trim().isEmpty()) return ownerId;
		return StringHelper.longHashCode(name.trim());
	}
	
	private long nhc(String name) {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("Passed name was null or empty");
		return StringHelper.longHashCode(name.trim());
	}

	
	/**
	 * Indexes the owner object and returns the ManagedObject generated
	 * @param objectToAdd The object to add
	 * @return the generated MBeanInfo for the managed object
	 */
	public MBeanInfo put(T objectToAdd) {
		return put(objectToAdd, null);
	}

	
	/**
	 * Indexes the passed object and returns the ManagedObject generated
	 * @param objectToAdd The object to add
	 * @param name The logical name of the object 
	 * @return the generated MBeanInfo for the managed object
	 */
	public MBeanInfo put(T objectToAdd, String name) {
		final long id = lhc(name);
		nvl(objectToAdd, "Managed Object");
		ManagedObject<T> mo = objectsByNameId.get(id);
		if(mo==null) {
			synchronized(objectsByNameId) {
				mo = objectsByNameId.get(id);
				if(mo==null) {
					mo = new ManagedObject<T>(objectToAdd, name==null ? ("" + ownerId) : name);
					objectsByNameId.put(id, mo);
					objectsByTargetObject.put(objectToAdd, mo);		
				}
			}
		}
		return mo.info;
	}
	
	/**
	 * Returns the getter method handle for the passed attribute name
	 * @param attributeName The attribute name
	 * @return the method handle or null if one was not found
	 * @throws AttributeNotFoundException Thrown when the attribute name is not recognized
	 * @throws MBeanException Thrown when attribute is not readable
	 */
	public MethodHandle getAttributeGetter(String attributeName) throws AttributeNotFoundException, MBeanException {
		final long id = nhc(attributeName);
		MethodHandle[] pair = globalAttrInvokers.get(id);
		if(pair==null) throw new AttributeNotFoundException("No attribute found for [" + attributeName + "]");
		if(pair.length==0 || pair[0]==null) throw new MBeanException(new Exception(), "The attribute [" + attributeName + "] is not readable");
		return pair[0].bindTo(globalTargetObjects.get(id));
	}
	
	/**
	 * Returns the setter method handle for the passed attribute name
	 * @param attributeName The attribute name
	 * @return the method handle or null if one was not found
	 * @throws AttributeNotFoundException Thrown when the attribute name is not recognized
	 * @throws MBeanException Thrown when attribute is not writable
	 */
	public MethodHandle getAttributeSetter(String attributeName) throws AttributeNotFoundException, MBeanException {
		final long id = nhc(attributeName);
		MethodHandle[] pair = globalAttrInvokers.get(id);
		if(pair==null) throw new AttributeNotFoundException("No attribute found for [" + attributeName + "]");
		if(pair.length<2 || pair[1]==null) throw new MBeanException(new Exception(), "The attribute [" + attributeName + "] is not writable");
		return pair[1].bindTo(globalTargetObjects.get(id));
	}
	
	/**
	 * Returns the method handle for the passed operation name
	 * @param opCode The hashed operation name and signature
	 * @return the method handle or null if one was not found
	 * @throws MBeanException thrown when the operation is not recognized
	 */
	public MethodHandle getOperationHandle(String opName, String[] signature) throws MBeanException {	
		final long opCode = opCode(opName, signature);
		MethodHandle mh = globalOpInvokers.get(opCode).bindTo(globalTargetObjects.get(opCode));
		if(mh==null) {
			throw new MBeanException(new Exception(), "Failed to locate operation with name [" + opName + "] and signature " + Arrays.toString(signature));
		}
		return mh;
	}
	
	/**
	 * Computes the op code for the operation name and signature
	 * @param opName The operation action name
	 * @param sig The operation signature
	 * @return the op code
	 */
	private long opCode(String opName, String...sig) {
		return StringHelper.longHashCode(opName + Arrays.deepToString(sig));
	}
	
	
// ================================================================================================================================
// ================================================================================================================================
	
	/**
	 * Retrieves a managed object by name
	 * @param name The name of the managed object
	 * @return the named managed object or null if one was not found
	 */
	public ManagedObject<T> get(String name) {
		return objectsByNameId.get(lhc(name));
	}
	
	/**
	 * Retrieves a managed object by the original added object
	 * @param addedObject The original added object
	 * @return the named managed object or null if one was not found
	 */
	public ManagedObject<T> get(T addedObject, Class<? extends T> type) {
		return objectsByTargetObject.get(addedObject);
	}
	
	/**
	 * Removes a managed object by logical name
	 * @param name The logical name of the managed object
	 * @return the removed managed object or null if one was not found
	 */
	public ManagedObject<T> remove(String name) {
		ManagedObject<T> mo = objectsByNameId.remove(lhc(name));
		if(mo!=null) {
			clearEntries(mo);
			objectsByTargetObject.remove(mo.managedObject);
		}
		return mo;		
	}
	
	/**
	 * Clears all the entries for the passed managed object
	 * @param mo the managed object to clear
	 */
	private void clearEntries(ManagedObject<T> mo) {
		mo.clearme();
	}
	
	public MBeanInfo mergeAllMBeanInfos() {
		MBeanInfoMerger merger = Reflector.newMerger(objectsByNameId.get(ownerId).info);
		String owner = "" + ownerId;
		for(ManagedObject<T> mo: objectsByNameId.values()) {
			if(mo.name.equals(owner)) continue;
			merger.append(mo.info);
		}		
		return merger.merge();
	}
	
	/**
	 * Removed a managed object by the original managed object
	 * @param addedObject the original managed object
	 * @return the removed managed object or null if one was not found
	 */
	public ManagedObject<T> remove(T addedObject) {
		ManagedObject<T> mo = objectsByTargetObject.remove(addedObject);
		if(mo!=null) {
			clearEntries(mo);
			objectsByNameId.remove(lhc(mo.name));
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
	@SuppressWarnings("hiding")
	public class ManagedObject<T> {
		/** The managed object */
		protected T managedObject;		
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
		private ManagedObject(T objectToAdd, String name) {
			managedObject = objectToAdd;
			this.name = name;	
			Class<?> clazz = managedObject.getClass();
			info = from(clazz, attrInvokers, opInvokers, attrInvokers);					
			globalAttrInvokers.putAll(attrInvokers);
			globalOpInvokers.putAll(opInvokers);
			for(long id: getAttributeIds()) { globalTargetObjects.put(id, objectToAdd); }
			for(long id: getOperationIds()) { globalTargetObjects.put(id, objectToAdd); }
		}
		
		
		/**
		 * Removes this managed objects indexes from global 
		 */
		private void clearme() {
			for(long id: getAttributeIds()) { globalAttrInvokers.remove(id); globalTargetObjects.remove(id); }
			for(long id: getOperationIds()) { globalOpInvokers.remove(id); globalTargetObjects.remove(id); }
		}
		
		/**
		 * Returns an array of the attribute invokers
		 * @return an array of the attribute invokers
		 */
		public long[] getAttributeIds() {
			if(attrInvokers.isEmpty()) return EMPTY_ID_ARR;
			return getIds(attrInvokers);
		}
		
		/**
		 * Returns an array of the op invokers
		 * @return an array of the op invokers
		 */
		public long[] getOperationIds() {
			if(opInvokers.isEmpty()) return EMPTY_ID_ARR;
			return getIds(opInvokers);
		}
		
		
		/**
		 * Retrieves the keys of the passed NonBlockingHashMapLong as a long array
		 * @param nbhm The NonBlockingHashMapLong to get the keys for
		 * @return the array of keys
		 */
		@SuppressWarnings("rawtypes")
		private long[] getIds(NonBlockingHashMapLong<?> nbhm) {
			long[] arr = new long[nbhm.size()];
			IteratorLong il = (IteratorLong)nbhm.keySet().iterator();
			int cnt = 0;
			while(il.hasNext()) {
				arr[cnt] = il.nextLong();
				cnt++;
			}
			return arr;
		}

	}

}
