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
package org.helios.jmx.mbean;

import java.io.ObjectStreamException;
import java.io.Serializable;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.helios.jmx.annotation.Reflector;
import org.helios.jmx.mbean.NotificationBroadcastService.NotificationEmitterSupport;

/**
 * <p>Title: BaseMBean</p>
 * <p>Description: Base instrumented MBean implementation</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.mbean.BaseMBean</code></p>
 */

public class BaseMBean extends StandardMBean implements NotificationEmitter, Serializable {
	/**  */
	private static final long serialVersionUID = -6899194134934874651L;
	/** The MBean's notification emitter */
	protected final NotificationEmitterSupport emitter;
	/** The MBean's designated JMX ObjectName, assigned at registration time */
	protected ObjectName objectName;
	/** The MBeanServer where this MBean was registered, assigned at registration time */
	protected MBeanServer server;
	
	/**
	 * Creates a new BaseMBean
	 * @param implementation The implementation of the mbean interface wrapped under this mbean
	 * @param mbeanInterface The primary MBean interface that this bean will expose
	 * @param isMXBean true if the published MBean will be a compliant MXBean, false otherwise
	 */
	public <T> BaseMBean(T implementation, Class<T> mbeanInterface, boolean isMXBean) {
		super(implementation, mbeanInterface, isMXBean);
		emitter = NotificationBroadcastService.getInstance().newNotificationEmitter(initialize());
	}

	/**
	 * Creates a new BaseMBean
	 * @param mbeanInterface The primary MBean interface that this bean will expose
	 * @throws NotCompliantMBeanException
	 */
	public BaseMBean(Class<?> mbeanInterface) throws NotCompliantMBeanException {
		super(mbeanInterface);
		emitter = NotificationBroadcastService.getInstance().newNotificationEmitter(initialize());
	}

	/**
	 * Creates a new BaseMBean
	 * @param implementation The implementation of the mbean interface wrapped under this mbean
	 * @param mbeanInterface The primary MBean interface that this bean will expose
	 * @throws NotCompliantMBeanException
	 */
	public <T> BaseMBean(T implementation, Class<T> mbeanInterface) throws NotCompliantMBeanException {
		super(implementation, mbeanInterface);
		emitter = NotificationBroadcastService.getInstance().newNotificationEmitter(initialize());
	}

	/**
	 * Creates a new BaseMBean
	 * @param mbeanInterface The primary MBean interface that this bean will expose
	 * @param isMXBean true if the published MBean will be a compliant MXBean, false otherwise
	 */
	public BaseMBean(Class<?> mbeanInterface, boolean isMXBean) {
		super(mbeanInterface, isMXBean);
		emitter = NotificationBroadcastService.getInstance().newNotificationEmitter(initialize());
	}
	
	/**
	 * Initializes the MBean's meta-data
	 * @return the determined initial MBeanInfo
	 */
	protected MBeanInfo initialize() {
		MBeanInfo info = Reflector.newMerger(getCachedMBeanInfo())
				.append(Reflector.from(getMBeanInterface(), null, null, null))
				.append(new MBeanNotificationInfo(new String[]{"jmx.mbean.info.changed"}, Notification.class.getName(), "Notification broadcast when an MBean's MBeanInfo has changed"))
				.merge();
		cacheMBeanInfo(info);
		fireMBeanInfoChanged();
		return info;
	}
	
	/**
	 * Replaces this object with the ObjectName if it has been assigned, otherwise returns a descriptive string.
	 * @return the serialization output stream replacement for this object
	 * @throws ObjectStreamException
	 */
	Object writeReplace() throws ObjectStreamException {
		return objectName==null ? "Unregistered " + getClass().getName() + " Instance" : objectName;
	}
	
	/**
	 * Returns this MBean's registration JMX ObjectName
	 * @return this MBean's registration JMX ObjectName
	 */
	public ObjectName getObjectName() {
		return objectName;
	}

	/**
	 * Broadcasts an MBeanInfo changed notification 
	 */
	protected void fireMBeanInfoChanged() {
		emitter.sendNotificationAsync("jmx.mbean.info.changed", this, "MBeanInfo Udate", getCachedMBeanInfo());
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#addNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws IllegalArgumentException {
		emitter.addNotificationListener(listener, filter, handback);		
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#removeNotificationListener(javax.management.NotificationListener)
	 */
	@Override
	public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
		emitter.removeNotificationListener(listener);		
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#getNotificationInfo()
	 */
	@Override
	public MBeanNotificationInfo[] getNotificationInfo() {
		return getCachedMBeanInfo().getNotifications();
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationEmitter#removeNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException {
		emitter.addNotificationListener(listener, filter, handback);		
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.StandardMBean#preRegister(javax.management.MBeanServer, javax.management.ObjectName)
	 */
	@Override
	public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
		this.server = server;
		this.objectName = name;
		return super.preRegister(server, name);
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.StandardMBean#postRegister(java.lang.Boolean)
	 */
	@Override
	public void postRegister(Boolean registrationDone) {
		super.postRegister(registrationDone);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.StandardMBean#preDeregister()
	 */
	@Override
	public void preDeregister() throws Exception {
		emitter.clearAllListeners();
		super.preDeregister();
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.StandardMBean#postDeregister()
	 */
	@Override
	public void postDeregister() {
		super.postDeregister();
	}
}
