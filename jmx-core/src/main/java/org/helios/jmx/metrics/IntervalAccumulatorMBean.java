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
package org.helios.jmx.metrics;

import java.util.Date;

/**
 * <p>Title: IntervalAccumulatorMBean</p>
 * <p>Description: Base untyped interface for accumulators</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.metrics.IntervalAccumulatorMBean</code></p>
 */

public interface IntervalAccumulatorMBean {
	/**
	 * Increments the count by one
	 * @return the new count
	 */
	public long increment();

	/**
	 * Increments the count by the passed value
	 * @param value the value to increment the count by
	 * @return the new count
	 */
	public long increment(long value);

	/**
	 * Returns the accumulator id
	 * @return the accumulator id
	 */
	public long getId();


	/**
	 * Returns the count of processed metrics
	 * @return the count of processed metrics
	 */
	public long getCount();
	
	/**
	 * Returns the last timestamp of activity on this accumulator
	 * @return the last timestamp of activity on this accumulator
	 */
	public long getLastSampleTime();
	
	/**
	 * Returns the last date of activity on this accumulator
	 * @return the last date of activity on this accumulator
	 */
	public Date getLastSampleDate();

	/**
	 * Reset procedure after the flush procedure and init of a new aggregator
	 */
	public void reset();
}
