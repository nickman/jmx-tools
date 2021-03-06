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
package org.helios.jmx.metrics.ewma;

import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Title: Counter</p>
 * <p>Description: An {@link IMetricSetter} implementing atomic long</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.metrics.ewma.Counter</code></p>
 */

public class Counter extends AtomicLong implements IMetricSetter, CounterMBean {

	/**  */
	private static final long serialVersionUID = -6511119248010799524L;
	
	/** The initial value the counter starts with and resets to */
	protected final long intialValue;
	
	/** The error counter */
	private final AtomicLong errors = new AtomicLong(0L);
	
	/**
	 * Creates a new Counter with an initial value of 0.
	 */
	public Counter() {
		this(0L);
	}

	/**
	 * Creates a new Counter
	 * @param initialValue The initial value
	 */
	public Counter(long initialValue) {
		super(initialValue);
		this.intialValue = initialValue;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.CounterMBean#incr()
	 */
	@Override
	public void incr() {
		incrementAndGet();		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.CounterMBean#err()
	 */
	@Override
	public void err() {
		errors.incrementAndGet();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.CounterMBean#append(double)
	 */
	@Override
	public void append(double value) {
		set((long)value);		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.CounterMBean#append(long)
	 */
	@Override
	public void append(long value) {
		set(value);		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.CounterMBean#append(int)
	 */
	@Override
	public void append(int value) {
		set(value);		
	}

	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.CounterMBean#reset()
	 */
	@Override
	public void reset() {
		set(intialValue);
		errors.set(0L);		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.CounterMBean#getErrors()
	 */	
	@Override
	public long getErrors() {
		return errors.get();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.CounterMBean#getValue()
	 */
	@Override
	public long getValue() {
		return get();
	}

}
