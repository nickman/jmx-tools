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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeType;

import org.helios.jmx.annotation.Reflector;
import org.helios.jmx.managed.Invoker;
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
	
	/** The composite type for this class */
	private static final CompositeType concurrentOpenType;
	
	/** A map of invokers keyed by the corresponding open type key  */
	private static final Map<String, Invoker> concurrentInvokers;
	
	static {
		Map<String, Invoker> invs = new LinkedHashMap<String, Invoker>(); 
		concurrentOpenType = Reflector.getCompositeTypeForAnnotatedClass(ConcurrentDirectEWMA.class, invs);
		Map<String, Invoker> sizedInvs = new LinkedHashMap<String, Invoker>(invs.size()+1, 1F);
		sizedInvs.putAll(invs);
		concurrentInvokers = Collections.unmodifiableMap(sizedInvs);		
	}
	
	/**
	 * Invokes the named invoker, retruning the resulting value, 
	 * or null if the passed name was null, or no invoker was found.
	 * @param instance The instance to invoke on
	 * @param name The name of the invoker to execute
	 * @return the invoker's returned value or null
	 */
	private static Object invoke(ConcurrentDirectEWMAMBean instance, String name) {
		if(name==null || instance==null) return null;
		Invoker invoker = concurrentInvokers.get(name);
		if(invoker==null) return null;
		return invoker.bindTo(instance).invoke();		
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
	
	/**
	 * Returns the most recently appended value.
	 * @return the most recently appended value 
	 */
	public double getLastValue() {
		lock.xlock();
		try {
			return UnsafeAdapter.getDouble(address[0] + LAST_VALUE);
		} finally {
			lock.xunlock();
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMA#error()
	 */
	@Override
	public long error() {
		lock.xlock();
		try {
			return super.error();
		} finally {
			lock.xunlock();
		}		
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMA#reset()
	 */
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


	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMA#getErrors()
	 */
	@Override
	public long getErrors() {
		lock.xlock();
		try {
			return UnsafeAdapter.getLong(address[0] + ERRORS);
		} finally {
			lock.xunlock();
		}
	}	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMA#append(double)
	 */
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
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMA#getAverage()
	 */
	public double getAverage() {
		lock.xlock();
		try {
			return super.getAverage();
		} finally {
			lock.xunlock();
		}
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMA#getCount()
	 */
	@Override
	public long getCount() {
		lock.xlock();
		try {
			return super.getCount();
		} finally {
			lock.xunlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMA#getMaximum()
	 */
	@Override
	public double getMaximum() {
		lock.xlock();
		try {
			return super.getMaximum();
		} finally {
			lock.xunlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMA#getMean()
	 */
	@Override
	public double getMean() {
		lock.xlock();
		try {
			return super.getMean();
		} finally {
			lock.xunlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.DirectEWMA#getMinimum()
	 */
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
		lock.xlock();
		try {
			return super.toString();
		} finally {
			lock.xunlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.IMetricSetter#append(long)
	 */
	@Override
	public void append(long value) {
		lock.xlock();
		try {
			super.append((double)value);
		} finally {
			lock.xunlock();
		}		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.IMetricSetter#append(int)
	 */
	@Override
	public void append(int value) {
		lock.xlock();
		try {
			super.append((double)value);
		} finally {
			lock.xunlock();
		}		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.IMetricSetter#err()
	 */
	@Override
	public void err() {
		lock.xlock();
		try {
			error();
		} finally {
			lock.xunlock();
		}					
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.metrics.ewma.IMetricSetter#incr()
	 */
	@Override
	public void incr() {
		lock.xlock();
		try {
			super.incr();
		} finally {
			lock.xunlock();
		}					
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.openmbean.CompositeData#getCompositeType()
	 */
	@Override
	public CompositeType getCompositeType() {
		return concurrentOpenType;
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
		lock.xlock();
		try {
			Object[] results = new Object[keys.length];
			for(int i = 0; i < keys.length; i++) {
				results[i] = invoke(this, keys[i]);
			}
			return results;		
		} finally {
			lock.xunlock();
		}					
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.openmbean.CompositeData#containsKey(java.lang.String)
	 */
	@Override
	public boolean containsKey(String key) {
		if(key==null) return false;
		return concurrentInvokers.containsValue(key);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.openmbean.CompositeData#containsValue(java.lang.Object)
	 */
	@Override
	public boolean containsValue(Object value) {
		lock.xlock();
		try {
			if(value==null) return false;
			for(Invoker invoker: concurrentInvokers.values()) {
				Object result = invoker.invoke();
				if(value.equals(result)) return true;
			}
			return false;
		} finally {
			lock.xunlock();
		}					
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.openmbean.CompositeData#values()
	 */
	@Override
	public Collection<?> values() {
		lock.xlock();
		try {
			List<Object> values = new ArrayList<Object>(concurrentInvokers.size());
			for(Invoker invoker: concurrentInvokers.values()) {
				values.add(invoker.invoke());
			}
			return values;
		} finally {
			lock.xunlock();
		}					
	}
	
}
