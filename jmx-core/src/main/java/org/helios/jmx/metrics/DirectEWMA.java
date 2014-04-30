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

import javax.management.ObjectName;

import org.helios.jmx.util.helpers.JMXHelper;
import org.helios.jmx.util.unsafe.DeAllocateMe;
import org.helios.jmx.util.unsafe.UnsafeAdapter;

/**
 * <p>Title: DirectEWMA</p>
 * <p>Description: An Exponential Weighted Moving Average calculator using direct memory allocation. Not thread-safe.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.rindle.period.impl.DirectEWMA</code></p>
 */

public class DirectEWMA implements DeAllocateMe, DirectEWMAMBean, EWMAAppender {
	/** The address of the memory allocation */
	protected final long[] address = new long[1];
	
	/** The offset of the length of the sliding window in ms. */
	public final static byte WINDOW = 0;							
	/** The offset of the last sample timestamp in ms. */
	public final static byte LAST_SAMPLE = WINDOW + UnsafeAdapter.LONG_SIZE;
	/** The offset of the rolling average */
	public final static byte AVERAGE = LAST_SAMPLE + UnsafeAdapter.LONG_SIZE;
	/** The offset of the minimum value */
	public final static byte MINIMUM = LAST_SAMPLE + UnsafeAdapter.DOUBLE_SIZE;
	/** The offset of the maximum value */
	public final static byte MAXIMUM = MINIMUM + UnsafeAdapter.DOUBLE_SIZE;
	/** The offset of the mean value */
	public final static byte MEAN = MAXIMUM + UnsafeAdapter.DOUBLE_SIZE;
	/** The offset of the count value */
	public final static byte COUNT = MEAN + UnsafeAdapter.LONG_SIZE;
	/** The offset of the concurrency value */
	public final static byte CONCURRENCY = COUNT + UnsafeAdapter.LONG_SIZE;
	/** The offset of the error count */
	public final static byte ERRORS = CONCURRENCY + UnsafeAdapter.INT_SIZE;
	
	/** The total memory allocation  */
	public final static byte TOTAL = ERRORS + UnsafeAdapter.LONG_SIZE;
	
	/**
	 * Creates a new DirectEWMA
	 * @param windowSize The length of the sliding window in ms.
	 * @param objectName The object name to register the EWMA with
	 */
	public DirectEWMA(long windowSize, ObjectName objectName) {
		this(windowSize, TOTAL, objectName);
	}
	
	/**
	 * Creates a new DirectEWMA
	 * @param windowSize The length of the sliding window in ms.
	 */
	public DirectEWMA(long windowSize) {
		this(windowSize, TOTAL);
	}
	
	
	/**
	 * Creates a new DirectEWMA to represent the stats for an instrumented method
	 * @param windowSize The length of the sliding window in ms.
	 * @param memSize The memory allocation size
	 */
	protected DirectEWMA(long windowSize, long memSize) {
		address[0] = UnsafeAdapter.allocateAlignedMemory(memSize);
		UnsafeAdapter.putLong(address[0] + WINDOW, windowSize);
		UnsafeAdapter.registerForDeAlloc(this);
		reset();		
	}
	
	/**
	 * Creates a new DirectEWMA to represent the stats for an instrumented method
	 * @param windowSize The length of the sliding window in ms.
	 * @param memSize The memory allocation size
	 * @param objectName The object name to register the EWMA with
	 */
	protected DirectEWMA(long windowSize, long memSize, ObjectName objectName) {
		this(windowSize, memSize);
		JMXHelper.registerMBean(this, objectName);
	}	
	
	/**
	 * Returns an appender to this EWMA
	 * @return an appender to this EWMA
	 */
	public EWMAAppender getAppender() {
		return this;
	}

	
	@Override
	public void reset() {
		UnsafeAdapter.putLong(address[0] + LAST_SAMPLE, 0L);
		UnsafeAdapter.putDouble(address[0] + AVERAGE, 0D);
		UnsafeAdapter.putDouble(address[0] + MINIMUM, 0D);
		UnsafeAdapter.putDouble(address[0] + MAXIMUM, 0D);
		UnsafeAdapter.putDouble(address[0] + MEAN, 0D);
		UnsafeAdapter.putLong(address[0] + COUNT, 0L);
		UnsafeAdapter.putLong(address[0] + ERRORS, 0L);		
		UnsafeAdapter.putInt(address[0] + CONCURRENCY, 0);
	}
	
	

