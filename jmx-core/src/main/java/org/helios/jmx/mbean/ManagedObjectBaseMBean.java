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
package org.helios.jmx.mbean;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.ReflectionException;

import org.helios.jmx.annotation.ManagedNotification;
import org.helios.jmx.annotation.ManagedOperation;
import org.helios.jmx.annotation.ManagedResource;
import org.helios.jmx.annotation.Reflector;
import org.helios.jmx.managed.ManagedObjectRepo;
import org.helios.jmx.util.helpers.JMXHelper;

/**
 * <p>Title: ManagedObjectBaseMBean</p>
 * <p>Description: An extension of the {@link BaseMBean} that supports the addition and removal of managed sub-objects at runtime.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.mbean.ManagedObjectBaseMBean</code></p>
 */
@ManagedResource
public class ManagedObjectBaseMBean extends BaseMBean implements PopOperationsMBean {
	/** The managed object repository for managing the dynamic invocation and meta-data aspects of this instance */
	protected final ManagedObjectRepo<Object> managedObjects = new ManagedObjectRepo<Object>(System.identityHashCode(this));
	
	/** Flag indicating if resources have been released on unregister */
	protected final AtomicBoolean resourcesReleased = new AtomicBoolean(true);
	/** Serial number generator for added objects without logical names */
	protected final AtomicLong addedObjectSerial = new AtomicLong(0);
	
	/** An empty arg array const */
	private static final Object[] EMPTY_ARGS = {};
	/** An empty attribute list const */
	private static final AttributeList EMPTY_ATTR_LIST = new AttributeList(0); 

	
	/**
	 * Creates a new ManagedObjectBaseMBean
	 * @param implementation The implementation of the mbean interface wrapped under this mbean
	 * @param mbeanInterface The default MBean interface that this bean will expose
	 * @param isMXBean true if the published MBean will be a compliant MXBean, false otherwise
	 */
	public <T> ManagedObjectBaseMBean(T implementation, Class<T> mbeanInterface, boolean isMXBean) {
		super(implementation, mbeanInterface, isMXBean);
	}

	/**
	 * Creates a new ManagedObjectBaseMBean
	 * @param mbeanInterface The default MBean interface that this bean will expose
	 * @throws NotCompliantMBeanException
	 */
	public ManagedObjectBaseMBean(Class<?> mbeanInterface) throws NotCompliantMBeanException {
		super(mbeanInterface);		
	}

	/**
	 * Creates a new ManagedObjectBaseMBean
	 * @param implementation The implementation of the mbean interface wrapped under this mbean
	 * @param mbeanInterface The default MBean interface that this bean will expose
	 * @throws NotCompliantMBeanException
	 */
	public <T> ManagedObjectBaseMBean(T implementation, Class<T> mbeanInterface) throws NotCompliantMBeanException {
		super(implementation, mbeanInterface);
	}

