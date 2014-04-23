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

import java.util.Date;

/**
 * <p>Title: DoubleIntervalAccumulator</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.metrics.DoubleIntervalAccumulator</code></p>
 */

public class DoubleIntervalAccumulator implements DoubleIntervalAccumulatorMBean {
	/** The delegate accumulator */
	protected final DoubleIntervalAccumulatorMBean delegate;
	/**
	 * Creates a new DoubleIntervalAccumulator
	 * @param delegate The delegate accumulator
	 */
	public DoubleIntervalAccumulator(DoubleIntervalAccumulatorMBean delegate) {
		this.delegate = delegate;
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
	 * @see org.helios.jmx.metrics.DoubleIntervalAccumulatorMBean#append(double)
	 */
	@Override
	public IntervalAccumulator append(double value) {
		return delegate.append(value);
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
	 * @see org.helios.jmx.metrics.DoubleIntervalAccumulatorMBean#getDoubleMean()
	 */
	@Override
	public double getDoubleMean() {
		return delegate.getDoubleMean();
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
	 * @see org.helios.jmx.metrics.DoubleIntervalAccumulatorMBean#getDoubleMin()
	 */
	@Override
	public double getDoubleMin() {
		return delegate.getDoubleMin();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.DoubleIntervalAccumulatorMBean#getDoubleMax()
	 */
	@Override
	public double getDoubleMax() {
		return delegate.getDoubleMax();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.IntervalAccumulatorMBean#getCount()
	 */
	@Override
	public long getCount() {
		return delegate.getCount();
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.DoubleIntervalAccumulatorMBean#getDoubleAverage()
	 */
	public double getDoubleAverage() {
		return delegate.getDoubleAverage();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.IntervalAccumulatorMBean#getLastSampleTime()
	 */
	@Override
	public long getLastSampleTime() {
		return delegate.getLastSampleTime();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.IntervalAccumulatorMBean#getLastSampleDate()
	 */
	@Override
	public Date getLastSampleDate() {
		return new Date(delegate.getLastSampleTime());
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.IntervalAccumulatorMBean#reset()
	 */
	@Override
	public void reset() {
		delegate.reset();
	}
}
