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
package test.org.helios.jmx.connectors.attach;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.helios.jmx.remote.protocol.attach.AttachJMXConnector;
import org.helios.jmx.util.helpers.JMXHelper;
import org.helios.vm.attach.VirtualMachine;
import org.helios.vm.attach.VirtualMachineDescriptor;
import org.junit.Test;

import test.org.helios.jmx.BaseTest;

/**
 * <p>Title: AttachConnectorTest</p>
 * <p>Description: Unit tests for the attach connector</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.jmx.connectors.attach.AttachConnectorTest</code></p>
 */

public class AttachConnectorTest extends BaseTest {

	/**
	 * Attempts connections to all located JVMs
	 * @throws Exception on any error
	 */
	@Test
	public void testAttachConnectorById() throws Exception {
		for(VirtualMachineDescriptor vmd: VirtualMachine.list()) {
			JMXConnector jmxConnector = null;
			try {
				String id = vmd.id();
				JMXServiceURL serviceURL = jmxUrl("service:jmx:attach://%s", id);
				jmxConnector = JMXConnectorFactory.connect(serviceURL);
				log("Connected to [%s]:[%s]", jmxConnector.getConnectionId(), ((AttachJMXConnector)jmxConnector).getVmAgentProperties());
				MBeanServerConnection conn = jmxConnector.getMBeanServerConnection();
				log("VM is [%s]", JMXHelper.getAttribute(conn, ManagementFactory.RUNTIME_MXBEAN_NAME, new String[] {"Name"}));
			} finally {
				try { jmxConnector.close(); } catch (Exception ex) {}
			}			
		}
	}

}
