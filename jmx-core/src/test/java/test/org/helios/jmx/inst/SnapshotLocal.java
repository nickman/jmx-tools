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
package test.org.helios.jmx.inst;

import static org.helios.jmx.util.unsafe.UnsafeAdapter.LONG_SIZE;
import static org.helios.jmx.util.unsafe.UnsafeAdapter.putLong;

import java.lang.ref.PhantomReference;

import org.helios.jmx.util.reference.ReferenceService;
import org.helios.jmx.util.unsafe.DeAllocateMe;
import org.helios.jmx.util.unsafe.UnsafeAdapter;

/**
 * <p>Title: SnapshotLocal</p>
 * <p>Description: Instrumentation context associating a {@link Snapshot} and a thread's view of it</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.jmx.inst.SnapshotLocal</code></p>
 */

public class SnapshotLocal implements DeAllocateMe, Runnable {
	/** The address holder */
	private long[][] address = new long[][]{{0}};
	
	/** A phantom reference to the tracked thread */
	private final PhantomReference<Thread> thread; 
	
	private long address() { return address[0][0]; }
	
	/** The offset of the thread id */
	public static final byte ID = 0;
	
	/** The total initial allocation size in bytes */
	public static final byte TOTAL_INITIAL_SIZE = ID + LONG_SIZE;
	
	

	/**
	 * Creates a new SnapshotLocal
	 */
	public SnapshotLocal() {
		address[0][0] = UnsafeAdapter.allocateAlignedMemory(TOTAL_INITIAL_SIZE);
		UnsafeAdapter.registerForDeAlloc(this);
		this.thread = ReferenceService.getInstance().newPhantomReference(Thread.currentThread(), this);
		putLong(address() + ID, Thread.currentThread().getId());
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.util.unsafe.DeAllocateMe#getAddresses()
	 */
	@Override
	public long[][] getAddresses() {
		return address;
	}	
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		final long addr = address();
		if(addr>0) {
			address[0][0] = -1L;
			UnsafeAdapter.freeMemory(addr);
		}				
	}
}
