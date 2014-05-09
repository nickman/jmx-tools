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

import java.util.Date;

import javax.management.StandardMBean;

import org.helios.jmx.annotation.ManagedAttribute;
import org.helios.jmx.annotation.ManagedResource;
import org.helios.jmx.annotation.Reflector;
import org.helios.jmx.util.helpers.JMXHelper;
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

	@Test
	public void testAnnotatedMBean() throws Exception {
		try {
			TestBean tb = new TestBean(TestBeanMBean.class, true);
			tb.register();
			Thread.sleep(60000);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			throw ex;
		}
	}
	
	@ManagedResource(objectName="test.org.helios.jmx.comp:service=TestMBean", description="A test mbean")
	public interface TestBeanMBean {
		@ManagedAttribute(description="Returns the system date", name="SystemDate")
		public Date getDate();
	}
	
	/*
	 * TODO:
	 * DescriptorBuilder
	 * Invoker
	 * Metric --> Class 
	 */
	
	
	public class TestBean extends StandardMBean implements TestBeanMBean {
		protected TestBean(Class<?> mbeanInterface, boolean isMXBean) {
			super(mbeanInterface, isMXBean);			
			cacheMBeanInfo(Reflector.clean(Reflector.from(mbeanInterface)));
		}		
		public Date getDate() {
			return new Date();
		}
		
		protected void register() {
			JMXHelper.registerMBean(this, JMXHelper.objectName(getCachedMBeanInfo().getDescriptor().getFieldValue("objectName")));
		}
	}

}
