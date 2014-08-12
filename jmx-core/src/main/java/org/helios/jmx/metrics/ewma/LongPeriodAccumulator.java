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
package org.helios.jmx.metrics.ewma;

import java.util.Date;
import java.util.Random;

import org.helios.jmx.util.helpers.SystemClock;
import org.helios.jmx.util.unsafe.DeAllocateMe;
import org.helios.jmx.util.unsafe.UnsafeAdapter;
import org.helios.jmx.util.unsafe.UnsafeAdapter.SpinLock;

/**
 * <p>Title: LongPeriodAccumulator</p>
 * <p>Description: An accumulator of long values for the duration of a period to calculate stats that require all raw values.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.metrics.ewma.LongPeriodAccumulator</code></p>
 */

public class LongPeriodAccumulator implements DeAllocateMe {
	/** The offset of the last reset timestamp */
	public final static byte LAST_RESET = 0;
	/** The offset of the current allocated capacity */
	public final static byte CAPACITY = LAST_RESET + UnsafeAdapter.LONG_SIZE;
	/** The offset of the current size */
	public final static byte ALLOCATED = CAPACITY + UnsafeAdapter.INT_SIZE;
	/** The offset of the initial size */
	public final static byte INITIAL_SIZE = ALLOCATED + UnsafeAdapter.INT_SIZE;							
	/** The offset of the extend size */
	public final static byte EXTEND_SIZE = INITIAL_SIZE + UnsafeAdapter.INT_SIZE;
	/** The offset of the max size */
	public final static byte MAX_SIZE = EXTEND_SIZE + UnsafeAdapter.INT_SIZE;
	/** The offset of the reset indicator */
	public final static byte RESET = MAX_SIZE + UnsafeAdapter.INT_SIZE;
	/** The offset of the append overrun counter */
	public final static byte OVERRUNS = RESET + UnsafeAdapter.BOOLEAN_SIZE;
	
	/** The total size of the header and the starting address[0][0] of the capacity */
	public final static byte TOTAL = OVERRUNS + UnsafeAdapter.INT_SIZE;
	
	/** A zero byte const */
	public static final byte ZERO_BYTE = 0;

	/** The address[0][0] of the current allocation */
	protected long[][] address = {{-1L}};
	
	/** The spin lock to prevent concurrent access */
	protected SpinLock lock = UnsafeAdapter.allocateSpinLock();
	
	/**
	 * Creates a new LongPeriodAccumulator
	 * @param initialSize The initial number of longs to allocate capacity for
	 * @param extendSize The number of longs to re-allocate capacity for when space is exhausted
	 * @param maxSize The maximum number of longs allocate capacity for
	 * @param resetOnClear true to reset the size of the allocation on clear
	 */
	public LongPeriodAccumulator(int initialSize, int extendSize, int maxSize, boolean resetOnClear) {
		validate(initialSize, extendSize, maxSize);
		final long bodySize = initialSize << 3;
		address[0][0] = UnsafeAdapter.allocateAlignedMemory((bodySize) + TOTAL, this);
		// final values, only set in ctor
		UnsafeAdapter.putInt(address[0][0] + CAPACITY, initialSize);	// not really final	
		UnsafeAdapter.putInt(address[0][0] + INITIAL_SIZE, initialSize);
		UnsafeAdapter.putInt(address[0][0] + EXTEND_SIZE, extendSize);		
		UnsafeAdapter.putBoolean(address[0][0] + RESET, resetOnClear);
		// dynamic values init
		init();
	}
	
	/**
	 * Initializes the memory spaces at init and reset
	 */
	protected void init() {
		UnsafeAdapter.putInt(address[0][0] + ALLOCATED, 0);		
		UnsafeAdapter.putInt(address[0][0] + OVERRUNS, 0);
		UnsafeAdapter.setMemory(address[0][0] + TOTAL, getCapacity() << 3, ZERO_BYTE);		
		UnsafeAdapter.putLong(address[0][0] + LAST_RESET, System.currentTimeMillis());
	}

	/**
	 * Returns the initial size of the accumulator
	 * @return the initial size of the accumulator
	 */
	public int getInitialSize() {
		return UnsafeAdapter.getInt(address[0][0] + INITIAL_SIZE);
	}
	
	/**
	 * Returns the max size of the accumulator
	 * @return the max size of the accumulator
	 */
	public int getMaximumSize() {
		return UnsafeAdapter.getInt(address[0][0] + MAX_SIZE);
	}
	
