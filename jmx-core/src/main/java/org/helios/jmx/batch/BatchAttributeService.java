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
package org.helios.jmx.batch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.helios.jmx.concurrency.JMXManagedThreadPool;
import org.helios.jmx.util.helpers.JMXHelper;

/**
 * <p>Title: BatchAttributeService</p>
 * <p>Description: A service to retrieve multiple attributes from multiple MBeans in a single batch call</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.batch.BatchAttributeService</code></p>
 */

public class BatchAttributeService extends NotificationBroadcasterSupport implements NotificationListener, NotificationFilter {
	/** The singleton instance */
	private static volatile BatchAttributeService instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The notification broadcaster thread pool */
	private final JMXManagedThreadPool threadPool;
	
	/** This services's JMX ObjectName */
	private final ObjectName objectName = JMXHelper.objectName(getClass());
	
    /** A map of numeric typed composite fields in composite types, keyed by the ObjectName/Attribute Name */
    private final Map<ObjectName, Map<String, String[]>> compositeNumerics = new NonBlockingHashMap<ObjectName, Map<String, String[]>>();
    /** A map of MBean attribute names that are either numeric or compsites containing numerics keyed by the ObjectName */
    private final Map<ObjectName, String[]> allNumerics = new NonBlockingHashMap<ObjectName, String[]>();
    
	
	/** The bean's notification MBeanInfos */
	private static final MBeanNotificationInfo[] MBEAN_INFO = new  MBeanNotificationInfo[] {
		
	};
	/** Factory for notification IDs */
	private final AtomicLong notificationIdFactory = new AtomicLong(0L);
	
	/** The registered listeners */
	private final Set<ListenerInfo> listeners = new CopyOnWriteArraySet<ListenerInfo>();
	
	/** The JMX ObjectNames of known batch attribute providers */
	private final Set<ObjectName> providers = new CopyOnWriteArraySet<ObjectName>();
	
	 
	/**
	 * Returns the BatchAttributeService singleton instance
	 * @return the BatchAttributeService singleton instance
	 */
	public static BatchAttributeService getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					
					instance = new BatchAttributeService(new JMXManagedThreadPool(JMXHelper.objectName(BatchAttributeService.class), BatchAttributeService.class.getSimpleName()), MBEAN_INFO);
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new BatchAttributeService
	 */
	private BatchAttributeService(JMXManagedThreadPool threadPool, MBeanNotificationInfo[] ninfos) {
		super(threadPool, ninfos);
		this.threadPool = threadPool;
		JMXHelper.registerMBean(this, objectName);
		JMXHelper.addNotificationListener(MBeanServerDelegate.DELEGATE_NAME, this, this, null);
	}
	
	public Map<ObjectName, Map<String, Object>> batchGetAttributes(Map<ObjectName, Map<String, String>> criteria) {
		Map<ObjectName, Map<String, Object>> map = new HashMap<ObjectName, Map<String, Object>>();
		
		return map;
	}
	
	/**
	 * Returns a map of arrays of composite type keys that represent numeric data keyed by the MBean's attribute name
	 * @param objectName The object name to get the map for
	 * @return the map of numeric keys
	 */
	protected Map<String, String[]> getCompositeNumerics(ObjectName objectName) {
		Map<String, String[]> map = compositeNumerics.get(objectName);
		if(map==null) {
			synchronized(compositeNumerics) {
				map = compositeNumerics.get(objectName);
				if(map==null) {
					map = new HashMap<String, String[]>();
					MBeanInfo infos = JMXHelper.getMBeanInfo(objectName);
					for(MBeanAttributeInfo info: infos.getAttributes()) {
						Set<String> numericKeys = new HashSet<String>();
						try {                                          
							if(CompositeData.class.isAssignableFrom(Class.forName(info.getType()))) {
								CompositeData cd = (CompositeData)JMXHelper.getAttribute(objectName, info.getName());
								CompositeType ct = cd.getCompositeType();
								for(String key: ct.keySet()) {
									OpenType<?> ot = ct.getType(key);
									try {
										if(Number.class.isAssignableFrom(Class.forName(ot.getClassName()))) {
											numericKeys.add(key);
										}
									} catch (Exception x) { /* No Op */ }
								}
								map.put(info.getName(), numericKeys.toArray(new String[numericKeys.size()]));
							}
						} catch (Exception x) { /* No Op */ }
					}
				}
				compositeNumerics.put(objectName, map);
			}
		}
		return map;
	}

