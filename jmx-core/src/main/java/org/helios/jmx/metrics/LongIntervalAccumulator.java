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
package org.helios.jmx.metrics;

/**
 * <p>Title: LongIntervalAccumulator</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.metrics.LongIntervalAccumulator</code></p>
 */

public class LongIntervalAccumulator implements LongIntervalAccumulatorMBean {
	/** The underlying accumulator */
	private final LongIntervalAccumulatorMBean delegate;
	
	
	/**
	 * Creates a new LongIntervalAccumulator
	 * @param delegate The underlying accumulator
	 */
	public LongIntervalAccumulator(LongIntervalAccumulatorMBean delegate) {
		this.delegate = delegate;
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.LongIntervalAccumulatorMBean#append(long)
	 */
	@Override
	public IntervalAccumulator append(long value) {
		return delegate.append(value);
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.LongIntervalAccumulatorMBean#getLongMean()
	 */
	@Override
	public long getLongMean() {
		return delegate.getLongMean();
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.LongIntervalAccumulatorMBean#getLongMin()
	 */
	@Override
	public long getLongMin() {
		return delegate.getLongMin();
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.LongIntervalAccumulatorMBean#getLongMax()
	 */
	@Override
	public long getLongMax() {
		return delegate.getLongMax();
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.IntervalAccumulatorMBean#increment()
	 */
	@Override
	public long increment() {
		return delegate.increment();
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.IntervalAccumulatorMBean#increment(long)
	 */
	@Override
	public long increment(long value) {
		return delegate.increment(value);
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.IntervalAccumulatorMBean#getId()
	 */
	@Override
	public long getId() {
		return delegate.getId();
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.IntervalAccumulatorMBean#getLastTime()
	 */
	@Override
	public long getLastTime() {
		return delegate.getLastTime();
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.IntervalAccumulatorMBean#getCount()
	 */
	@Override
	public long getCount() {
		return delegate.getCount();
	}


}
