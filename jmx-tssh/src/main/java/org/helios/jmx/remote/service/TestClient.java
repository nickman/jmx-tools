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
package org.helios.jmx.remote.service;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * <p>Title: TestClient</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.remote.service.TestClient</code></p>
 */

public class TestClient {
	JMXServiceURL serviceURL = null;
	JMXConnector jmxConnector = null;
	MBeanServerConnection conn = null;
	
	
	/**
	 * Creates a new TestClient
	 */
	public TestClient(String url) {
		try {
			serviceURL = new JMXServiceURL(url);
			jmxConnector = JMXConnectorFactory.connect(serviceURL);
			log("Connected.");
			conn = jmxConnector.getMBeanServerConnection();
			log("Runtime: [%s]", conn.getAttribute(new ObjectName(ManagementFactory.RUNTIME_MXBEAN_NAME), "Name"));
		} catch (Exception ex) {
			String msg = log("Failed to connect client with JMXServiceURL [%s]", url);
			throw new RuntimeException(msg, ex);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("Test Client");
		new TestClient("service:jmx:rmi://njwmintx:8005/jndi/rmi://njwmintx:8009/jmxrmi");

	}
	
	/**
	 * Pattern logger
	 * @param format The pattern format
	 * @param args The pattern values
	 * @return the formatted message 
	 */
	public static String log(Object format, Object...args) {
		String msg = String.format("[TestServers]" + format.toString(),args);
		System.out.println(msg);
		return msg;
	}		

}