	/**
	 * Creates a new ManagedObjectBaseMBean
	 * @param mbeanInterface The default MBean interface that this bean will expose
	 * @param isMXBean  true if the published MBean will be a compliant MXBean, false otherwise
	 */
	public ManagedObjectBaseMBean(Class<?> mbeanInterface, boolean isMXBean) {
		super(mbeanInterface, isMXBean);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.mbean.BaseMBean#initialize()
	 */
	@Override
	protected MBeanInfo initialize() {
		cacheMBeanInfo(managedObjects.put(this));
		fireMBeanInfoChanged();
		return getMBeanInfo();
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.StandardMBean#getAttribute(java.lang.String)
	 */
	@Override
	public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
		try {
			return managedObjects.getAttributeGetter(attribute).invoke();
		} catch (Throwable e) {
			throw new ReflectionException(new Exception(e), "Failed to dynInvoke for attribute [" + attribute + "]");				
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.StandardMBean#getAttributes(java.lang.String[])
	 */
	@Override
	public AttributeList getAttributes(String[] attributes) {
		if(attributes==null || attributes.length==0) return EMPTY_ATTR_LIST;
		AttributeList attrList = new AttributeList(attributes.length);
		for(String s: attributes) {
			try {
				attrList.add(new Attribute(s, managedObjects.getAttributeGetter(s).invoke()));
			} catch (Exception x) {/* No Op */}
		}
		return attrList;
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.StandardMBean#setAttribute(javax.management.Attribute)
	 */
	@Override
	public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
		try {			
			managedObjects.getAttributeSetter(attribute.getName()).invoke(attribute.getValue());
		} catch (Throwable e) {
			throw new ReflectionException(new Exception(e), "Failed to dynInvoke for attribute [" + attribute + "]");				
		}			
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.StandardMBean#setAttributes(javax.management.AttributeList)
	 */
	@Override
	public AttributeList setAttributes(AttributeList attributes) {
		if(attributes==null || attributes.isEmpty()) return EMPTY_ATTR_LIST;
		AttributeList newlist = new AttributeList(attributes.size()); 
		for(Attribute attr: attributes.asList()) {
			try {
				setAttribute(attr);
				String attrName = attr.getName();				
				newlist.add(new Attribute(attrName, getAttribute(attrName)));
			} catch (Exception ex) {
				/* No Op ? */
			}
		}
		return newlist;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.StandardMBean#invoke(java.lang.String, java.lang.Object[], java.lang.String[])
	 */
	@Override
	public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
		try {
			return managedObjects.getOperationHandle(actionName, signature).invoke(params);
		} catch (Throwable e) {
			e.printStackTrace(System.err);
			throw new ReflectionException(new Exception(e), "Failed to dynInvoke for operation [" + actionName + "]");				
		}			
	}
	
	/**
	 * Adds a new managed object
	 * @param managedObject The managed object to add
	 * @param name The logical name of the managed object
	 */
	public void addManagedObject(Object managedObject, String name) {
		managedObjects.put(managedObject, name);
	}
	
	/**
	 * Adds a new anonymous managed object
	 * @param managedObject the managed object to add
	 * @return the generated serial number for the added object,
	 * which can be used to remove the object
	 */
	public long addManagedObject(Object managedObject) {
		final long id = addedObjectSerial.incrementAndGet();
		managedObjects.put(managedObject, id);
		return id;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.mbean.PopOperationsMBean#pop(java.lang.String)
	 */
	@Override
	public boolean pop(String name) throws AttributeNotFoundException {
		return pop(name, false);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.mbean.PopOperationsMBean#pop(java.lang.String, boolean)
	 */
	@Override
	public boolean pop(String name, boolean bounce) throws AttributeNotFoundException {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed name was null or empty");
		boolean result = false;
		synchronized(managedObjects) {
			if(!managedObjects.hasNamedObject(name)) throw new AttributeNotFoundException("The attribute [" + name + "] was not found");
			MBeanInfo info = managedObjects.pop(name);
			if(info!=null) {
				cacheMBeanInfo(Reflector.newMerger(getCachedMBeanInfo()).append(info).merge());
				fireMBeanInfoChanged();					
				result = true;
			}
			result = false;
		}
		if(result && bounce) bounce();
		return result;
	}
		
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.mbean.PopOperationsMBean#unpop(java.lang.String, boolean)
	 */
	@Override
	public boolean unpop(String name, boolean bounce) throws AttributeNotFoundException {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed name was null or empty");
		boolean result = false;
		synchronized(managedObjects) {
			if(!managedObjects.hasNamedObject(name)) throw new AttributeNotFoundException("The attribute [" + name + "] was not found");
			result =  managedObjects.unpop(name);
			if(result) {
				cacheMBeanInfo(managedObjects.mergeAllMBeanInfos());
				fireMBeanInfoChanged();										
			}
		}
		if(bounce) bounce();
		return result;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.mbean.PopOperationsMBean#unpop(java.lang.String)
	 */
	@Override
	public boolean unpop(String name) throws AttributeNotFoundException {
		return unpop(name, false);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.mbean.PopOperationsMBean#popAll()
	 */
	@Override
	@ManagedOperation(name="popAll", description="Pops all the poppable attributes", 
			notifications={ @ManagedNotification(notificationTypes={"jmx.mbean.info.changed"}, name="MBeanInfoChanged", description="Notification emitted when the MBeanInfo is updated") }				
	)					
	public int popAll() {
		return popAll(false);
	}

	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.mbean.PopOperationsMBean#popAll(boolean)
	 */
	@Override
	public int popAll(boolean bounce) {
		MBeanInfo[] poppedInfos = null;
		synchronized(managedObjects) {
			poppedInfos = managedObjects.popAll();
			if(poppedInfos!=null && poppedInfos.length>0) {				
				MBeanInfo merged = Reflector.newMerger(getCachedMBeanInfo()).append(poppedInfos).merge();
				cacheMBeanInfo(merged);
				fireMBeanInfoChanged();					
			}
		}
		if(bounce) bounce();
		return poppedInfos==null ? 0 : poppedInfos.length;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.mbean.PopOperationsMBean#unPopAll()
	 */
	@Override
	public int unPopAll() {
		return unPopAll(false);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.mbean.PopOperationsMBean#unPopAll(boolean)
	 */
	@Override
	public int unPopAll(boolean bounce) {
		int unpopped =  0;
		synchronized(managedObjects) {
			unpopped =  managedObjects.unPopAll();
			cacheMBeanInfo(managedObjects.mergeAllMBeanInfos());
			fireMBeanInfoChanged();										
			
		}
		if(bounce) bounce();
		return unpopped;
	}	
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.mbean.PopOperationsMBean#bounce()
	 */
	@Override
	public void bounce() {
		if(resourcesReleased.compareAndSet(false, true)) {
			try { 
				JMXHelper.unregisterMBean(objectName);
				JMXHelper.registerMBean(getRegistrationObject(), objectName);			
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			} finally {
				resourcesReleased.compareAndSet(true, false);
			}
		}
	}
	
	/**
	 * Returns the object to be registered/unregistered.
	 * In the base class, returns <b><code>this</code></b> but subclasses 
	 * such as proxied mbeans may prescribe a different object here.
	 * @return the object to be registered/unregistered.
	 */
	protected Object getRegistrationObject() {
		return this;
	}
	

	
	/**
	 * Clears all managed objects in the repo and de-allocates any other managed object resources
	 */
	protected void clear() {
		managedObjects.clear();		
	}

	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.mbean.BaseMBean#preDeregister()
	 */
	@Override
	public void preDeregister() throws Exception {
		if(resourcesReleased.compareAndSet(false, true)) {
			clear();
		}
		super.preDeregister();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.mbean.BaseMBean#postDeregister()
	 */
	@Override
	public void postDeregister() {
		if(resourcesReleased.compareAndSet(false, true)) {
			clear();
		}
		super.postDeregister();
	}
	
	
	/*
	 * pop/unpop
	 * popAll/unPopAll
	 * regUnreg / unload on unregister (true/false)
	 * add managed object / remove managed object
	 * =====================================================
	 * ProxiedMBean
	 * ProxiedManagedObjectMBean
	 * Proxied interface
	 * =====================================================
	 * Embedded versus Extended
	 * =====================================================
	 * Builder
	 * =====================================================
	 * ManagedAttributes:
	 * 		key
	 * 		sendAttrChgNotifs
	 * 
	 */
	

}
