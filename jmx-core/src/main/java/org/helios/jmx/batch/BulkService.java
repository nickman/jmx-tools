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
package org.helios.jmx.batch;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * <p>Title: BulkService</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.batch.BulkService</code></p>
 */

public class BulkService implements MBeanRegistration {
	/** This MBean's ObjectName */
	protected ObjectName myObjectName = null;
	/** The MBeanServer this service is registered in */
	protected MBeanServer server = null;
	/** Indicates if registration has started on this instance */
	protected boolean registered = false;

	public Map<ObjectName, Map<String, Object>> getAttributes(List<String> attributeNames, ObjectName ...objectNames) {
		Map<ObjectName, Map<String, Object>> map = null;
		Set<ObjectName> resolved = new HashSet<ObjectName>();
		for(ObjectName on: objectNames) {
			resolved.addAll(server.queryNames(on, null));
		}
		map = new HashMap<ObjectName, Map<String, Object>>(resolved.size());
		for(ObjectName on: resolved) { map.put(on, new HashMap<String, Object>()); }
		for(ObjectName on: resolved) {
			Map<String, Object> onMap = map.get(on);
			for(String attr: attributeNames) {
				try {
					Object value = server.getAttribute(on, attr);
					if(value instanceof Serializable) {
						onMap.put(attr, value);
					}
				} catch (Exception ex) {}
			}
		}
		return map;
	}
	
	public Map<ObjectName, Object> invoke(ObjectName objectNames[], String opName, Object[] params, String[] signature) {
		Map<ObjectName, Object> map = null;
		Set<ObjectName> resolved = new HashSet<ObjectName>();
		for(ObjectName on: objectNames) {
			resolved.addAll(server.queryNames(on, null));
		}
		map = new HashMap<ObjectName, Object>(resolved.size());
		for(ObjectName on: resolved) {
			try {
				Object value = server.invoke(on, opName, params, signature);
				if(value instanceof Serializable) {
					map.put(on, value);
				}				
			} catch (Exception ex) {}
		}
		return map;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#preRegister(javax.management.MBeanServer, javax.management.ObjectName)
	 */
	@Override
	public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
		if(registered) return name;
		registered = true;
		myObjectName = name;
		this.server = server;
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		if(!mbs.getDefaultDomain().equals(server.getDefaultDomain())) {
			ManagementFactory.getPlatformMBeanServer().registerMBean(this, name);
		}
		return name;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#postRegister(java.lang.Boolean)
	 */
	@Override
	public void postRegister(Boolean registrationDone) {
		/* No Op */
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#preDeregister()
	 */
	@Override
	public void preDeregister() throws Exception {
		/* No Op */
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#postDeregister()
	 */
	@Override
	public void postDeregister() {
		/* No Op */
	}
	

}

/**
 import javax.management.*;
import javax.management.remote.*;
import java.lang.management.*;


bulkAttrSig = [List.class.getName(), ObjectName[].class.getName()] as String[];
//jurl = new JMXServiceURL("service:jmx:attach:///[.*myjvm.*]");
jurl = new JMXServiceURL("service:jmx:jmxmp://localhost:17078");
connector = null;
server = null;
bulkOn = new ObjectName("blahblah:service=BulkJMXService");

bulkAttrs = { attrs, names ->
    server.invoke(bulkOn, "getAttributes", [attrs, names] as Object[], bulkAttrSig);
}

try {
    connector = JMXConnectorFactory.connect(jurl, null);
    println "Connected";
    server = connector.getMBeanServerConnection();
    bulkAttrs(["ProcessRateStats", "ReceiveTimeStats"], [new ObjectName("blahblah:service=JmsListenerRunner")] as ObjectName[]);
} finally {
    try { connector.close(); } catch (e) {}
}
*/