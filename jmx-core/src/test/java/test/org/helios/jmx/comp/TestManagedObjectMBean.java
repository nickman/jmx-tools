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

import java.util.Random;
import java.util.UUID;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerDelegate;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.helios.jmx.annotation.ManagedMetric;
import org.helios.jmx.annotation.ManagedOperation;
import org.helios.jmx.annotation.ManagedOperationParameter;
import org.helios.jmx.mbean.ManagedObjectBaseMBean;
import org.helios.jmx.mbean.PopOperationsMBean;
import org.helios.jmx.metrics.ewma.ConcurrentDirectEWMA;
import org.helios.jmx.metrics.ewma.ConcurrentDirectEWMAMBean;
import org.helios.jmx.util.helpers.JMXHelper;
import org.helios.jmx.util.helpers.SystemClock;
import org.helios.jmx.util.helpers.SystemClock.ElapsedTime;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.org.helios.jmx.BaseTest;

/**
 * <p>Title: TestManagedObjectMBean</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.jmx.comp.TestManagedObjectMBean</code></p>
 */

public class TestManagedObjectMBean extends BaseTest {
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

	@Test
	public void testAnnotatedMBean() throws Exception {
		try {
			String ons = "test.org.helios.jmx.comp:service=ManagedObjectMBean,type=" + name.getMethodName();
			final ObjectName on = JMXHelper.objectName(ons);
			ManagedObjectBaseMBean tb = new ManagedObjectBaseMBean(PopOperationsMBean.class, true) {
				final ConcurrentDirectEWMA randoms = new ConcurrentDirectEWMA(100);
				@ManagedMetric(displayName="RandomsElapsed", description="Random Generation Elapsed Times", unit="ns")
				public ConcurrentDirectEWMAMBean getRandomsElapsed() {
					return randoms;
				}
				
				@ManagedOperation(name="generateRandom", parameters=@ManagedOperationParameter(name="quant", description="The number of randoms to generate"))
				public void generateRandoms(int quant) {
					ElapsedTime et = SystemClock.startClock();
					Random r = new Random(System.currentTimeMillis());
					for(int i = 0; i < quant; i++) {
						UUID uuid = new UUID(r.nextLong(), r.nextLong());
					}
					randoms.append(et.elapsed());
				}
				
				@ManagedOperation(name="generateRandomRandoms")
				public void generateRandoms() {
					ElapsedTime et = SystemClock.startClock();
					Random r = new Random(System.currentTimeMillis());
					int quant = Math.abs(r.nextInt(1000000));
					for(int i = 0; i < quant; i++) {
						UUID uuid = new UUID(r.nextLong(), r.nextLong());
					}
					randoms.append(et.elapsed());
				}
				
				
			};
			JMXHelper.registerMBean(tb, on);
			SystemClock.sleep(120000);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			throw ex;
		}
	}
			
	
}
