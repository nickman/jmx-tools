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
package test.org.helios.jmx.util.unsafe.collections;

import org.helios.jmx.util.helpers.SystemClock;
import org.helios.jmx.util.unsafe.collections.LongSlidingWindow;
import org.junit.Test;

/**
 * <p>Title: LongSlidingWindowTest</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.jmx.util.unsafe.collections.LongSlidingWindowTest</code></p>
 */

public class LongSlidingWindowTest extends BaseUnsafeCollectonTest {
	
	@Test
	public void testSmallSize() {
		log("PRE Size: %s, Count: %s", unsafeMemoryStats.getTotalAllocatedMemory(), unsafeMemoryStats.getTotalAllocationCount());
		LongSlidingWindow lsw = new LongSlidingWindow(100);
		log("POST Size: %s, Count: %s", unsafeMemoryStats.getTotalAllocatedMemory(), unsafeMemoryStats.getTotalAllocationCount());
		lsw = null;
		System.gc();
		SystemClock.sleep(1000000);
		log("POST GC Size: %s, Count: %s", unsafeMemoryStats.getTotalAllocatedMemory(), unsafeMemoryStats.getTotalAllocationCount());
	}

}
