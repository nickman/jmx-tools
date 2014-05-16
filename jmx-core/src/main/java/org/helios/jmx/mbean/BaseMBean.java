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
package org.helios.jmx.mbean;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

/**
 * <p>Title: BaseMBean</p>
 * <p>Description: Base instrumented MBean implementation</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.mbean.BaseMBean</code></p>
 */

public class BaseMBean extends StandardMBean {

	/**
	 * Creates a new BaseMBean
	 * @param mbeanInterface
	 * @throws NotCompliantMBeanException
	 */
	public BaseMBean(Class<?> mbeanInterface) throws NotCompliantMBeanException {
		super(mbeanInterface);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new BaseMBean
	 * @param implementation
	 * @param mbeanInterface
	 * @throws NotCompliantMBeanException
	 */
	public <T> BaseMBean(T implementation, Class<T> mbeanInterface)
			throws NotCompliantMBeanException {
		super(implementation, mbeanInterface);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new BaseMBean
	 * @param mbeanInterface
	 * @param isMXBean
	 */
	public BaseMBean(Class<?> mbeanInterface, boolean isMXBean) {
		super(mbeanInterface, isMXBean);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new BaseMBean
	 * @param implementation
	 * @param mbeanInterface
	 * @param isMXBean
	 */
	public <T> BaseMBean(T implementation, Class<T> mbeanInterface,
			boolean isMXBean) {
		super(implementation, mbeanInterface, isMXBean);
		// TODO Auto-generated constructor stub
	}

}
