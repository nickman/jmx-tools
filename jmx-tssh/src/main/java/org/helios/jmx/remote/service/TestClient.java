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

import org.apache.log4j.BasicConfigurator;
import org.helios.opentsdb.CommandTerminal;
import org.helios.opentsdb.ExtendedConnection;
import org.helios.opentsdb.SSHConnectionConfiguration;
import org.helios.opentsdb.TunnelManager;

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
		BasicConfigurator.configure();
		try {
			serviceURL = new JMXServiceURL(url);
			jmxConnector = JMXConnectorFactory.connect(serviceURL);
			log("Connected.");
			conn = jmxConnector.getMBeanServerConnection();
			final String runtime = conn.getAttribute(new ObjectName(ManagementFactory.RUNTIME_MXBEAN_NAME), "Name").toString();
			log("Runtime: [%s]", runtime);
//			final String PID = runtime.split("@")[0];
			
//			final TunnelManager tm = TunnelManager.getInstance();
//			SSHConnectionConfiguration config = SSHConnectionConfiguration.
//					newBuilder("tpsolaris", "nwhitehe")
//					.setUserPassword("mysol!1")
//					.setKeyExchangeTimeout(0)
//					.setVerifyHosts(false)
//					.build();
//			SSHConnectionConfiguration config = SSHConnectionConfiguration.
//					newBuilder("10.12.114.48", "nwhitehe")
//					.setUserPassword("jer1029")
//					.setKeyExchangeTimeout(2000)
//					.setConnectTimeout(2000)
//					.setVerifyHosts(false)
//					.build();
//			
//			log("SSHConfig:\n%s", config);
//			final String PID = "3766"; //runtime.split("@")[0];
//			ExtendedConnection conn = tm.getConnection(config);
//			conn.fullAuth();
//			log("Full Authed.");
//			CommandTerminal ct = conn.createCommandTerminal();
//			log("Command Terminal Created.");
//			StringBuilder[] results = ct.execSplit("ps -ef | grep " + PID + " | grep -v grep");
//			for(StringBuilder s: results) {
//				log("Command Result:\n\t%s", s);
//			}
			
			jmxConnector.close();
			
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
		//new TestClient("service:jmx:rmi://njwmintx:8005/jndi/rmi://njwmintx:8009/jmxrmi");
//		final String URL_TEMPLATE = "service:jmx:tunnel://localhost:%s/ssh/jmxmp:u=%s,p=%s,h=%s,lp=%s,sk=%s";
//		new TestClient(String.format(URL_TEMPLATE,
//			//8006,"nwhitehe", "mysol!1", "tpsolaris", 8006, false			
//			8006,"nwhitehe", "jer1029", "10.12.114.48", 8006, false
//		));
		
		new TestClient("service:jmx:tunnel://pdk-pt-ceas-01:17083/ssh/jmxmp:pr=C:/ProdMonitors/ssh.properties,pref=pdk-ecs,h=pdk-pt-ceas-01,p=XXXX");
		new TestClient("service:jmx:tunnel://pdk-pt-ceas-01:17082/ssh/jmxmp:pr=C:/ProdMonitors/ssh.properties,pref=pdk-ecs,h=pdk-pt-ceas-01,p=XXXX");
		

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
