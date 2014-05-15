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
package org.helios.jmx.util.reference;

import java.lang.ref.Reference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.helios.jmx.util.helpers.JMXHelper;
import org.helios.jmx.util.reference.ReferenceService.ReferenceType;


/**
 * <p>Title: MBeanProxy</p>
 * <p>Description: Proxy MBean that avoid strong reference to the real MBean object</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Kohsuke Kawaguchi
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.util.reference.MBeanProxy</code></p>
 */
public class MBeanProxy implements InvocationHandler, MBeanRegistration, ReferenceRunnable {
	
	/** The reference holding the real impl */
	private final Reference<Object> real;
	/** The MBeanServer where the MBean was registered */
	private MBeanServer server;
	/** The ObjectName of the registered MBean */
	private ObjectName name;
	/** The proxy for the MBean */
	private Object proxy = null; 
	
	
	private MBeanProxy(ReferenceType refType, Object realObject) {
		this.real = ReferenceService.getInstance().newReference(refType, realObject, this);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		System.out.println(" ================> Unregister Task: Server [" + server + "], ObjectName: [" + name + "]");
		if(server!=null && name != null) {
			try {
				server.unregisterMBean(name);
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.util.reference.ReferenceRunnable#getClearedRunnable()
	 */
	@Override
	public Runnable getClearedRunnable() {
		return this;
	}

	
	/**
	 * Creates a proxy MBean and registers it to the server, overriding the
	 * existing mbean if necessary.
	 * @param refType The reference type to store the actual impl
	 * @param server MBean will be registered to this server.
	 * @param name The name under which the MBean will be registered
	 * @param mbeanInterface MBean interface to be exposed
	 * @param object MBean instance to be exposed
	 */
	public static void register(ReferenceType refType, MBeanServer server, ObjectName name, Class<Object> mbeanInterface, Object object) {
		try {
			MBeanProxy mbeanProxy = new MBeanProxy(refType, object);
			Object proxy = mbeanInterface.cast(Proxy.newProxyInstance(
					mbeanInterface.getClassLoader(), new Class[] { mbeanInterface,
							MBeanRegistration.class }, mbeanProxy));
			mbeanProxy.proxy = proxy;
	
			if (server.isRegistered(name)) {
				try {
					server.unregisterMBean(name);
				} catch (JMException e) {
					// if we fail to unregister, try to register ours anyway.
					// maybe a GC kicked in in-between.
				}
			}
	
			// since the proxy class has random names like '$Proxy1',
			// we need to use StandardMBean to designate a management interface
			server.registerMBean(new StandardMBean(proxy, mbeanInterface), name);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Creates a proxy MBean and registers it to the default MBeanServer, overriding the
	 * existing mbean if necessary.
	 * @param refType The reference type to store the actual impl
	 * @param name The name under which the MBean will be registered
	 * @param mbeanInterface MBean interface to be exposed
	 * @param object MBean instance to be exposed
	 */
	public static void register(ReferenceType refType, ObjectName name, Class<Object> mbeanInterface, Object object)  {
		try {
			register(refType, JMXHelper.getHeliosMBeanServer(), name, mbeanInterface, object);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Creates and returns an MBean proxy without registering it
	 * @param refType THe reference type
	 * @param mbeanInterface The MBean interface
	 * @param object The MBean impl
	 * @param proxyInterfaces Additional interfaces implemented by the proxy
	 * @return the MBean proxy which can be registered
	 */
	public static MBeanProxy proxyMBean(ReferenceType refType, Class<?> mbeanInterface, Object object, Class<?>... proxyInterfaces)  {
		MBeanProxy mbeanProxy = new MBeanProxy(refType, object);
		Class<?>[] proxyifaces = new Class[proxyInterfaces.length + 2];
		proxyifaces[0] = mbeanInterface;
		proxyifaces[1] = MBeanRegistration.class;
		for(int i = 0; i < proxyInterfaces.length; i++) {
			proxyifaces[i+2] = proxyInterfaces[i];
		}
//		StringBuilder b = new StringBuilder("\n\tMBean Ifaces:");
//		for(Class c: proxyifaces) {
//			b.append("\n\t").append(c.getName());
//		}
//		System.out.println(b.append("\n"));
		Object proxy = mbeanInterface.cast(Proxy.newProxyInstance(
				mbeanInterface.getClassLoader(), proxyifaces, mbeanProxy));
		mbeanProxy.proxy = proxy;
		return mbeanProxy;
	}

	/**
	 * Returns the JMX registerable proxy instance
	 * @return the JMX registerable proxy instance
	 */
	public Object getMBeanProxy() {
		return proxy;
	}
	
	/**
	 * Returns the reference to the real mbean impl which is being proxied
	 * @return the reference to the real mbean impl which is being proxied
	 */
	public Reference<?> getReference() {
		return real;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
	 */
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {		
		Object o = real.get();
		Object response = null;
		if (method.getDeclaringClass() == MBeanRegistration.class) {
			try {				
				response = method.invoke(this, args);
			} catch (InvocationTargetException e) {
				if (e.getCause() != null) throw e.getCause();
				throw e;
			}
			if(o instanceof MBeanRegistration) {
				try {
					method.invoke(this, args);
				} catch (InvocationTargetException e) {
					if (e.getCause() != null) throw e.getCause();
					throw e;
				}								
			}
			return response;			
		}

		if (o == null) {
			throw new InstanceNotFoundException(name + " no longer exists");
			//unregister();
//			throw new IllegalStateException(name + " no longer exists");
		}

		try {
			return method.invoke(o, args);
		} catch (InvocationTargetException e) {
			if (e.getCause() != null) throw e.getCause();
			throw e;
		}
	}

	private void unregister() {
		try {
			server.unregisterMBean(name);
			name = null;
		} catch (JMException e) {
			throw new Error(e); // is this even possible?
		}
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#preRegister(javax.management.MBeanServer, javax.management.ObjectName)
	 */
	public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
		this.server = server;
		this.name = name;
		return name;
	}

	public void postRegister(Boolean registrationDone) {
		// noop
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#preDeregister()
	 */
	public void preDeregister() throws Exception {
		// noop
	}

	public void postDeregister() {
		server = null;
		name = null;
	}

}