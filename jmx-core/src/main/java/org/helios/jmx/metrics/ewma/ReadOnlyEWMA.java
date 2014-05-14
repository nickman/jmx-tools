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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;

import org.helios.jmx.annotation.Reflector;
import org.helios.jmx.managed.Invoker;

/**
 * <p>Title: ReadOnlyEWMA</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.metrics.ewma.ReadOnlyEWMA</code></p>
 */

public class ReadOnlyEWMA implements DirectEWMAMBean {
	/**  */
	private static final long serialVersionUID = -9209212906780703253L;
	private long lastSample = -1, count = -1, errors = -1, window = -1;
	private double min = -1D, max = -1D, avg = -1D, mean = -1D;
	
	/** The composite type for this class */
	private static final CompositeType openType;
	
	/** A map of invokers keyed by the corresponding open type key  */
	private static final Map<String, Invoker> invokers;
	
	static {
		Map<String, Invoker> invs = new LinkedHashMap<String, Invoker>(); 
		openType = Reflector.getCompositeTypeForAnnotatedClass(ReadOnlyEWMA.class, invs);
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
	private static Object invoke(ReadOnlyEWMA instance, String name) {
		if(name==null || instance==null) return null;
		Invoker invoker = invokers.get(name);
		if(invoker==null) return null;
		return invoker.bindTo(instance).invoke();		
	}
	
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

	/**
	 * Replaces this read only ewma witha composite data representation when it is serialized 
	 * @return a composite data instance
	 * @throws ObjectStreamException
	 */
	Object writeReplace() throws ObjectStreamException {
		return toCompositeData(openType);
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

}
