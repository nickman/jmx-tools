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

import org.helios.jmx.util.unsafe.UnsafeAdapter;
import org.helios.jmx.util.unsafe.UnsafeAdapter.SpinLock;

/**
 * <p>Title: ConcurrentDirectEWMA</p>
 * <p>Description: A thread safe version of {@link DirectEWMA} which is safe to be accessed by concurrent threads at the small cost of a spin lock on each access.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.rindle.period.impl.ConcurrentDirectEWMA</code></p>
 */

public class ConcurrentDirectEWMA extends DirectEWMA implements ConcurrentDirectEWMAMBean {
	/** The spin lock to guard the EWMA */
	protected final SpinLock lock = UnsafeAdapter.allocateSpinLock();

	/**
	 * Creates a new ConcurrentDirectEWMA
	 * @param windowSize The length of the sliding window in ms.
	 * @param objectName The object name to register the EWMA with
	 */
	public ConcurrentDirectEWMA(long windowSize, ObjectName objectName) {
		super(windowSize, TOTAL, objectName);		
	}

	/**
	 * Creates a new ConcurrentDirectEWMA
	 * @param windowSize The length of the sliding window in ms.
	 */
	public ConcurrentDirectEWMA(long windowSize) {
		super(windowSize, TOTAL);		
	}
	

	/**
	 * Returns the timestamp of the last sample as a long UTC.
	 * @return the timestamp of the last sample 
	 */
	public long getLastSample() {
		lock.xlock();
		try {
			return UnsafeAdapter.getLong(address[0] + LAST_SAMPLE);
		} finally {
			lock.xunlock();
		}
	}
	

	@Override
	public long error() {
		lock.xlock();
		try {
			return super.error();
		} finally {
			lock.xunlock();
		}		
	}
	

	@Override
	public void reset() {
		if(lock==null) {
			super.reset();
			return;
		}
		lock.xlock();
		try {
			super.reset();
		} finally {
			lock.xunlock();
		}				
	}


	@Override
	public long getErrors() {
		lock.xlock();
		try {
			return UnsafeAdapter.getLong(address[0] + ERRORS);
		} finally {
			lock.xunlock();
		}
	}	
	
	@Override
	public void append(double sample) {
		lock.xlock();
		try {
			super.append(sample);
		} finally {
			lock.xunlock();
		}
	}
	
	/**
	 * Returns the last computed average.
	 * @return the last computed average 
	 */
	public double getAverage() {
		lock.xlock();
		try {
			return super.getAverage();
		} finally {
			lock.xunlock();
		}
	}
	

	@Override
	public long getCount() {
		lock.xlock();
		try {
			return super.getCount();
		} finally {
			lock.xunlock();
		}
	}
	
	@Override
	public double getMaximum() {
		lock.xlock();
		try {
			return super.getMaximum();
		} finally {
			lock.xunlock();
		}
	}
	
	@Override
	public double getMean() {
		lock.xlock();
		try {
			return super.getMean();
		} finally {
			lock.xunlock();
		}
	}
	
	@Override
	public double getMinimum() {
		lock.xlock();
		try {
			return super.getMinimum();
		} finally {
			lock.xunlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder b = new StringBuilder("EWMA [");
		lock.xlock();
		try {
			b.append("ts:").append(new Date(UnsafeAdapter.getLong(address[0] + LAST_SAMPLE)));
			b.append(", avg:").append(UnsafeAdapter.getDouble(address[0] + AVERAGE));				
		} finally {
			lock.xunlock();
		}
		return b.append("]").toString();
	}
	
	
}
