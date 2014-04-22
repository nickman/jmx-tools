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

/**
 * <p>Title: DoubleIntervalAccumulatorMBean</p>
 * <p>Description: MBean interface for the double-view of an accumulator instance</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.metrics.DoubleIntervalAccumulatorMBean</code></p>
 */

public interface DoubleIntervalAccumulatorMBean extends IntervalAccumulatorMBean {
	/**
	 * Processes a new data point into this aggregator
	 * @param value The value to process
	 * @return this aggregator
	 */
	public IntervalAccumulator append(double value);


	/**
	 * Returns the double mean value
	 * @return the double mean value
	 */
	public double getDoubleMean();

	/**
	 * Returns the double minimum value
	 * @return the double minimum value
	 */
	public double getDoubleMin();

	/**
	 * Returns the double maximum value
	 * @return the double maximum value
	 */
	public double getDoubleMax();

}
