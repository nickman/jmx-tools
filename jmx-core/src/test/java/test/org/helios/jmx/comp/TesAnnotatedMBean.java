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
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.Descriptor;
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
import javax.management.MBeanServerDelegate;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.StandardMBean;
import javax.management.remote.JMXConnector;

import org.helios.jmx.annotation.ManagedAttribute;
import org.helios.jmx.annotation.ManagedMetric;
import org.helios.jmx.annotation.ManagedNotification;
import org.helios.jmx.annotation.ManagedOperation;
import org.helios.jmx.annotation.ManagedOperationParameter;
import org.helios.jmx.annotation.ManagedResource;
import org.helios.jmx.annotation.Reflector;
import org.helios.jmx.annotation.Reflector.MBeanInfoMerger;
import org.helios.jmx.managed.ManagedObjectRepo;
import org.helios.jmx.metrics.ewma.ConcurrentDirectEWMA;
import org.helios.jmx.metrics.ewma.ConcurrentDirectEWMAMBean;
import org.helios.jmx.metrics.ewma.DirectEWMA;
import org.helios.jmx.metrics.ewma.DirectEWMAMBean;
import org.helios.jmx.util.helpers.JMXHelper;
import org.helios.jmx.util.helpers.StringHelper;
import org.helios.jmx.util.helpers.SystemClock;
import org.helios.jmx.util.helpers.SystemClock.ElapsedTime;
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
	
	/** The delegate MBeanServer's ObjectName */
	protected static final ObjectName DELEGATE = JMXHelper.objectName(MBeanServerDelegate.DELEGATE_NAME);
	public static final MBeanNotificationInfo[] EMPTY_N_INFO = {};
	
	@Test
	public void testAnnotatedMBean() throws Exception {
		try {
			String ons = "test.org.helios.jmx.comp:service=TestMBean,type=" + name.getMethodName();
			ObjectName on = JMXHelper.objectName(ons);
			
			TestBean tb = new TestBean(TestBeanMBean.class, false);
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
			final TestBeanMBean tbm = JMX.newMBeanProxy(conn, on, TestBeanMBean.class);
//			tbm.pop("UUIDElapsed");
			tbm.popAll();
			Thread t = new Thread("EWMAExcerciser") {
				public void run() {
					while(true) {
						tbm.doRandomUUID();
						try { Thread.sleep(100); } catch (Exception x) {}
					}							
				}
			};
			t.setDaemon(true);
			t.start();
			Thread t2 = new Thread("EWMAResetter") {
				public void run() {
					while(true) {						
						try { Thread.sleep(5000); } catch (Exception x) {}
						tbm.reset();
					}							
				}
			};
			t2.setDaemon(true);
			t2.start();
			Thread t3 = new Thread("RandomExcerciser") {
				public void run() {
					while(true) {						
						try { Thread.sleep(4000); } catch (Exception x) {}
						tbm.generateRandoms();
					}							
				}
			};
			t3.setDaemon(true);
			t3.start();
			
			try { Thread.sleep(5000); } catch (Exception x) {}
//			log(tbm.getUUIDElapsed().toString());
			MBeanInfo mi = conn.getMBeanInfo(on);
			StringBuilder b = new StringBuilder("\t");
			for(MBeanAttributeInfo minfo: mi.getAttributes()) {
				Object value = conn.getAttribute(on, minfo.getName());
				log("[%s] type: [%s], value: [%s]", minfo.getName(), value.getClass().getName(), value);
				b.append(minfo.getName()).append(":").append(conn.getAttribute(on, minfo.getName())).append(", ");
			}
			b.deleteCharAt(b.length()-1);
			b.deleteCharAt(b.length()-1);
			log(b.toString());
			ObjectName OSON = JMXHelper.objectName(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
			MBeanInfo osInfo = conn.getMBeanInfo(OSON);
			MBeanAttributeInfo minfo = null;
			for(MBeanAttributeInfo m: osInfo.getAttributes()) {
				if(m.getName().equals("SystemCpuLoad")) {
					minfo = m;
					break;
				}
			}
			Descriptor d = minfo.getDescriptor();
			for(String s: d.getFieldNames()) {
				Object value = d.getFieldValue(s);
				log("D Name: [%s], Type: [%s] Value: [%s]", s, value.getClass().getName(), value);
			}
			Thread.currentThread().join();
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
		
		@ManagedOperation(name="GenerateRandoms", description="Generates a random number of UUIDs")
		public void generateRandoms();
		
		
		@ManagedMetric(displayName="UUIDElapsed", description="Elapsed times for UUID generation", unit="ns", popable=true)
		public DirectEWMAMBean getUUIDElapsed();
		@ManagedMetric(displayName="UUIDRandom", description="The rate of random generation", unit="Randoms/ms", popable=true)
		public DirectEWMAMBean getUUIDRandom();
		
		@ManagedOperation(name="reset", description="Resets the metrics", notifications={
				@ManagedNotification(notificationTypes={"ewma.reset"}, name="EWMAReset", description="Notification emitted when the EWMA is reset")
		})
		public void reset();
//		@ManagedOperation(name="popEWMAs", description="Promotes the EWMA fields to be first class MBean attributes", notifications={
//				@ManagedNotification(notificationTypes={"jmx.mbean.info.changed"}, name="MBeanInfoChanged", description="Notification emitted when the MBeanInfo is updated")
//		})
//		public void popEWMAs();
//		@ManagedOperation(name="unpopEWMAs", description="Removes the EWMA fields from first class MBean status", notifications={
//				@ManagedNotification(notificationTypes={"jmx.mbean.info.changed"}, name="MBeanInfoChanged", description="Notification emitted when the MBeanInfo is updated")
//		})		
//		public void unpopEWMAs();

		/**
		 * Pops the named attribute
		 * @param name The name of the attribute
		 * @return true if the object was popped, false if it could not be found and null if it was already popped. 
		 */
		@ManagedOperation(name="popAttribute", description="Pops the named attribute", 
				notifications={ @ManagedNotification(notificationTypes={"jmx.mbean.info.changed"}, name="MBeanInfoChanged", description="Notification emitted when the MBeanInfo is updated") },
				parameters={@ManagedOperationParameter(name="AttributeName", description="The name of the attribute to pop")}
		)
		public Boolean pop(String name);
		
		/**
		 * Unpops the named attribute
		 * @param name The name of the attribute
		 * @return true if the object was unpopped, false if it could not be found and null if it was already unpopped. 
		 */
		@ManagedOperation(name="unpopAttribute", description="Unpops the named attribute", 
				notifications={ @ManagedNotification(notificationTypes={"jmx.mbean.info.changed"}, name="MBeanInfoChanged", description="Notification emitted when the MBeanInfo is updated") },
				parameters={@ManagedOperationParameter(name="AttributeName", description="The name of the attribute to unpop")}
		)				
		public Boolean unpop(String name);
		
		/**
		 * Pops all the popable attributes that have not been popped
		 * @return the number of objects popped
		 */
		@ManagedOperation(name="popAll", description="Pops all the popable attributes", 
				notifications={ @ManagedNotification(notificationTypes={"jmx.mbean.info.changed"}, name="MBeanInfoChanged", description="Notification emitted when the MBeanInfo is updated") }				
		)				
		public int popAll();
		
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
		final ManagedObjectRepo<Object> managedObjects = new ManagedObjectRepo<Object>(System.identityHashCode(this));
		final NotificationBroadcasterSupport broadcaster;  
		
		ObjectName objectName = null;
		protected TestBean(Class<?> mbeanInterface, boolean isMXBean) {
			super(mbeanInterface, isMXBean);
			cacheMBeanInfo(managedObjects.put(this));
			broadcaster = new NotificationBroadcasterSupport(this.getMBeanInfo().getNotifications());
		}		
		final AtomicLong notifSerial = new AtomicLong(0L);
		
		private final Random R = new Random(System.currentTimeMillis());
		
		
		
		final DirectEWMAMBean uuidElapsed = new DirectEWMA(100);
		public DirectEWMAMBean getUUIDElapsed() {
			return uuidElapsed;
		}
		
		final DirectEWMAMBean randomElapsed = new DirectEWMA(10000);
		public DirectEWMAMBean getUUIDRandom() {
			return randomElapsed;
		}
		
		public void generateRandoms() {
			ElapsedTime et = SystemClock.startClock();
			long alloy = 0;
			int loops = Math.abs(R.nextInt(1000)) + 1000;
			int cnt = 0;
			for(int i = 0; i < loops; i++) {
				long t = UUID.randomUUID().getLeastSignificantBits();
				if(cnt%2==0) {
					alloy += t;
				} else {
					alloy -= t;
				}
				cnt++;
			}
			
			long elapsed = et.elapsedMs();
			long rate = et.rateMs(cnt);
			log("genRand Rate: %s/ms  (Loops: %s Elapsed: %s Alloy: %s)", rate, loops, elapsed, alloy);
			randomElapsed.append(rate);	
//			randomElapsed.increment(loops-1);
		}
		
		private void fireMBeanInfoChanged() {
			Notification notif = new Notification("jmx.mbean.info.changed", this, notifSerial.incrementAndGet());
			notif.setUserData(getCachedMBeanInfo());
			sendNotification(notif);
//
//			notif = new MBeanServerNotification(MBeanServerNotification.UNREGISTRATION_NOTIFICATION, JMXHelper.objectName(MBeanServerDelegate.DELEGATE_NAME),notifSerial.incrementAndGet(), this.objectName); 			
//			sendNotification(notif);
//			notif = new MBeanServerNotification(MBeanServerNotification.REGISTRATION_NOTIFICATION, JMXHelper.objectName(MBeanServerDelegate.DELEGATE_NAME),notifSerial.incrementAndGet(), this.objectName);
//			notif.setUserData(getCachedMBeanInfo());
//			sendNotification(notif);
			
		}
		
		public Boolean pop(String name) {			
			MBeanInfo info = managedObjects.pop(name);
			if(info!=null) {
				synchronized(managedObjects) {
					cacheMBeanInfo(Reflector.newMerger(getCachedMBeanInfo()).append(info).merge());
					fireMBeanInfoChanged();					
					return true;
				}
			}
			return false;
		}
			
		public Boolean unpop(String name) {
			boolean result =  managedObjects.unpop(name);
			if(result) {
				synchronized(managedObjects) {
					cacheMBeanInfo(managedObjects.mergeAllMBeanInfos());
					fireMBeanInfoChanged();										
				}				
			}
			return result;
		}
		
		/**
		 * @return
		 */
		public int popAll() {
			MBeanInfo[] infos = managedObjects.popAll();
			if(infos!=null && infos.length>0) {				
				synchronized(managedObjects) {
					MBeanInfo merged = Reflector.newMerger(getCachedMBeanInfo()).append(infos).merge();
					cacheMBeanInfo(merged);
					fireMBeanInfoChanged();					
				}
			}
			return infos.length;
		}
		
		
		
		public void reset() {			
			sendNotification(new Notification("ewma.reset", this, notifSerial.incrementAndGet(), SystemClock.time(), "EWMA Resets: " + uuidElapsed.toString()));
			uuidElapsed.reset();	
			randomElapsed.reset();
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
			try {
				return managedObjects.getAttributeGetter(attribute).invoke();
			} catch (Throwable e) {
				throw new ReflectionException(new Exception(e), "Failed to dynInvoke for attribute [" + attribute + "]");				
			}
		}
		
		private final AttributeList EMPTY_ATTR_LIST = new AttributeList(0); 
		
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
				return managedObjects.getOperationHandle(actionName, signature).invoke(params);
				
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
