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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeDataView;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;

import org.helios.jmx.annotation.ManagedAttribute;
import org.helios.jmx.annotation.Reflector;
import org.helios.jmx.managed.Invoker;
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

public class DirectEWMA implements DeAllocateMe, DirectEWMAMBean, IMetricSetter {
	/**  */
	private static final long serialVersionUID = 4057837003464578145L;

	/** The address of the memory allocation */
	protected final long[] address = new long[1];
	
	/** The offset of the length of the sliding window in ms. */
	public final static byte WINDOW = 0;							
	/** The offset of the last sample timestamp in ms. */
	public final static byte LAST_SAMPLE = WINDOW + UnsafeAdapter.LONG_SIZE;
	/** The most recently appended value */
	public final static byte LAST_VALUE = LAST_SAMPLE + UnsafeAdapter.LONG_SIZE;
	/** The offset of the rolling average */
	public final static byte AVERAGE = LAST_VALUE + UnsafeAdapter.DOUBLE_SIZE;
	/** The offset of the minimum value */
	public final static byte MINIMUM = AVERAGE + UnsafeAdapter.DOUBLE_SIZE;
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
	
	/** The composite type for this class */
	private static final CompositeType openType;
	
	/** A map of invokers keyed by the corresponding open type key  */
	private static final Map<String, Invoker> invokers;
	
	static {
		Map<String, Invoker> invs = new LinkedHashMap<String, Invoker>(); 
		openType = Reflector.getCompositeTypeForAnnotatedClass(DirectEWMA.class, invs);
		Map<String, Invoker> sizedInvs = new LinkedHashMap<String, Invoker>(invs.size()+1, 1F);
		sizedInvs.putAll(invs);
		invokers = Collections.unmodifiableMap(sizedInvs);		
	}
	
