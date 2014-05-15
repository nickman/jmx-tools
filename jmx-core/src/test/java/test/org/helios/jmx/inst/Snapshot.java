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
import static org.helios.jmx.util.unsafe.UnsafeAdapter.getLong;
import static org.helios.jmx.util.unsafe.UnsafeAdapter.putLong;

import java.lang.ref.PhantomReference;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.jmx.util.reference.ReferenceService;
import org.helios.jmx.util.unsafe.DeAllocateMe;
import org.helios.jmx.util.unsafe.UnsafeAdapter;

/**
 * <p>Title: Snapshot</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.jmx.inst.Snapshot</code></p>
 */

public class Snapshot implements DeAllocateMe, Runnable {
	/** The address holder */
	private long[][] address = new long[][]{{0}};
	
	/** A phantom reference to the tracked method */
	private final PhantomReference<Method> method; 
	/** The snapshot locals for each thread */
	private final ThreadLocal<SnapshotLocal> locals = new ThreadLocal<SnapshotLocal>() {
		@Override
		protected SnapshotLocal initialValue() {
			return new SnapshotLocal();
		}
	};
	private long address() { return address[0][0]; }
	
	/** The serial number generator for snapshot instances */
	private static final AtomicLong ID_FACTORY = new AtomicLong(0L);
	
	/** The offset of the snapshot id */
	public static final byte ID = 0;
	
	/** The total initial allocation size in bytes */
	public static final byte TOTAL_INITIAL_SIZE = ID + LONG_SIZE;
	
	/**
	 * Creates a new Snapshot
	 * @param method The method this snapshot is tracking
	 */
	public Snapshot(Method method) {			
		address[0][0] = UnsafeAdapter.allocateAlignedMemory(TOTAL_INITIAL_SIZE);
		UnsafeAdapter.registerForDeAlloc(this);
		this.method = ReferenceService.getInstance().newPhantomReference(method, this);
		putLong(address() + ID, ID_FACTORY.incrementAndGet());
		
	}
	
	/**
	 * Returns the tracked method which may be null if the method has been garbage collected
	 * @return the tracked method or null
	 */
	public Method getMethod() {
		return this.method.get();
	}
	
	/**
	 * Returns the ID of this snapshot
	 * @return the ID of this snapshot
	 */
	public long getId() {
		return getLong(address() + ID);
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
