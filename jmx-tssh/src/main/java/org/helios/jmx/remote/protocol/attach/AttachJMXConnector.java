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
package org.helios.jmx.remote.protocol.attach;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnector;
import javax.security.auth.Subject;

import org.helios.vm.attach.VirtualMachine;
import org.helios.vm.attach.VirtualMachineDescriptor;

/**
 * <p>Title: AttachJMXConnector</p>
 * <p>Description: Attach API JMXConnector implementation</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.remote.protocol.attach.AttachJMXConnector</code></p>
 */

public class AttachJMXConnector implements JMXConnector {
	/** The target JVM id */
	protected String jvmId = null;
	/** The target JVM display name  */
	protected String jvmDisplayName = null;
	/** The target JVM display name matching pattern */
	protected Pattern displayNamePattern = null;
	/** The JMXServiceURL sap */
	protected String urlPath = null;
	/** The attached vm */
	protected VirtualMachine vm = null;
	/** The JMXConnector to the attached JVM */
	protected JMXConnector jmxConnector = null;
	/** The attached VM's system properties */
	protected Properties vmSystemProperties = null;
	/** The attached VM's agent properties */
	protected Properties vmAgentProperties = null;
	/**
	 * Creates a new AttachJMXConnector
	 * @param jvmIdentifier The target JVM identifier or display name match expression
	 */
	public AttachJMXConnector(String jvmIdentifier) {
		if(jvmIdentifier==null || jvmIdentifier.trim().isEmpty()) throw new IllegalArgumentException("The passed JVM identifier was null or empty");
		urlPath = jvmIdentifier.trim();
		if(isNumber(urlPath)) {
			jvmId = urlPath;
		} else {
			if(urlPath.startsWith("[") && urlPath.endsWith("]")) {
				StringBuilder b = new StringBuilder(urlPath);
				b.deleteCharAt(0);
				b.deleteCharAt(b.length()-1);
				displayNamePattern = Pattern.compile(b.toString());
			} else {
				jvmDisplayName = urlPath;
			}
		}		
	}
	
	/**
	 * Connects to the target virtual machine
	 */
	protected void attach() {
		if(jvmId!=null) {
			vm = VirtualMachine.attach(jvmId);
			return;
		}
		List<VirtualMachineDescriptor> machines = VirtualMachine.list();
		for(VirtualMachineDescriptor vmd: machines) {
			String displayName = vmd.displayName();
			if(jvmDisplayName!=null) {
				if(jvmDisplayName.equals(displayName)) {
					vm = VirtualMachine.attach(vmd.id());
					return;
				}
			} else {
				Matcher m = displayNamePattern.matcher(displayName);
				if(m.matches()) {
					vm = VirtualMachine.attach(vmd.id());
					return;
				}
			}
		}
		throw new RuntimeException("Failed to find any matching JVMs for [" + urlPath + "]");
	}
	
	
	/**
	 * Determines if the passed value is a number in which case it can be assumed the JVM identifier is the PID
	 * @param value The jvm identifier to test
	 * @return true for a number, false otherwise
	 */
	protected static boolean isNumber(String value) {
		try {
			Long.parseLong(value);
			return true;
		} catch (Exception ex) {
			return false;
		}
		
	}
	

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#connect()
	 */
	@Override
	public void connect() throws IOException {
		attach();
		jmxConnector = vm.getJMXConnector();
		jvmId = vm.id();
		vmSystemProperties = vm.getSystemProperties();
		vmAgentProperties = vm.getAgentProperties();
		try { vm.detach(); } catch (Exception x) {/* No Op */}		
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#connect(java.util.Map)
	 */
	@Override
	public void connect(Map<String, ?> env) throws IOException {
		connect();
	}


	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#getConnectionId()
	 */
	@Override
	public String getConnectionId() throws IOException {		
		return String.format("[Attached:%s] %s", jvmId, jmxConnector.getConnectionId());
	}

	public MBeanServerConnection getMBeanServerConnection() throws IOException {
		return jmxConnector.getMBeanServerConnection();
	}

	public MBeanServerConnection getMBeanServerConnection(
			Subject delegationSubject) throws IOException {
		return jmxConnector.getMBeanServerConnection(delegationSubject);
	}

	public void close() throws IOException {
		jmxConnector.close();
	}

	public void addConnectionNotificationListener(
			NotificationListener listener, NotificationFilter filter,
			Object handback) {
		jmxConnector.addConnectionNotificationListener(listener, filter,
				handback);
	}

	public void removeConnectionNotificationListener(
			NotificationListener listener) throws ListenerNotFoundException {
		jmxConnector.removeConnectionNotificationListener(listener);
	}

	public void removeConnectionNotificationListener(NotificationListener l,
			NotificationFilter f, Object handback)
			throws ListenerNotFoundException {
		jmxConnector.removeConnectionNotificationListener(l, f, handback);
	}

	public Properties getVmSystemProperties() {
		return vmSystemProperties;
	}

	public Properties getVmAgentProperties() {
		return vmAgentProperties;
	}

}