	@Override
	public long getLastSample() {
		return UnsafeAdapter.getLong(address[0] + LAST_SAMPLE);
	}
	

	@Override
	public double getAverage() {
		return UnsafeAdapter.getDouble(address[0] + AVERAGE);
	}
	

	@Override
	public double getMinimum() {
		return UnsafeAdapter.getDouble(address[0] + MINIMUM);
	}
	

	@Override
	public double getMaximum() {
		return UnsafeAdapter.getDouble(address[0] + MAXIMUM);
	}	

	@Override
	public double getMean() {
		return UnsafeAdapter.getDouble(address[0] + MEAN);
	}
	
	

	@Override
	public long getCount() {
		return UnsafeAdapter.getLong(address[0] + COUNT);
	}	
	

	@Override
	public long getWindow() {
		return UnsafeAdapter.getLong(address[0] + WINDOW);
	}
	

	@Override
	public void append(double sample) {
		final long now = System.currentTimeMillis();
		final long lastSample = getLastSample(); 
		if(lastSample == 0L) {
			UnsafeAdapter.putDouble(address[0] + AVERAGE, sample);
			UnsafeAdapter.putLong(address[0] + LAST_SAMPLE, now);
		} else {
			long deltaTime = now - lastSample;
			double coeff = Math.exp(-1.0 * ((double)deltaTime / getWindow()));
			UnsafeAdapter.putDouble(address[0] + AVERAGE, (1.0 - coeff) * sample + coeff * getAverage());
			UnsafeAdapter.putLong(address[0] + LAST_SAMPLE, now);
		}	
		final long newCount = increment();
		if(newCount==1) {
			UnsafeAdapter.putDouble(address[0] + MINIMUM, sample);
			UnsafeAdapter.putDouble(address[0] + MAXIMUM, sample);
			UnsafeAdapter.putDouble(address[0] + MEAN, sample);
		} else {
			if(sample < UnsafeAdapter.getDouble(address[0] + MINIMUM)) UnsafeAdapter.putDouble(address[0] + MINIMUM, sample);
			if(sample > UnsafeAdapter.getDouble(address[0] + MAXIMUM)) UnsafeAdapter.putDouble(address[0] + MAXIMUM, sample);
			UnsafeAdapter.putDouble(address[0] + MEAN, avgd(UnsafeAdapter.getDouble(address[0] + MEAN), newCount-1, sample));
		}
	}
	
	/**
	 * Calcs a double average incorporating a new value
	 * using <b><code>(prev_avg*cnt + newval)/(cnt+1)</code></b>
	 * @param prev_avg The pre-average
	 * @param cnt The pre-count
	 * @param newval The new value
	 * @return the average
	 */
	public static double avgd(double prev_avg, double cnt, double newval) {		
		return (prev_avg*cnt + newval)/(cnt+1);
	}	
	

	@Override
	public long increment() {
		return increment(1L);
	}


	@Override
	public long increment(long value) {
		long newval = UnsafeAdapter.getLong(address[0] + COUNT) + value;
		UnsafeAdapter.putLong(address[0] + COUNT, newval);
		return newval;
	}
	
//	public long incrementErrors() {
//		long newval = UnsafeAdapter.getLong(address[0] + ERRORS) + 1;
//		UnsafeAdapter.putLong(address[0] + ERRORS, newval);
//		return newval;	
//		
//		return UnsafeAdapter.getLong(address[0] + ERRORS);
//	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder("EWMA [");
		b.append("ts:").append(new Date(getLastSample()));
		b.append(", avg:").append(getAverage());
		b.append("]");		
		return b.append("]").toString();
	}


	@Override
	public long[][] getAddresses() {
		return new long[][]{address};
	}


	@Override
	public long error() {
		long newval = UnsafeAdapter.getLong(address[0] + ERRORS) + 1;
		UnsafeAdapter.putLong(address[0] + ERRORS, newval);
		return newval;	
	}

	@Override
	public long getErrors() {
		return UnsafeAdapter.getLong(address[0] + ERRORS);
	}


}