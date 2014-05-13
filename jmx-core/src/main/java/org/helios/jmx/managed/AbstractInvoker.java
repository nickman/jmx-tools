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

public abstract class AbstractInvoker<T, R> implements Invoker<T, R> {
	/** The invocation target */
	protected T target = null;
	
	
	
	/**
	 * Creates a new AbstractInvoker
	 */
	public AbstractInvoker() {
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.managed.Invoker#bindTo(java.lang.Object)
	 */
	@Override
	public Invoker<T, R> bindTo(T target) {
		this.target = target;
		return this;
	}
	
	public T getTarget() {
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
