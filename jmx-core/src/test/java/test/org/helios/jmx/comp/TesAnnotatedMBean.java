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
package test.org.helios.jmx.comp;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.JMX;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.StandardMBean;
import javax.management.remote.JMXConnector;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.cliffc.high_scale_lib.NonBlockingHashMapLong;
import org.cliffc.high_scale_lib.NonBlockingHashMapLong.IteratorLong;
import org.helios.jmx.annotation.ManagedAttribute;
import org.helios.jmx.annotation.ManagedMetric;
import org.helios.jmx.annotation.ManagedNotification;
import org.helios.jmx.annotation.ManagedOperation;
import org.helios.jmx.annotation.ManagedOperationParameter;
import org.helios.jmx.annotation.ManagedResource;
import org.helios.jmx.annotation.Reflector;
import org.helios.jmx.managed.ManagedObjectRepo;
import org.helios.jmx.metrics.ewma.DirectEWMA;
import org.helios.jmx.metrics.ewma.DirectEWMAMBean;
import org.helios.jmx.util.helpers.JMXHelper;
import org.helios.jmx.util.helpers.StringHelper;
import org.helios.jmx.util.helpers.SystemClock;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import test.org.helios.jmx.BaseTest;

/**
 * <p>Title: TesAnnotatedMBean</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.jmx.comp.TesAnnotatedMBean</code></p>
 */

public class TesAnnotatedMBean extends BaseTest {
	
	@BeforeClass
	public static void startJMXServer() {
		startJmxMpServer("0.0.0.0", 8006);
	}
	
	@AfterClass
	public static void closeConnector() {
		if(connector!=null) {
			try { connector.close(); } catch (Exception x) {}
		}
		connector = null;
	}

	
	protected static JMXConnector connector = null;
	
	public static MBeanServerConnection getRemoteMBeanServer() {
		if(connector==null) connector = getConnector();
		try {
			return connector.getMBeanServerConnection();
		} catch (Exception x) {
			throw new RuntimeException(x);
		}
	}

	@Test
	public void testAnnotatedMBean() throws Exception {
		try {
			String ons = "test.org.helios.jmx.comp:service=TestMBean,type=" + name.getMethodName();
			ObjectName on = JMXHelper.objectName(ons);
			
			TestBean tb = new TestBean(TestBeanMBean.class, true);
			tb.register(on);
			MBeanServerConnection conn = getRemoteMBeanServer();
			Object obj = conn.getAttribute(on, "SystemDate");
			log("SystemDate: [%s]", obj);
			Assert.assertNotNull("The attribute value was null", obj);
			Assert.assertTrue("The attribute was not a date", obj instanceof Date);
			Date dt = new Date(System.currentTimeMillis()-100000);
			conn.setAttribute(on, new Attribute("SystemUTC", dt.getTime()));
			long utc = (Long)conn.getAttribute(on, "SystemUTC");
			Assert.assertEquals("SystemUTC was unexpected", dt.getTime(), utc);
			Date systemDate = (Date)conn.getAttribute(on, "SystemDate");
			Assert.assertEquals("SystemDate was unexpected", dt, systemDate);
			
			obj = conn.getAttribute(on, "RandomUUID");
			Assert.assertNull("The initial random uuid was not null", obj);
			obj = conn.invoke(on, "UpdateRandomUUID", new Object[]{}, new String[]{});
			Assert.assertNotNull("The updated random uuid was null", obj);
			obj = conn.getAttribute(on, "RandomUUID");
			Assert.assertNotNull("The updated random uuid as an attribute was not null", obj);
			
			String uuid = UUID.randomUUID().toString();
			log("UUID: %s", uuid);
			String[] parts = uuid.split("\\-");
			log("UUID: %s, parts: %s", uuid, parts.length);
			Object[] oparts = new Object[parts.length];
			System.arraycopy(parts, 0, oparts, 0, parts.length);
			
			Arrays.fill(parts, String.class.getName());
			obj = conn.invoke(on, "UpdateMultiRandomUUID", oparts, parts);
			
			Assert.assertEquals("UUID was unexpected", uuid, obj);
			TestBeanMBean tbm = JMX.newMBeanProxy(conn, on, TestBeanMBean.class);
			tbm.popEWMAs();
			while(true) {
				for(int i = 0; i < 1000; i++) {
					tbm.doRandomUUID();					
				}
				log(tbm.getUUIDElapsed().toString());
				tbm.reset();
				Thread.sleep(5000);
			}
			
			
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			throw ex;
		}
	}
	
