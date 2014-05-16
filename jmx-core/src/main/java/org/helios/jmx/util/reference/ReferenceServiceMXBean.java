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
package org.helios.jmx.util.reference;

import java.util.Map;

/**
 * <p>Title: ReferenceServiceMXBean</p>
 * <p>Description: JMX MBean interface for {@link ReferenceService}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.util.reference.ReferenceServiceMXBean</code></p>
 */

public interface ReferenceServiceMXBean {
	/**
	 * Returns the length of the reference queue
	 * @return the length of the reference queue
	 */
	public long getQueueDepth();
	
	/**
	 * Returns the total number of cleared reference executions since the last reset
	 * @return the total number of cleared reference executions
	 */
	public long getClearedRefCount();
	
	/**
	 * Resets the service stats
	 */
	public void resetStats();
	
	/**
	 * Returns the last computed average.
	 * @return the last computed average 
	 */
	public double getAverage();

	/**
	 * Returns the minimum recorded value since the last reset
	 * @return the minimum recorded value 
	 */
	public double getMinimum();

	/**
	 * Returns the maximum recorded value since the last reset
	 * @return the maximum recorded value 
	 */
	public double getMaximum();

	/**
	 * Returns the mean recorded value since the last reset
	 * @return the mean recorded value 
	 */
	public double getMean();
	
	/**
	 * Returns the count of recorded values since the last reset
	 * @return the count of recorded values 
	 */
	public long getCount();
	
	/**
	 * Returns the count of errors since the last reset
	 * @return the count of errors 
	 */
	public long getErrors();
	
	/**
	 * Returns a map of the counts of cleared references by reference type name
	 * @return a map of the counts of cleared references by reference type name
	 */
	public ReferenceTypeCountMBean[] getCountsByTypes();
	
	public Map<String, ReferenceTypeCountMBean> getCountTT();
}
