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

import org.helios.jmx.util.unsafe.DeAllocateMe;
import org.helios.jmx.util.unsafe.UnsafeAdapter;

/**
 * <p>Title: EWMASnapshot</p>
 * <p>Description: Snapshot manager to compute deltas on pre and post method invocation metrics </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.metrics.EWMASnapshot</code></p>
 */

public class EWMASnapshot implements DeAllocateMe {
	public long[] address = new long[]{-1};

	/**
	 * Creates a new EWMASnapshot
	 */
	public EWMASnapshot() {
		UnsafeAdapter.registerForDeAlloc(this);
	}
	
	public void open(long...values) {
		boolean allocated = address[0] == -1;
		long longBytes = (UnsafeAdapter.LONG_SIZE * values.length);
		if(allocated) {
			UnsafeAdapter.reallocateAlignedMemory(address[0], longBytes + UnsafeAdapter.INT_SIZE);
		} else {
			address[0] = UnsafeAdapter.allocateAlignedMemory(longBytes + UnsafeAdapter.INT_SIZE);
		}
		UnsafeAdapter.putLong(address[0], values.length);
		UnsafeAdapter.copyMemory(values, UnsafeAdapter.LONG_ARRAY_OFFSET, null, UnsafeAdapter.INT_SIZE, longBytes);			
	}
	
	public void close(long...values) {
		if(address[0] == -1) throw new IllegalStateException("Cannot close this snapshot as it has not been opened");
		if(values.length > getCurrentSize()) {
			// Do nothing. close arr is bigger than open arr. Sumthin's up.....
		}
	}
	
	public int getCurrentSize() {
		if(address[0]==-1L) return 0;
		return UnsafeAdapter.getInt(address[0]);
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
