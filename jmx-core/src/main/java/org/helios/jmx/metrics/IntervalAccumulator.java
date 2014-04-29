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
package org.helios.jmx.metrics;

import java.util.Date;

import javax.management.ObjectName;

import org.helios.jmx.util.helpers.JMXHelper;
import org.helios.jmx.util.unsafe.DeAllocateMe;
import org.helios.jmx.util.unsafe.UnsafeAdapter;

/**
 * <p>Title: IntervalAccumulator</p>
 * <p>Description: A container for interval metrics for a specific joinpoint</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.metrics.IntervalAccumulator</code></p>
 */

public class IntervalAccumulator implements DeAllocateMe, LongIntervalAccumulatorMBean, DoubleIntervalAccumulatorMBean {
	/** The address[0] of the store for this aggregator */
	protected final long[] address = new long[1];
	
	protected final ConcurrentDirectEWMA ewma = new ConcurrentDirectEWMA(50);
	
	/** The offset of the aggregator lock */
	public final static byte XLOCK = 0;							// 8
	/** The offset of the global id */
	public final static byte ID = XLOCK + 1;		// 8
	/** The offset of the last time */
	public final static byte LAST_TIME = ID + UnsafeAdapter.LONG_SIZE;
	/** The offset of the count */
	public final static byte COUNT = LAST_TIME + UnsafeAdapter.LONG_SIZE;
	/** The offset of the min value */
	public final static byte MIN = COUNT + UnsafeAdapter.LONG_SIZE;
	/** The offset of the max value */
	public final static byte MAX = MIN + UnsafeAdapter.LONG_SIZE;
	/** The offset of the average or driver value */
	public final static byte MEAN = MAX + UnsafeAdapter.LONG_SIZE;
	/** The offset of the long/double indicator (double is 0, long is 1) */
	public final static byte DOUBLE_OR_LONG = MEAN + UnsafeAdapter.LONG_SIZE;	
	/** The total memory allocation  */
	public final static byte TOTAL = DOUBLE_OR_LONG + UnsafeAdapter.LONG_SIZE;
	
	
	
	/** A zero byte value */
	public static final byte ZERO_BYTE = 0;
	/** A one byte value */
	public static final byte ONE_BYTE = 1;
	/** Double indicator */
	public static final byte DOUBLE = 0;
	/** Long indicator */
	public static final byte LONG = 1;

	
	public long[][] getAddresses() {
		return new long[][]{address};
	}
	
	/**
	 * Creates a new PeriodAggregatorImpl
	 * @param isDouble true for a double, false for a long
	 * @param packageName The class package name 
	 * @param className The class name
	 * @param methodName The method name
	 * @param signature The method signature
	 * @param altPattern The naming pattern
	 */
	public IntervalAccumulator(boolean isDouble, String packageName, String className, String methodName, Class<?>[] signature, String altPattern) {
		address[0] = UnsafeAdapter.allocateAlignedMemory(TOTAL);
		UnsafeAdapter.registerForDeAlloc(this);
		UnsafeAdapter.setMemory(address[0], TOTAL, ZERO_BYTE);
		UnsafeAdapter.putLong(address[0], UnsafeAdapter.NO_LOCK);
		UnsafeAdapter.putByte(address[0] + DOUBLE_OR_LONG, isDouble ? DOUBLE : LONG);
		reset();
		ObjectName on = JMXHelper.objectName(String.format("%s:class=%s,method=%s", packageName, className, methodName));
		if(isDouble) {
			JMXHelper.registerMBean(new DoubleIntervalAccumulator(this), on);
		} else {
			JMXHelper.registerMBean(new LongIntervalAccumulator(this), on);
		}
	}
	
