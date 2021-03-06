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

import java.io.Serializable;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataView;

import org.helios.jmx.annotation.ManagedAttribute;
import org.helios.jmx.annotation.ManagedResource;

/**
 * <p>Title: DirectEWMAMBean</p>
 * <p>Description: MBean interface for {@link DirectEWMA}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.metrics.DirectEWMAMBean</code></p>
 */
@ManagedResource(description="An exponentially weighted moving average metric tracker")
public interface DirectEWMAMBean extends EWMAAppenderMBean, CompositeData, CompositeDataView, Serializable {
	/**
	 * Resets the EWMA
	 */
	public void reset();

	/**
	 * Returns the timestamp of the last sample as a long UTC.
	 * @return the timestamp of the last sample 
	 */
	@ManagedAttribute(description="The timestamp of the last sample as a long UTC")
	public long getLastSample();
	
	/**
	 * Returns the most recently appended value
	 * @return the most recently appended value
	 */
	@ManagedAttribute(description="The most recently appended value")
	public double getLastValue();

	/**
	 * Returns the last computed average.
	 * @return the last computed average 
	 */
	@ManagedAttribute(description="The last computed weighted average")
	public double getAverage();

	/**
	 * Returns the minimum recorded value since the last reset
	 * @return the minimum recorded value 
	 */
	@ManagedAttribute(description="The minimum recorded value since the last reset")
	public double getMinimum();

	/**
	 * Returns the maximum recorded value since the last reset
	 * @return the maximum recorded value 
	 */
	@ManagedAttribute(description="The maximum recorded value since the last reset")
	public double getMaximum();

	/**
	 * Returns the mean recorded value since the last reset
	 * @return the mean recorded value 
	 */
	@ManagedAttribute(description="The mean recorded value since the last reset")
	public double getMean();

	/**
	 * Returns the count of recorded values since the last reset
	 * @return the count of recorded values 
	 */
	@ManagedAttribute(description="The count of recorded values since the last reset")
	public long getCount();
	
	/**
	 * Returns the count of errors since the last reset
	 * @return the count of errors 
	 */
	@ManagedAttribute(description="The count of recorded errors since the last reset")
	public long getErrors();
	

	/**
	 * Returns the window size in ms.
	 * @return the window size  
	 */
	public long getWindow();

	/**
	 * Appends a new double sample
	 * @param sample a new double sample
	 */
	public void append(double sample);

	/**
	 * Increments the count by one and returns the new count
	 * @return the new count
	 */
	public long increment();

	/**
	 * Increments the count by the passed value and returns the new count
	 * @param value the number to increment by
	 * @return the new count
	 */
	public long increment(long value);
	
	/**
	 * Increments the error count
	 * @return the new error count
	 */
	public long error();
	
	
	
//	/**
//	 * Returns an appender to this EWMA
//	 * @return an appender to this EWMA
//	 */
//	public EWMAAppenderMBean getAppender();	

}