	/**
	 * Returns the size of the extension of the bufer when the accumulator is extended
	 * @return the size of the extension of the bufer when the accumulator is extended
	 */
	public int getExtendSize() {
		return UnsafeAdapter.getInt(address[0][0] + EXTEND_SIZE);
	}
	
	/**
	 * Indicates if the size of the allocation buffer is shrunk back to the initial size after clearing.
	 * @return true if reset occurs, false otherwise
	 */
	public boolean isResetOnClear() {
		return UnsafeAdapter.getBoolean(address[0][0] + RESET);
	}
	
	/**
	 * Returns the number of overruns where an append failed on account of a full buffer.
	 * Is reset back to zero on a clear.
	 * @return the number of overruns 
	 */
	public int getOverruns() {
		return UnsafeAdapter.getInt(address[0][0] + OVERRUNS);
	}
	
	/**
	 * Returns the timestamp of the last reset (or creation) as a long UTC
	 * @return the timestamp of the last reset
	 */
	public long getLastReset() {
		return UnsafeAdapter.getLong(address[0][0] + LAST_RESET);
	}
	
	/**
	 * Returns the timestamp of the last reset (or creation) as a date
	 * @return the timestamp of the last reset
	 */
	public Date getLastResetDate() {
		return new Date(getLastReset());
	}
	
	/**
	 * Returns the current total capacity of the accumulator
	 * @return the current total capacity of the accumulator
	 */
	public int getCapacity() {
		return UnsafeAdapter.getInt(address[0][0] + CAPACITY);
	}
	
	/**
	 * Returns the current number of values in the accumulator
	 * @return the current number of values in the accumulator
	 */
	public int getSize() {
		return UnsafeAdapter.getInt(address[0][0] + ALLOCATED);
	}
	
	/**
	 * Returns the total byte size of the memory allocated for this instance
	 * @return the total byte size of the memory allocated for this instance
	 */
	public long getTotalSize() {
		return TOTAL + getCapacity() << 3;
	}
	
	/**
	 * Appends a value to the end of the buffer
	 * @param value The value to append
	 * @return the zero based index of the appended value or -1 if an overrun occured
	 * where the buffer is at maximum size and cannot accept any new values
	 */
	public int append(long value) {
		lock.xlock();
		try {
			final int currentSize = getSize();
			final int currentCapacity = getCapacity();
			if(currentCapacity==currentSize) {
				final int extendSize = getExtendSize();
				if(extendSize < 1 || currentCapacity + extendSize > getMaximumSize()) {
					increment(OVERRUNS);
					return -1;
				}
				extend();
			}
			UnsafeAdapter.putInt(address[0][0] + ALLOCATED, currentSize + 1);
			UnsafeAdapter.putLong(address[0][0] + TOTAL + ((currentSize + 1) << 3), value);			
			return currentSize; // -- the index of the just appended value
		} finally {
			lock.xunlock();
		}
	}
	
	
	
	/**
	 * Shrinks the buffer size back to the initial size,
	 * sets the new capacity in the header.
	 * If this resulted in data truncation, the specified size
	 * will be set to the initial size. This would not usually happend
	 * since this method should only be called as part of a clear where
	 * the size would have already been set to zero. 
	 */
	protected void shrinkToInitial() {	
		log("Shrinking from [%s] back to [%s]", getCapacity(), getInitialSize());
		UnsafeAdapter.putInt(address[0][0] + CAPACITY, getInitialSize());
		if(getSize() > getInitialSize()) {
			UnsafeAdapter.putInt(address[0][0] + ALLOCATED, getInitialSize());
		}
		address[0][0] = UnsafeAdapter.reallocateAlignedMemory(address[0][0], TOTAL + (getInitialSize() << 3));
		log("Shrunk");
		
	}
	
	/**
	 * Extends the size of the buffer by the configured extend size,
	 * initializes the new chunk of memory to all zero bytes
	 * and sets the new capacity of the buffer.
	 */
	protected void extend() {		
		final int currentCapacity = getCapacity();
		final int extend = getExtendSize();
		final long newChunkOffset =  currentCapacity << 3;
		final long newChunkSize = extend << 3;
		log("Extending [%s] by [%s] to [%s]", currentCapacity, extend, currentCapacity + extend);
		UnsafeAdapter.putInt(address[0][0] + CAPACITY, currentCapacity + extend);
		address[0][0] = UnsafeAdapter.reallocateAlignedMemory(address[0][0], TOTAL + newChunkOffset + newChunkSize);
		UnsafeAdapter.setMemory(address[0][0] + newChunkOffset, newChunkSize, ZERO_BYTE);
		log("Extended");
	}
	
