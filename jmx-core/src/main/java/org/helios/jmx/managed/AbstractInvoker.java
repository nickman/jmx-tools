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
package org.helios.jmx.managed;


/**
 * <p>Title: AbstractInvoker</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.managed.AbstractInvoker</code></p>
 */

public abstract class AbstractInvoker implements Invoker {
	/** The invocation target */
	protected Object target = null;
	/** The target logical name */
	protected final String name;
	
	
	
	/**
	 * Creates a new AbstractInvoker
	 * @param name The logical name of the invoker's target object.
	 * Typically a JMX attribute name or operation action
	 */
	public AbstractInvoker(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.managed.Invoker#bindTo(java.lang.Object)
	 */
	@Override
	public Invoker bindTo(Object target) {
		this.target = target;
		return this;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.managed.Invoker#getTarget()
	 */
	@Override
	public Object getTarget() {
		return target;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.managed.Invoker#isBound()
	 */
	@Override
	public boolean isBound() {
		return target!=null;
	}

//	@Override
//	public R invoke(Object... args) {
//		// TODO Auto-generated method stub
//		return null;
//	}

}