	@ManagedResource(objectName="test.org.helios.jmx.comp:service=TestMBean", description="A test mbean")
	public interface TestBeanMBean {
		@ManagedAttribute(description="Returns the system date", name="SystemDate")
		public Date getDate();
		@ManagedAttribute(description="The system date as a UTC long", name="SystemUTC")
		public long getUTC();		
		@ManagedAttribute(description="The system date as a UTC long", name="SystemUTC")
		public void setUTC(long value);
		@ManagedAttribute(description="The current UUID", name="RandomUUID")
		public String getRandomUUID();
		@ManagedAttribute(description="The current UUID", name="RandomUUID")
		public void setRandomUUID(String uuid);
		@ManagedOperation(name="UpdateRandomUUID", description="Sets a new Random UUID")
		public String doRandomUUID();
		@ManagedOperation(name="UpdateMultiRandomUUID", description="Sets a new Random UUID", parameters={
				@ManagedOperationParameter(name="a"),
				@ManagedOperationParameter(name="b"),
				@ManagedOperationParameter(name="c"),
				@ManagedOperationParameter(name="d"),
				@ManagedOperationParameter(name="e")
		})
		public String doRandomUUID(String a, String b, String c, String d, String e);		
		
		@ManagedMetric(displayName="UUIDElapsed", description="Elapsed times for UUID generation", unit="ns")
		public DirectEWMAMBean getUUIDElapsed();
		@ManagedOperation(name="reset", description="Resets the metrics", notifications={
				@ManagedNotification(notificationTypes={"ewma.reset"}, name="EWMAReset", description="Notification emitted when the EWMA is reset")
		})
		public void reset();
		@ManagedOperation(name="popEWMAs", description="Promotes the EWMA fields to be first class MBean attributes", notifications={
				@ManagedNotification(notificationTypes={"jmx.mbean.info.changed"}, name="MBeanInfoChanged", description="Notification emitted when the MBeanInfo is updated")
		})
		public void popEWMAs();
		@ManagedOperation(name="unpopEWMAs", description="Removes the EWMA fields from first class MBean status", notifications={
				@ManagedNotification(notificationTypes={"jmx.mbean.info.changed"}, name="MBeanInfoChanged", description="Notification emitted when the MBeanInfo is updated")
		})		
		public void unpopEWMAs();
		
	}
	
	/**
	 * <p>Title: TestBean</p>
	 * <p>Description: </p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>test.org.helios.jmx.comp.TestBean</code></p>
	 */
	/*
	 * TODO:
	 * DescriptorBuilder
	 * Invoker
	 * Metric --> Class 
	 */
	
	
	public class TestBean extends StandardMBean implements TestBeanMBean, NotificationEmitter, Serializable {
		/**  */
		private static final long serialVersionUID = -67640604151534755L;
//		final NonBlockingHashMapLong<MethodHandle[]> attrInvokers = new NonBlockingHashMapLong<MethodHandle[]>();
//		final NonBlockingHashMapLong<MethodHandle> opInvokers = new NonBlockingHashMapLong<MethodHandle>();
//		final NonBlockingHashMapLong<Object> targetObjects = new NonBlockingHashMapLong<Object>(); 
		final ManagedObjectRepo<Object> managedObjects = new ManagedObjectRepo<Object>(System.identityHashCode(this));
		final NotificationBroadcasterSupport broadcaster;  
		public final MBeanNotificationInfo[] EMPTY_N_INFO = {};
		ObjectName objectName = null;
		protected TestBean(Class<?> mbeanInterface, boolean isMXBean) {
			super(mbeanInterface, isMXBean);
			cacheMBeanInfo(managedObjects.put(this));
//			cacheMBeanInfo(Reflector.clean(Reflector.from(mbeanInterface, attrInvokers, opInvokers, attrInvokers)));
			broadcaster = new NotificationBroadcasterSupport(this.getMBeanInfo().getNotifications());
//			targetObjects.putAll(Reflector.invokerTargetMap(this, attrInvokers, opInvokers));
//			Reflector.bindInvokers(this, attrInvokers, opInvokers);
			
		}		
		final AtomicLong notifSerial = new AtomicLong(0L);
		
		
		
		final DirectEWMAMBean uuidElapsed = new DirectEWMA(100);
		
		public void popEWMAs() {
			MBeanInfo info = managedObjects.put(uuidElapsed, "UUIDElapsed");		
			synchronized(managedObjects) {
				cacheMBeanInfo(Reflector.newMerger(getCachedMBeanInfo()).append(info).merge());
				sendNotification(new Notification("jmx.mbean.info.changed", this, notifSerial.incrementAndGet()));
			}
		}
		