	public void clear() {
		
	}
	
	public long[] toArray(int startIndex, int endIndex) {
		return null;
	}
	
	public long getMin() {
		return -1;
	}
	
	public long getMax() {
		return -1;
	}
	
	public long getMean() {
		return -1;
	}
	
	public long getPercentile(int perc) {
		return -1;
	}
	
	/**
	 * Increments the integer at the specified offset
	 * @param offset The offset of the integer to increment
	 * @param incrementBy The value to increment by
	 * @return the new value
	 */
	protected int increment(long offset, int incrementBy) {
		int newValue = UnsafeAdapter.getInt(address[0][0] + offset) + incrementBy;
		UnsafeAdapter.putInt(address[0][0] + offset, newValue);
		return newValue;
	}
	
	/**
	 * Increments the integer at the specified offset by 1
	 * @param offset The offset of the integer to increment
	 * @return the new value
	 */
	protected int increment(long offset) {
		return increment(offset, 1);
	}
	
	/**
	 * Validates the provided sizes for the accumulator
	 * @param initialSize The initial number of values to allocate capacity for
	 * @param extendSize The number of values to re-allocate capacity for when space is exhausted
	 * @param maxSize The maximum number of values allocate capacity for
	 */
	protected static void validate(int initialSize, int extendSize, int maxSize) {
		if(initialSize < 0) throw new IllegalArgumentException("Invalid Initial Size (<0): " + initialSize);
		if(extendSize < 1) throw new IllegalArgumentException("Invalid Extend Size (<1): " + extendSize);
		if(maxSize < initialSize) throw new IllegalArgumentException("Invalid Max Size (maxSize < initialSize): " + maxSize);
		if(extendSize<1 && maxSize > initialSize) throw new IllegalArgumentException("Pointless Max Size since (maxSize < initialSize) but (extendSize < 1): " + maxSize);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.util.unsafe.DeAllocateMe#getAddresses()
	 */
	@Override
	public long[][] getAddresses() {
		return address;
	}
	
	public static void main(String args[]) {
		log("Accumulator Test. Total: %s bytes", TOTAL);
		System.setProperty(UnsafeAdapter.TRACK_ALLOCS_PROP, "true");
		System.setProperty(UnsafeAdapter.ALIGN_ALLOCS_PROP, "true");
		log("INITIAL: \n%s", UnsafeAdapter.printUnsafeMemoryStats());
		
		LongPeriodAccumulator lpa = new LongPeriodAccumulator(10, 2, 50, true);
		log("CREATED: Size:%s\n%s", lpa.getSize(), UnsafeAdapter.printUnsafeMemoryStats());
		Random r = new Random(System.currentTimeMillis());
		for(int i = 0; i < 60; i++) {
			lpa.append(r.nextLong());
		}
		log("TEST COMPLETE: Size:%s, Overruns:%s\n%s", lpa.getSize(), lpa.getOverruns(), UnsafeAdapter.printUnsafeMemoryStats());
		lpa.clear();
		System.gc();
		SystemClock.sleep(1000);
		log("CLEARED:\n%s", UnsafeAdapter.printUnsafeMemoryStats());
		lpa = null;
		System.gc();
		SystemClock.sleep(1000);
		log("DEALLOC:\n%s", UnsafeAdapter.printUnsafeMemoryStats());
		
		
	}
	
	/**
	 * Low maintenance out logger
	 * @param fmt The format of the message
	 * @param args The message token values
	 */
	public static void log(Object fmt, Object...args) {
		System.out.println(String.format(fmt.toString(), args));
	}
	
	/**
	 * Low maintenance err logger
	 * @param fmt The format of the message
	 * @param args The message token values
	 */
	public static void loge(Object fmt, Object...args) {
		System.err.println(String.format(fmt.toString(), args));
		if(args!=null && args.length>0 && args[args.length-1] instanceof Throwable) {
			System.err.println("Stack trace follows....");
			((Throwable)args[args.length-1]).printStackTrace(System.err);			
		}
	}
	

}
