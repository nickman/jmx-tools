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

import org.helios.jmx.util.unsafe.UnsafeAdapter;
import org.helios.jmx.util.unsafe.UnsafeAdapter.UnsafeMemoryMBean;
import org.junit.BeforeClass;
import org.junit.Ignore;

import test.org.helios.jmx.BaseTest;

/**
 * <p>Title: BaseUnsafeCollectonTest</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.jmx.util.unsafe.collections.BaseUnsafeCollectonTest</code></p>
 */
@Ignore
public class BaseUnsafeCollectonTest extends BaseTest {
	/** The unsafe memory stats */
	protected static UnsafeMemoryMBean unsafeMemoryStats = UnsafeAdapter.unsafeMemoryStats;
	/**
	 * Checks to make sure the UnsafeAdapter is in trackMem mode so we can test
	 */
	@BeforeClass
	public static void checkUnsafeManaged() {
	     org.junit.Assume.assumeTrue(UnsafeAdapter.trackMem);
	 }

}