	/**
	 * Invokes the named invoker, retruning the resulting value, 
	 * or null if the passed name was null, or no invoker was found.
	 * @param instance The instance to invoke on
	 * @param name The name of the invoker to execute
	 * @return the invoker's returned value or null
	 */
	private static Object invoke(DirectEWMA instance, String name) {
		if(name==null || instance==null) return null;
		Invoker invoker = invokers.get(name);
		if(invoker==null) return null;
		return invoker.bindTo(instance).invoke();		
	}
	
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
	 * Replaces this objects with a serializable {@link ReadOnlyEWMA} when it is written to a serialization stream
	 * @return a {@link ReadOnlyEWMA} representing a snapshot of this ewma.
	 * @throws ObjectStreamException thrown on error writing to the object output stream
	 */
	Object writeReplace() throws ObjectStreamException {
		return new ReadOnlyEWMA(this);
	}	
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.openmbean.CompositeDataView#toCompositeData(javax.management.openmbean.CompositeType)
	 */
	@Override
	public CompositeData toCompositeData(CompositeType ct) {
		try {
			return new CompositeDataSupport(openType, invokers.keySet().toArray(new String[0]), values().toArray(new Object[0])) ;
		} catch (OpenDataException ex) {
			throw new RuntimeException(ex);
		}		
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
	public EWMAAppenderMBean getAppender() {
		return this;
	}

	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMAMBean#reset()
	 */
	@Override
	public void reset() {
//		UnsafeAdapter.putLong(address[0] + LAST_SAMPLE, 0L);
		UnsafeAdapter.putDouble(address[0] + LAST_VALUE, 0D);
		UnsafeAdapter.putDouble(address[0] + AVERAGE, 0D);
		UnsafeAdapter.putDouble(address[0] + MINIMUM, 0D);
		UnsafeAdapter.putDouble(address[0] + MAXIMUM, 0D);
		UnsafeAdapter.putDouble(address[0] + MEAN, 0D);
		UnsafeAdapter.putLong(address[0] + COUNT, 0L);
		UnsafeAdapter.putLong(address[0] + ERRORS, 0L);		
		UnsafeAdapter.putInt(address[0] + CONCURRENCY, 0);
	}
	
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMAMBean#getLastSample()
	 */
	@Override
	public long getLastSample() {
		return UnsafeAdapter.getLong(address[0] + LAST_SAMPLE);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMAMBean#getLastValue()
	 */
	public double getLastValue() {
		return UnsafeAdapter.getDouble(address[0] + LAST_VALUE);
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMAMBean#getAverage()
	 */
	@Override
	public double getAverage() {
		return UnsafeAdapter.getDouble(address[0] + AVERAGE);
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMAMBean#getMinimum()
	 */
	@Override
	public double getMinimum() {
		return UnsafeAdapter.getDouble(address[0] + MINIMUM);
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMAMBean#getMaximum()
	 */
	@Override
	public double getMaximum() {
		return UnsafeAdapter.getDouble(address[0] + MAXIMUM);
	}	

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMAMBean#getMean()
	 */
	@Override
	public double getMean() {
		return UnsafeAdapter.getDouble(address[0] + MEAN);
	}
	
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMAMBean#getCount()
	 */
	@Override
	public long getCount() {
		return UnsafeAdapter.getLong(address[0] + COUNT);
	}	
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMAMBean#getWindow()
	 */
	@Override
	public long getWindow() {
		return UnsafeAdapter.getLong(address[0] + WINDOW);
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMAMBean#append(double)
	 */
	@Override
	public void append(double sample) {
		final long now = System.currentTimeMillis();
		final long lastSample = getLastSample(); 
		UnsafeAdapter.putDouble(address[0] + LAST_VALUE, sample);
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
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMAMBean#increment()
	 */
	@Override
	public long increment() {
		return increment(1L);
	}
	


	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMAMBean#increment(long)
	 */
	@Override
	public long increment(long value) {
		long newval = UnsafeAdapter.getLong(address[0] + COUNT) + value;
		UnsafeAdapter.putLong(address[0] + COUNT, newval);
		return newval;
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder("EWMA [");
		final long count = getCount();
		if(count>0) {
			b.append("ts:").append(new Date(getLastSample()));
		}
		b.append(", min:").append(getMinimum());
		b.append(", max:").append(getMaximum());
		b.append(", count:").append(getCount());
		b.append(", mean:").append(getMean());
		b.append(", avg:").append(getAverage());
		b.append(", last:").append(getLastValue());
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


	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMAMBean#error()
	 */
	@Override
	public long error() {
		long newval = UnsafeAdapter.getLong(address[0] + ERRORS) + 1;
		UnsafeAdapter.putLong(address[0] + ERRORS, newval);
		return newval;	
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMAMBean#getErrors()
	 */
	@Override
	public long getErrors() {
		return UnsafeAdapter.getLong(address[0] + ERRORS);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.IMetricSetter#append(long)
	 */
	@Override
	public void append(long value) {
		append((double)value);
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.IMetricSetter#append(int)
	 */
	@Override
	public void append(int value) {
		append((double)value);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.IMetricSetter#err()
	 */
	@Override
	public void err() {
		error();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.IMetricSetter#incr()
	 */
	@Override
	public void incr() {
		increment(1L);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.openmbean.CompositeData#getCompositeType()
	 */
	@Override
	public CompositeType getCompositeType() {
		return openType;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.openmbean.CompositeData#get(java.lang.String)
	 */
	@Override
	public Object get(String key) {
		return invoke(this, key);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.openmbean.CompositeData#getAll(java.lang.String[])
	 */
	@Override
	public Object[] getAll(String[] keys) {
		Object[] results = new Object[keys.length];
		for(int i = 0; i < keys.length; i++) {
			results[i] = invoke(this, keys[i]);
		}
		return results;		
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.openmbean.CompositeData#containsKey(java.lang.String)
	 */
	@Override
	public boolean containsKey(String key) {
		if(key==null) return false;
		return invokers.containsValue(key);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.openmbean.CompositeData#containsValue(java.lang.Object)
	 */
	@Override
	public boolean containsValue(Object value) {
		if(value==null) return false;
		for(Invoker invoker: invokers.values()) {
			Object result = invoker.bindTo(this).invoke();
			if(value.equals(result)) return true;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.openmbean.CompositeData#values()
	 */
	@Override
	public Collection<?> values() {
		List<Object> values = new ArrayList<Object>(invokers.size());
		for(Invoker invoker: invokers.values()) {
			values.add(invoker.bindTo(this).invoke());
		}
		return values;
	}

}