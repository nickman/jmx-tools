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

import java.util.Random;
import java.util.UUID;

import org.helios.jmx.util.helpers.SystemClock;
import org.helios.jmx.util.helpers.SystemClock.ElapsedTime;

/**
 * <p>Title: InstrumentMe</p>
 * <p>Description: Test class to instrument</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.jmx.inst.InstrumentMe</code></p>
 */

public class InstrumentMe {
	private final int maxRange;
	private final Random random;
	/**
	 * Creates a new InstrumentMe
	 */
	public InstrumentMe(int maxRange) {
		this.maxRange = Math.abs(maxRange);
		random = new Random(System.currentTimeMillis());
	}
	
	static {
		System.out.println("\n\t========================\n\tInitialized: InstrumentMe\n\t========================\n");
	}
	
	public String generateRandoms() {
		int loops = Math.abs(random.nextInt(maxRange));
		ElapsedTime et = SystemClock.startClock();
		for(int i = 0; i < loops; i++) {
			String s = UUID.randomUUID().toString();
		}
		return et.printAvg("UUID rate", loops);
	}

}