		public void unpopEWMAs() {
			managedObjects.remove("UUIDElapsed");
			synchronized(managedObjects) {
				cacheMBeanInfo(managedObjects.mergeAllMBeanInfos());
				sendNotification(new Notification("jmx.mbean.info.changed", this, notifSerial.incrementAndGet()));
			}
			
		}
		
/*		protected void addManagedSubObject(Object obj, Class<?> mbeanInterface) {
			if(obj==null) return;
			final NonBlockingHashMapLong<MethodHandle[]> attrs = new NonBlockingHashMapLong<MethodHandle[]>();
			final NonBlockingHashMapLong<MethodHandle> ops = new NonBlockingHashMapLong<MethodHandle>();
//			Reflector.bindInvokers(obj, attrs, ops);
			
			MBeanInfo info = Reflector.clean(Reflector.from(mbeanInterface, attrs, ops, attrs));
			addedObjectMetas.put(obj, new AddedObjectMeta(attrs, ops, info));
			attrInvokers.putAll(attrs);
			opInvokers.putAll(ops);
			synchronized(addedObjectMetas) {
				cacheMBeanInfo(Reflector.newMerger(getCachedMBeanInfo()).append(info).merge());
				sendNotification(new Notification("jmx.mbean.info.changed", this, notifSerial.incrementAndGet()));
			}
			
			
		}
*/		
		
		public void reset() {			
			sendNotification(new Notification("ewma.reset", this, notifSerial.incrementAndGet(), SystemClock.time(), "EWMA Resets: " + uuidElapsed.toString()));
			uuidElapsed.reset();			
		}
		
		/**
		 * Issues a write replace if this bean is serialized, replacing it in the object output stream with the ObjectName
		 * @return this bean's ObjectName
		 * @throws ObjectStreamException
		 */
		Object writeReplace() throws ObjectStreamException {
			return objectName;
		}	

		
		protected Date date = new Date();
		protected String randomUUID = null;
		
		public DirectEWMAMBean getUUIDElapsed() {
			return uuidElapsed;
		}
		
		public Date getDate() {
			return date;
		}
		
		public long getUTC() {
			return date.getTime();
		}
		
		public void setUTC(long value) {
			date = new Date(value);
		}
		
		public String getRandomUUID() {
			return randomUUID;
		}
		
		public void setRandomUUID(String uuid) {
			randomUUID = uuid;
		}
		
		public String doRandomUUID() {
			long st = System.nanoTime();
			randomUUID =  UUID.randomUUID().toString();
			long el = System.nanoTime()-st;
			uuidElapsed.append(el);
			return randomUUID;
		}
		
		public String doRandomUUID(String a, String b, String c, String d, String e) {
			randomUUID = String.format("%s-%s-%s-%s-%s", a,b,c,d,e);
			return randomUUID;
		}
		
		protected void register() {
			register(null);
		}
		
		protected void register(ObjectName on) {
			this.objectName = on;
			if(on==null) on = JMXHelper.objectName(getCachedMBeanInfo().getDescriptor().getFieldValue("objectName"));
			JMXHelper.registerMBean(this, on);
		}
		
		private final Object[] EMPTY_ARGS = {};
		
		/**
		 * {@inheritDoc}
		 * @see javax.management.StandardMBean#getAttribute(java.lang.String)
		 */
		@Override
		public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
//			final long attrCode = StringHelper.longHashCode(attribute);
//			MethodHandle[] mhPair = attrInvokers.get(attrCode);
//			Object target = targetObjects.get(attrCode);
//			if(mhPair==null) throw new AttributeNotFoundException("No attribute named [" + attribute + "]");
//			if(mhPair[0]==null) throw new AttributeNotFoundException("Attribute named [" + attribute + "] is not readable");
//			log("MethodHandle [%s] : %s", attribute, mhPair[0].getClass().getName());
			try {				
				return managedObjects.getAttributeGetter(attribute).invoke();
						//mhPair[0].bindTo(target).invoke();
			} catch (Throwable e) {
				throw new ReflectionException(new Exception(e), "Failed to dynInvoke for attribute [" + attribute + "]");				
			}
		}
		
