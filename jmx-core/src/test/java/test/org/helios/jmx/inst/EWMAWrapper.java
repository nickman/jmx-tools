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
package test.org.helios.jmx.inst;

import org.helios.jmx.metrics.ewma.ConcurrentDirectEWMA;

/**
 * <p>Title: EWMAWrapper</p>
 * <p>Description: EWMA invocation wrapper</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.jmx.inst.EWMAWrapper</code></p>
 */

public class EWMAWrapper {
	protected static final ConcurrentDirectEWMA ewma = new ConcurrentDirectEWMA(100);
	
	protected static final ThreadLocal<long[]> startTime = new ThreadLocal<long[]>() {
		@Override
		protected long[] initialValue() {
			return new long[1];
		}
	};
	
	static {
		System.out.println("\n\t========================\n\tInitialized: XXX\n\t========================\n");
	}

	
	public static void start() {
		startTime.get()[0] = System.nanoTime();
	}
	
	public static void end() {
		ewma.append(System.nanoTime() - startTime.get()[0]);
		System.out.println(ewma);
	}
	
	public static void error() {
		ewma.error();
	}
	
}
