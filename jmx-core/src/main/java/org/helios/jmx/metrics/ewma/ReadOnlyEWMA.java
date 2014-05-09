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
package org.helios.jmx.metrics.ewma;

import java.io.ObjectStreamException;
import java.io.Serializable;

import org.helios.jmx.opentypes.OpenTypeFactory;

/**
 * <p>Title: ReadOnlyEWMA</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.metrics.ewma.ReadOnlyEWMA</code></p>
 */

public class ReadOnlyEWMA implements DirectEWMAMBean, Serializable {
	/**  */
	private static final long serialVersionUID = -9209212906780703253L;
	private long lastSample = -1, count = -1, errors = -1, window = -1;
	private double min = -1D, max = -1D, avg = -1D, mean = -1D;
	/**
	 * Creates a new ReadOnlyEWMA
	 * @param ewma The dynamic EWMA that this read only is a snapshot of
	 */
	public ReadOnlyEWMA(DirectEWMA ewma) {
		lastSample = ewma.getLastSample();
		count = ewma.getCount();
		errors = ewma.getErrors();
		window = ewma.getWindow();
		min = ewma.getMinimum();
		max = ewma.getMaximum();
		avg = ewma.getAverage();
		mean = ewma.getMean();
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMAMBean#reset()
	 */
	@Override
	public void reset() {
		/* No Op */
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMAMBean#getLastSample()
	 */
	@Override
	public long getLastSample() {
		return lastSample;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMAMBean#getAverage()
	 */
	@Override
	public double getAverage() {
		return avg;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMAMBean#getMinimum()
	 */
	@Override
	public double getMinimum() {
		return min;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMAMBean#getMaximum()
	 */
	@Override
	public double getMaximum() {
		return max;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMAMBean#getMean()
	 */
	@Override
	public double getMean() {
		return mean;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMAMBean#getCount()
	 */
	@Override
	public long getCount() {
		return count;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMAMBean#getErrors()
	 */
	@Override
	public long getErrors() {
		return errors;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMAMBean#getWindow()
	 */
	@Override
	public long getWindow() {
		return window;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMAMBean#append(double)
	 */
	@Override
	public void append(double sample) {
		/* No Op */
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMAMBean#increment()
	 */
	@Override
	public long increment() {
		/* No Op */
		return count;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMAMBean#increment(long)
	 */
	@Override
	public long increment(long value) {
		/* No Op */
		return count;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMAMBean#error()
	 */
	@Override
	public long error() {
		/* No Op */
		return errors;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ReadOnlyEWMA [lastSample=");
		builder.append(lastSample);
		builder.append(", count=");
		builder.append(count);
		builder.append(", errors=");
		builder.append(errors);
		builder.append(", window=");
		builder.append(window);
		builder.append(", min=");
		builder.append(min);
		builder.append(", max=");
		builder.append(max);
		builder.append(", avg=");
		builder.append(avg);
		builder.append(", mean=");
		builder.append(mean);
		builder.append("]");
		return builder.toString();
	}

}