		/**
		 * {@inheritDoc}
		 * @see javax.management.StandardMBean#setAttribute(javax.management.Attribute)
		 */
		@Override
		public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
//			long attrCode = StringHelper.longHashCode(attribute.getName());
//			MethodHandle[] mhPair = attrInvokers.get(attrCode);
//			Object target = targetObjects.get(attrCode);
//			if(mhPair==null) throw new AttributeNotFoundException("No attribute named [" + attribute.getName() + "]");
//			if(mhPair.length==1 || mhPair[1]==null) throw new AttributeNotFoundException("Attribute named [" + attribute.getName() + "] is not readable");
			try {			
				managedObjects.getAttributeSetter(attribute.getName()).invoke(attribute.getValue());
//				mhPair[1].bindTo(target).invoke(attribute.getValue());
			} catch (Throwable e) {
				throw new ReflectionException(new Exception(e), "Failed to dynInvoke for attribute [" + attribute + "]");				
			}			
		}
		
		/**
		 * {@inheritDoc}
		 * @see javax.management.StandardMBean#invoke(java.lang.String, java.lang.Object[], java.lang.String[])
		 */
		@Override
		public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
//			long opCode = opCode(actionName, signature);
//			MethodHandle mh = opInvokers.get(opCode);
//			Object target = targetObjects.get(opCode);
//			if(mh==null) throw new MBeanException(new Exception(), "No operation named [" + actionName + "] with parameters " + Arrays.toString(signature));
			try {
//				int plength = (params==null ? 0 : params.length);
//				if(plength>0) {					
//					return mh.bindTo(target).invokeWithArguments(params);
//				} 				 
//				return mh.bindTo(target).invoke();
				return managedObjects.getOperationHandle(actionName, signature).invokeWithArguments(params);
				
//				return mh.invokeWithArguments(params);
			} catch (Throwable e) {
				e.printStackTrace(System.err);
				throw new ReflectionException(new Exception(e), "Failed to dynInvoke for operation [" + actionName + "]");				
			}			
		}
		
		/**
		 * Computes the op code for the operation name and signature
		 * @param opName The operation action name
		 * @param sig The operation signature
		 * @return the op code
		 */
		public long opCode(String opName, String...sig) {
			return StringHelper.longHashCode(opName + Arrays.deepToString(sig));
		}
		
		/**
		 * {@inheritDoc}
		 * @see javax.management.StandardMBean#preRegister(javax.management.MBeanServer, javax.management.ObjectName)
		 */
		@Override
		public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
			this.getCachedMBeanInfo().getDescriptor().setField("objectName", name.toString()); 
			return super.preRegister(server, name);
		}

		/**
		 * @param listener
		 * @param filter
		 * @param handback
		 * @see javax.management.NotificationBroadcasterSupport#addNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
		 */
		public void addNotificationListener(NotificationListener listener,
				NotificationFilter filter, Object handback) {
			broadcaster.addNotificationListener(listener, filter, handback);
		}

		/**
		 * @param listener
		 * @throws ListenerNotFoundException
		 * @see javax.management.NotificationBroadcasterSupport#removeNotificationListener(javax.management.NotificationListener)
		 */
		public void removeNotificationListener(NotificationListener listener)
				throws ListenerNotFoundException {
			broadcaster.removeNotificationListener(listener);
		}

		/**
		 * @param listener
		 * @param filter
		 * @param handback
		 * @throws ListenerNotFoundException
		 * @see javax.management.NotificationBroadcasterSupport#removeNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
		 */
		public void removeNotificationListener(NotificationListener listener,
				NotificationFilter filter, Object handback)
				throws ListenerNotFoundException {
			broadcaster.removeNotificationListener(listener, filter, handback);
		}

		/**
		 * @param notification
		 * @see javax.management.NotificationBroadcasterSupport#sendNotification(javax.management.Notification)
		 */
		public void sendNotification(Notification notification) {
			broadcaster.sendNotification(notification);
		}

		/**
		 * @return
		 * @see javax.management.NotificationBroadcasterSupport#getNotificationInfo()
		 */
		public MBeanNotificationInfo[] getNotificationInfo() {
			MBeanInfo minfo =  getMBeanInfo();
			if(minfo==null) return EMPTY_N_INFO;
			return minfo.getNotifications();
		}
		
		public MBeanInfo getMBeanInfo() {
			if(getCachedMBeanInfo()==null) {
				return new MBeanInfo(getClass().getName(), "", new MBeanAttributeInfo[0], new MBeanConstructorInfo[0], new MBeanOperationInfo[0], EMPTY_N_INFO);
			}
			return getCachedMBeanInfo();
		}

	}

}