	/**
	 * Creates a new PeriodAggregatorImpl
	 * @param packageName The class package name 
	 * @param className The class name
	 * @param methodName The method name
	 * @param signature The method signature
	 */
	public IntervalAccumulator(String packageName, String className, String methodName, Class<?>[] signature) {
		this(false, packageName, className, methodName, signature, null);
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.LongIntervalAccumulatorMBean#append(long)
	 */
	@Override
	public IntervalAccumulator append(final long value) {
		ewma.append(value);
		UnsafeAdapter.runInLock(address[0], new Runnable(){
			public void run() {
				final long newCount = increment();
					if(value < UnsafeAdapter.getLong(address[0] + MIN)) UnsafeAdapter.putLong(address[0] + MIN, value);
					if(value > UnsafeAdapter.getLong(address[0] + MAX)) UnsafeAdapter.putLong(address[0] + MAX, value);
					if(newCount==1) {
						UnsafeAdapter.putDouble(address[0] + MEAN, value);
					} else {
						UnsafeAdapter.putDouble(address[0] + MEAN, avgd(UnsafeAdapter.getDouble(address[0] + MEAN), newCount-1, value));
					}
			}
		}); 
		return this;
	}
	
	/**
	 * Processes a new data point into this aggregator
	 * @param value The value to process
	 * @return this aggregator
	 */
	public IntervalAccumulator append(final double value) {
		UnsafeAdapter.runInLock(address[0], new Runnable(){
			public void run() {
				final long newCount = increment();
				if(value < UnsafeAdapter.getDouble(address[0] + MIN)) UnsafeAdapter.putDouble(address[0] + MIN, value);
				if(value > UnsafeAdapter.getDouble(address[0] + MAX)) UnsafeAdapter.putDouble(address[0] + MAX, value);
				if(newCount==1) {
					UnsafeAdapter.putDouble(address[0] + MEAN, value);
				} else {
					UnsafeAdapter.putDouble(address[0] + MEAN, avgd(UnsafeAdapter.getDouble(address[0] + MEAN), newCount-1, value));
				}
			}
		}); 
		return this;
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
	
	
//	/**
//	 * Creates a new PeriodAggregatorImpl which is a direct copy of the passed impl
//	 * @param youButACopy The aggregator to copy
//	 */
//	public ReadOnlyPeriodAggregator readOnly() {
//		return new ReadOnlyPeriodAggregator(address[0]);
//	}
	
	/**
	 * Reset procedure after the flush procedure and init of a new aggregator
	 */
	public void reset() {
		if(isLong()) {
			UnsafeAdapter.putLong(address[0] + MIN, Long.MAX_VALUE);
			UnsafeAdapter.putLong(address[0] + MAX, Long.MIN_VALUE);
		} else {
			UnsafeAdapter.putDouble(address[0] + MIN, Double.MAX_VALUE);
			UnsafeAdapter.putDouble(address[0] + MAX, Double.MIN_VALUE);			
		}
		UnsafeAdapter.putLong(address[0] + COUNT, 0L);
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.LongIntervalAccumulatorMBean#increment()
	 */
	@Override
	public long increment() {
		return increment(1L);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.LongIntervalAccumulatorMBean#increment(long)
	 */
	@Override
	public long increment(long value) {
		long newval = UnsafeAdapter.getLong(address[0] + COUNT) + value;
		UnsafeAdapter.putLong(address[0] + COUNT, newval);
		UnsafeAdapter.putLong(address[0] + LAST_TIME, System.currentTimeMillis());
		return newval;
	}

	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.LongIntervalAccumulatorMBean#getId()
	 */
	@Override
	public long getId() {
		return UnsafeAdapter.getLong(address[0] + ID);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.LongIntervalAccumulatorMBean#getCount()
	 */
	@Override
	public long getCount() {
		return UnsafeAdapter.getLong(address[0] + COUNT);
	}

	/**
	 * Indicates if this accumulator is a long 
	 * @return true if this accumulator is a long, false otherwise
	 */
	public boolean isLong() {
		return UnsafeAdapter.getByte(address[0] + DOUBLE_OR_LONG)==LONG;
	}

	/**
	 * Indicates if this accumulator is a double 
	 * @return true if this accumulator is a double, false otherwise
	 */
	public boolean isDouble() {
		return UnsafeAdapter.getByte(address[0] + DOUBLE_OR_LONG)==DOUBLE;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.DoubleIntervalAccumulatorMBean#getDoubleMean()
	 */
	public double getDoubleMean() {
		return UnsafeAdapter.getDouble(address[0] + MEAN);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.DoubleIntervalAccumulatorMBean#getDoubleMin()
	 */
	public double getDoubleMin() {
		return UnsafeAdapter.getDouble(address[0] + MIN);
	}

	public double getDoubleMax() {
		return UnsafeAdapter.getDouble(address[0] + MAX);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.LongIntervalAccumulatorMBean#getLongMean()
	 */
	@Override
	public long getLongMean() {
		return (long)UnsafeAdapter.getDouble(address[0] + MEAN);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.LongIntervalAccumulatorMBean#getLongMin()
	 */
	@Override
	public long getLongMin() {
		return UnsafeAdapter.getLong(address[0] + MIN);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.LongIntervalAccumulatorMBean#getLongMax()
	 */
	@Override
	public long getLongMax() {
		return UnsafeAdapter.getLong(address[0] + MAX);
	}

	public Number getMean() {
		if(isDouble()) return getDoubleMean();
		return getLongMean();
	}

	public Number getMin() {
		if(isDouble()) return getDoubleMin();
		return getLongMin();
	}

	public Number getMax() {
		if(isDouble()) return getDoubleMax();
		return getLongMax();
	}
	
	

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Period [id=");
		builder.append(getId());
		builder.append(", LastTime=");
		builder.append(getLastSampleTime());
		final long cnt = getCount();
		builder.append(", Count=");
		builder.append(cnt);
		builder.append(", nType=");
		final boolean isd = isDouble();		
		builder.append(isd ? "d" : "l");
		
		if(cnt>0) {
			if(isd) {
				builder.append(", min=").append(getDoubleMin())
				.append(", max=").append(getDoubleMax())
				.append(", mean=").append(getDoubleMean());
			} else {
				builder.append(", min=").append(getLongMin())
				.append(", max=").append(getLongMax())
				.append(", mean=").append(getLongMean());				
			}
		} 		
		builder.append("]");
		return builder.toString();
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (address[0] ^ (address[0] >>> 32));
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IntervalAccumulator other = (IntervalAccumulator) obj;
		if (address[0] != other.address[0])
			return false;
		return true;
	}

	public long getLastSampleTime() {
		return ewma.getLastSample();
	}
	
	public long getLongAverage() {
		return (long)ewma.getAverage();
	}
	

	

	public double getDoubleAverage() {
		return ewma.getAverage();
	}

	@Override
	public Date getLastSampleDate() {
		return null;
	}


}
