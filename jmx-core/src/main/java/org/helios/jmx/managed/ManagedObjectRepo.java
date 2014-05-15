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

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.cliffc.high_scale_lib.NonBlockingHashMapLong;
import org.cliffc.high_scale_lib.NonBlockingHashMapLong.IteratorLong;
import org.helios.jmx.annotation.Popable;
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
	final NonBlockingHashMapLong<Invoker[]> globalAttrInvokers = new NonBlockingHashMapLong<Invoker[]>();
	/** All of the extracted managed object's MBean operation method handles */
	final NonBlockingHashMapLong<Invoker> globalOpInvokers = new NonBlockingHashMapLong<Invoker>();
	/** All of the invocation target objects */
	final NonBlockingHashMapLong<Object> globalTargetObjects = new NonBlockingHashMapLong<Object>();

	
	/** The id of the owning object */
	final long ownerId;

	/**
	 * Clears the repo 
	 */
	public void clear() {
		Set<ManagedObject> tmp = new HashSet<ManagedObject>(objectsByTargetObject.values());
		objectsByTargetObject.clear();
		for(ManagedObject mo: tmp) {
			mo.clear();
		}
		objectsByNameId.clear();
		objectsByTargetObject.clear();
		globalAttrInvokers.clear();
		globalOpInvokers.clear();
		globalTargetObjects.clear();
	}

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
	 * Pops the named attribute
	 * @param name The name of the attribute
	 * @return the MBeanInfo of the popped attribute, or null  
	 */
	@SuppressWarnings("unchecked")
	public MBeanInfo pop(String name) {		
		long hash = StringHelper.longHashCode(nvl(name, "Attribute Name"));
		Invoker[] invokerPair = globalAttrInvokers.get(hash);
		if(invokerPair==null || invokerPair.length==0 || invokerPair[0]==null) return null;
		if(!invokerPair[0].getClass().getAnnotation(Popable.class).value()) {
			return null;
		}
		Object target = invokerPair[0].invoke();
		return put((T) target, name);
	}
	
	/**
	 * Pops all the managed objects that have not been popped
	 * @return an array of mbean infos for the objects that were popped
	 */
	@SuppressWarnings("unchecked")
	public MBeanInfo[] popAll() {
		Set<MBeanInfo> infos = new LinkedHashSet<MBeanInfo>();
		for(Invoker[] invokerPair: globalAttrInvokers.values()) {
			if(invokerPair==null || invokerPair.length==0 || invokerPair[0]==null) continue;
			boolean poppable = invokerPair[0].getClass().getAnnotation(Popable.class).value();
			if(!poppable) continue;
			Object target = invokerPair[0].invoke();
			MBeanInfo info = put((T) target, invokerPair[0].getName());
			if(info!=null) {
				infos.add(info);
			}
		}
		return infos.toArray(new MBeanInfo[infos.size()]);		
	}
	
	public int unPopAll() {
		int unpopped = 0;
		for(Invoker[] invokerPair: globalAttrInvokers.values()) {
			if(invokerPair==null || invokerPair.length==0 || invokerPair[0]==null) continue;
			boolean poppable = invokerPair[0].getClass().getAnnotation(Popable.class).value();
			if(!poppable) continue;
			String name = invokerPair[0].getName();
			if(unpop(name)) unpopped++;
		}		
		return unpopped;
	}
	
	/**
	 * Unpops the named attribute
	 * @param name The name of the attribute
	 * @return true if the attribute was successfully unpopped  
	 */
	@SuppressWarnings("unchecked")
	public boolean unpop(String name) {
		long hash = StringHelper.longHashCode(nvl(name, "Attribute Name"));
		Invoker[] invokerPair = globalAttrInvokers.get(hash);
		if(invokerPair==null || invokerPair.length==0 || invokerPair[0]==null) return false;
		Object target = invokerPair[0].invoke();
		return remove((T) target)!=null;
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
		ManagedObject<T> mo = objectsByTargetObject.get(objectToAdd);
		if(mo==null) {
			synchronized(objectsByTargetObject) {
				mo = objectsByTargetObject.get(objectToAdd);
				if(mo==null) {
					mo = new ManagedObject<T>(objectToAdd, name);
					objectsByNameId.put(id, mo);
					objectsByTargetObject.put(objectToAdd, mo);					
					return mo.info;
				}
			}
		}
		return null;
	}
	
	/**
	 * Returns the getter method handle for the passed attribute name
	 * @param attributeName The attribute name
	 * @return the method handle or null if one was not found
	 * @throws AttributeNotFoundException Thrown when the attribute name is not recognized
	 * @throws MBeanException Thrown when attribute is not readable
	 */
	public Invoker getAttributeGetter(String attributeName) throws AttributeNotFoundException, MBeanException {
		final long id = nhc(attributeName);
		Invoker[] pair = globalAttrInvokers.get(id);
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
	public Invoker getAttributeSetter(String attributeName) throws AttributeNotFoundException, MBeanException {
		final long id = nhc(attributeName);
		Invoker[] pair = globalAttrInvokers.get(id);
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
	public Invoker getOperationHandle(String opName, String[] signature) throws MBeanException {	
		final long opCode = opCode(opName, signature);
		Invoker mh = globalOpInvokers.get(opCode).bindTo(globalTargetObjects.get(opCode));
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
			return mo;
		}
		return null;		
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
	
	public static void log(String fmt, Object...args) {
		System.out.println(String.format(fmt, args));
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
		final NonBlockingHashMapLong<Invoker[]> attrInvokers = new NonBlockingHashMapLong<Invoker[]>();
		/** The extracted managed object's MBean operation method handles */
		final NonBlockingHashMapLong<Invoker> opInvokers = new NonBlockingHashMapLong<Invoker>();
		
		private void clear() {
			managedObject = null;
			name = null;
			info = null;
			attrInvokers.clear();
			opInvokers.clear();
		}
		
		/**
		 * Creates a new ManagedObject
		 * @param objectToAdd The object to manage
		 * @param name The logical name
		 */
		private ManagedObject(T objectToAdd, String name) {
			final boolean subObject = name!=null;
			if(!subObject) {
				name = "" + ownerId;
			}
			log("Adding Object ID [%s]" , System.identityHashCode(objectToAdd));
			long start = System.currentTimeMillis();
			managedObject = objectToAdd;
			this.name = name;	
			Class<?> clazz = managedObject.getClass();
			MBeanInfo _info = from(clazz, attrInvokers, opInvokers, attrInvokers);
			if(subObject) {
				info = prefixSubObjects(_info);
				remapInvokerHashCodes(_info);
			} else {
				info =  _info;
			}
			
			Reflector.bindInvokers(objectToAdd, attrInvokers, opInvokers);
			globalAttrInvokers.putAll(attrInvokers);
			globalOpInvokers.putAll(opInvokers);
			for(long id: getAttributeIds()) { globalTargetObjects.put(id, objectToAdd); }
			for(long id: getOperationIds()) { globalTargetObjects.put(id, objectToAdd); }
			long elapsed = System.currentTimeMillis() - start;
			log("Added Object ID [%s] in [%s] ms" , System.identityHashCode(objectToAdd), elapsed);
		}
		
		private MBeanInfo prefixSubObjects(MBeanInfo _info) {
			MutableMBeanAttributeInfo[] mutableAttrs = MutableMBeanAttributeInfo.from(name, _info.getAttributes());  
			MutableMBeanOperationInfo[] mutableOps = MutableMBeanOperationInfo.from(name, _info.getOperations());
			return new MBeanInfo(_info.getClassName(), _info.getDescription(), 
					MutableMBeanAttributeInfo.toImmutable(mutableAttrs), _info.getConstructors(),
					MutableMBeanOperationInfo.toImmutable(mutableOps), _info.getNotifications(),
					_info.getDescriptor()
					
			);		
		}
		
		private void remapInvokerHashCodes(MBeanInfo unremappedInfo) {
			for(MBeanAttributeInfo info: unremappedInfo.getAttributes()) {
				long oldHash = StringHelper.longHashCode(info.getName());
				long newHash = StringHelper.longHashCode(name + info.getName());
				Invoker[] invs = attrInvokers.remove(oldHash);
				if(invs!=null) attrInvokers.put(newHash, invs);
			}
			for(MBeanOperationInfo info: unremappedInfo.getOperations()) {
				long oldHash = StringHelper.longHashCode(info.getName());
				long newHash = StringHelper.longHashCode(name + info.getName());
				Invoker invs = opInvokers.remove(oldHash);
				if(invs!=null) opInvokers.put(newHash, invs);
			}			
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
