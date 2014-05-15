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
package org.helios.jmx.util.helpers;

import java.io.ObjectStreamException;

import org.cliffc.high_scale_lib.Counter;

/**
 * <p>Title: NumericCounter</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.util.helpers.NumericCounter</code></p>
 */

public class NumericCounter extends Number {
	/**  */
	private static final long serialVersionUID = -1840368799060992801L;
	/** The delegate counter */
	private final Counter counter = new Counter();
	
	
	/**
	 * Creates a new NumericCounter
	 */
	public NumericCounter() {

	}
	
	/**
	 * Replaces this object with it's current long value when serialized
	 * @return the current long value
	 * @throws ObjectStreamException
	 */
	Object writeReplace() throws ObjectStreamException {
		return counter.get();
	}


	/**
	 * @param x
	 * @see org.cliffc.high_scale_lib.ConcurrentAutoTable#add(long)
	 */
	public void add(long x) {
		counter.add(x);
	}


	/**
	 * 
	 * @see org.cliffc.high_scale_lib.ConcurrentAutoTable#decrement()
	 */
	public void decrement() {
		counter.decrement();
	}


	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return counter.hashCode();
	}


	/**
	 * 
	 * @see org.cliffc.high_scale_lib.ConcurrentAutoTable#increment()
	 */
	public void increment() {
		counter.increment();
	}


	/**
	 * @param x
	 * @see org.cliffc.high_scale_lib.ConcurrentAutoTable#set(long)
	 */
	public void set(long x) {
		counter.set(x);
	}


	/**
	 * @return
	 * @see org.cliffc.high_scale_lib.ConcurrentAutoTable#get()
	 */
	public long get() {
		return counter.get();
	}


	/**
	 * @return
	 * @see org.cliffc.high_scale_lib.ConcurrentAutoTable#intValue()
	 */
	public int intValue() {
		return counter.intValue();
	}


	/**
	 * @return
	 * @see org.cliffc.high_scale_lib.ConcurrentAutoTable#longValue()
	 */
	public long longValue() {
		return counter.longValue();
	}


	/**
	 * @return
	 * @see org.cliffc.high_scale_lib.ConcurrentAutoTable#estimate_get()
	 */
	public long estimate_get() {
		return counter.estimate_get();
	}


	/**
	 * @return
	 * @see org.cliffc.high_scale_lib.ConcurrentAutoTable#toString()
	 */
	public String toString() {
		return counter.toString();
	}


	/**
	 * 
	 * @see org.cliffc.high_scale_lib.ConcurrentAutoTable#print()
	 */
	public void print() {
		counter.print();
	}


	/**
	 * @return
	 * @see org.cliffc.high_scale_lib.ConcurrentAutoTable#internal_size()
	 */
	public int internal_size() {
		return counter.internal_size();
	}


	/**
	 * Returns the internal counter
	 * @return the counter
	 */
	public Counter getCounter() {
		return counter;
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Number#floatValue()
	 */
	@Override
	public float floatValue() {
		return counter.get();
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Number#doubleValue()
	 */
	@Override
	public double doubleValue() {
		return counter.get();		
	}

}
