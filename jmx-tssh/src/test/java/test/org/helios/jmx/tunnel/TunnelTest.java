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
package test.org.helios.jmx.tunnel;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXServiceURL;

import org.helios.jmx.remote.tunnel.SSHTunnelConnector;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.org.helios.jmx.BaseTest;

/**
 * <p>Title: TunnelTest</p>
 * <p>Description: Basic tunnel tests</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.jmx.tunnel.TunnelTest</code></p>
 */

public class TunnelTest extends BaseTest {
	protected static final int JMXMP_PORT = 8475;
	protected static JMXConnectorServer jmxmpServer = null;
	
	@BeforeClass
	public static void startServer() {
		jmxmpServer = startJmxMpServer("0.0.0.0", JMXMP_PORT);
		log("JMXMP Server Started");
	}
	
	@AfterClass
	public static void stopServer() {
		if(jmxmpServer!=null) {
			try {
				jmxmpServer.stop();
				log("JMXMP Server Stopped");
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
			}
		}
	}
	
	@Test
	public void testBasicTunnel() throws Exception {
		JMXConnector connector = null;
		try {
			JMXServiceURL serviceURL = jmxUrl("service:jmx:tunnel://localhost:%s/ssh/jmxmp:u=nwhitehead,kp=helios,h=tpmint,lp=%s", JMXMP_PORT, JMXMP_PORT+1);
			SSHTunnelConnector.tunnel(serviceURL, null);
			connector = JMXConnectorFactory.connect(jmxUrl("service:jmx:jmxmp://%s:%s", "tpmint", JMXMP_PORT+1));
			log("Connected [%s]", connector.getConnectionId());			
		} finally {
			if(connector!=null) try { connector.close(); } catch (Exception x) { /* No Op */ }
		}
	}
	
	@Test
	public void testJMXConnect() throws Exception {
		JMXConnector connector = null;
		try {
			JMXServiceURL serviceURL = jmxUrl("service:jmx:tunnel://localhost:%s/ssh/jmxmp:u=nwhitehead,kp=helios,h=tpmint,lp=%s", JMXMP_PORT, JMXMP_PORT+1);
			connector = JMXConnectorFactory.connect(serviceURL);
			log("Connected [%s]", connector.getConnectionId());			
		} finally {
			if(connector!=null) try { connector.close(); } catch (Exception x) { /* No Op */ }
		}
	}
	

}