	/**
	 * Returns an array of attribute names of attributes with numeric types, or composite types containing numeric types
	 * @param objectName The MBean's ObjectName
	 * @return the array of numeric attribute names
	 */
	 protected String[] getAllNumerics(ObjectName objectName) {
		String[] attrNames = allNumerics.get(objectName);
		if(attrNames==null) {
			synchronized(allNumerics) {
				attrNames = allNumerics.get(objectName);
				if(attrNames==null) {
					Set<String> numericKeys = new HashSet<String>();
					MBeanInfo infos = JMXHelper.getMBeanInfo(objectName);
					for(MBeanAttributeInfo info: infos.getAttributes()) {
						try {
							Class<?> clazz = Class.forName(info.getType());
							if(Number.class.isAssignableFrom(clazz)) {
								numericKeys.add(info.getName());
							} else if(CompositeData.class.isAssignableFrom(clazz)) {
								CompositeData cd = (CompositeData)JMXHelper.getAttribute(objectName, info.getName());
								CompositeType ct = cd.getCompositeType();
								for(String key: ct.keySet()) {
									OpenType<?> ot = ct.getType(key);
									try {
										if(Number.class.isAssignableFrom(Class.forName(ot.getClassName()))) {
											numericKeys.add(info.getName() + "/" + key);
										}
									} catch (Exception x) { /* No Op */ }
								}                                                     
							}
						} catch (Exception x) { /* No Op */ }
					}
					attrNames = numericKeys.toArray(new String[numericKeys.size()]);
				}
			}
		}
		return attrNames;
	 }
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcasterSupport#addNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
		super.addNotificationListener(listener, filter, handback);
		listeners.add(new ListenerInfo(listener, filter, handback));
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcasterSupport#removeNotificationListener(javax.management.NotificationListener)
	 */
	@Override
	public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
		listeners.remove(new WildcardListenerInfo(listener));
		super.removeNotificationListener(listener);		
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcasterSupport#removeNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException {
		listeners.remove(new ListenerInfo(listener, filter, handback));
		super.removeNotificationListener(listener, filter, handback);
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
	 */
	@Override
	public void handleNotification(Notification notification, Object handback) {
		MBeanServerNotification msn = (MBeanServerNotification)notification;
		if(msn.getType().equals(MBeanServerNotification.REGISTRATION_NOTIFICATION)) {
			
		} else if(msn.getType().equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION)) {
			
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationFilter#isNotificationEnabled(javax.management.Notification)
	 */
	@Override
	public boolean isNotificationEnabled(Notification notification) {
		return notification!=null && (notification instanceof MBeanServerNotification);
	}
	
	
    /**
     * <p>Title: ListenerInfo</p>
     * <p>Description: A wrapper for varied listeners. Copied from {@link NotificationBroadcasterSupport}</p> 
     * <p>Company: Helios Development Group LLC</p>
     * @author Whitehead (nwhitehead AT heliosdev DOT org)
     * <p><code>org.helios.jmx.batch.BatchAttributeService.ListenerInfo</code></p>
     */
    public static class ListenerInfo {
        NotificationListener listener;
        NotificationFilter filter;
        Object handback;

        /**
         * Creates a new ListenerInfo
         * @param listener
         * @param filter
         * @param handback
         */
        ListenerInfo(NotificationListener listener,
                     NotificationFilter filter,
                     Object handback) {
            this.listener = listener;
            this.filter = filter;
            this.handback = handback;
        }

        public boolean equals(Object o) {
            if (!(o instanceof ListenerInfo))
                return false;
            ListenerInfo li = (ListenerInfo) o;
            if (li instanceof WildcardListenerInfo)
                return (li.listener == listener);
            else
                return (li.listener == listener && li.filter == filter
                        && li.handback == handback);
        }
    }

    /**
     * <p>Title: WildcardListenerInfo</p>
     * <p>Description:  A wrapper for varied listeners. Copied from {@link NotificationBroadcasterSupport}</p> 
     * <p>Company: Helios Development Group LLC</p>
     * @author Whitehead (nwhitehead AT heliosdev DOT org)
     * <p><code>org.helios.jmx.batch.BatchAttributeService.WildcardListenerInfo</code></p>
     */
    public static class WildcardListenerInfo extends ListenerInfo {
        /**
         * Creates a new WildcardListenerInfo
         * @param listener
         */
        WildcardListenerInfo(NotificationListener listener) {
            super(listener, null, null);
        }

        public boolean equals(Object o) {
            assert (!(o instanceof WildcardListenerInfo));
            return o.equals(this);
        }
    }

	

}
