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

import org.helios.jmx.util.unsafe.DeAllocateMe;
import org.helios.jmx.util.unsafe.UnsafeAdapter;

/**
 * <p>Title: DirectEWMA</p>
 * <p>Description: An Exponential Weighted Moving Average calculator using direct memory allocation. Not thread-safe.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.rindle.period.impl.DirectEWMA</code></p>
 */

public class DirectEWMA implements DeAllocateMe {
	/** The address of the memory allocation */
	protected final long[] address = new long[1];
	
	/** The offset of the length of the sliding window in ms. */
	public final static byte WINDOW = 0;							
	/** The offset of the last sample timestamp in ms. */
	public final static byte LAST_SAMPLE = WINDOW + UnsafeAdapter.LONG_SIZE;
	/** The offset of the rolling average */
	public final static byte AVERAGE = LAST_SAMPLE + UnsafeAdapter.LONG_SIZE;
	/** The total memory allocation  */
	public final static byte TOTAL = LAST_SAMPLE + UnsafeAdapter.DOUBLE_SIZE;
	
	/**
	 * Creates a new DirectEWMA
	 * @param windowSize The length of the sliding window in ms.
	 */
	public DirectEWMA(long windowSize) {
		this(windowSize, TOTAL);
	}
	
	/**
	 * Creates a new DirectEWMA
	 * @param windowSize The length of the sliding window in ms.
	 * @param memSize The memory allocation size
	 */
	protected DirectEWMA(long windowSize, long memSize) {
		address[0] = UnsafeAdapter.allocateAlignedMemory(memSize);
		UnsafeAdapter.putLong(address[0] + WINDOW, windowSize);
		UnsafeAdapter.putLong(address[0] + LAST_SAMPLE, 0L);
		UnsafeAdapter.putLong(address[0] + AVERAGE, 0L);
		UnsafeAdapter.registerForDeAlloc(this);
	}
	
	
	/**
	 * Returns the timestamp of the last sample as a long UTC.
	 * @return the timestamp of the last sample 
	 */
	public long getLastSample() {
		return UnsafeAdapter.getLong(address[0] + LAST_SAMPLE);
	}
	
	/**
	 * Returns the last computed average.
	 * @return the last computed average 
	 */
	public double getAverage() {
		return UnsafeAdapter.getDouble(address[0] + AVERAGE);
	}
	
	/**
	 * Returns the window size in ms.
	 * @return the window size  
	 */
	public long getWindow() {
		return UnsafeAdapter.getLong(address[0] + WINDOW);
	}
	
	/**
	 * Appends a new double sample
	 * @param sample a new double sample
	 */
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
	}
	
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

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.util.unsafe.DeAllocateMe#getAddresses()
	 */
	@Override
	public long[][] getAddresses() {
		return new long[][]{address};
	}

}
