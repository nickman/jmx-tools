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

import java.util.concurrent.ConcurrentHashMap;

import org.helios.rindle.util.helpers.ConfigurationHelper;


/**
 * <p>Title: IntervalPeriod</p>
 * <p>Description: Defines a metric interval period</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.metrics.IntervalPeriod</code></p>
 */

public class IntervalPeriod {
	/** A cache of all the created periods */
	private static final ConcurrentHashMap<Integer, IntervalPeriod> PERIODS = new ConcurrentHashMap<Integer, IntervalPeriod>(128); 	
	/** The period in s. */
	public final int period;
	
	/** The minimum period granularity */
	private static final int periodGranularity = ConfigurationHelper.getIntSystemThenEnvProperty("interval.granularity.min", 5);
	/** The maximum allowed period */
	private static final int maxPeriod = ConfigurationHelper.getIntSystemThenEnvProperty("interval.granularity.max", 5);
	
	/**
	 * Returns the {@link IntervalPeriod} for the passed period value, possibly adjusted for the system period granularity minimums.
	 * @param period The number of seconds to get the period for
	 * @return the {@link IntervalPeriod}
	 */
	public static IntervalPeriod getPeriod(int period) {
		int _period = round(period);
		IntervalPeriod iperiod = PERIODS.get(_period);
		if(iperiod==null) {
			synchronized(PERIODS) {
				iperiod = PERIODS.get(_period);
				if(iperiod==null) {
					iperiod = new IntervalPeriod(_period);
					PERIODS.put(_period, iperiod);
				}
			}
		}
		return iperiod;
	}
	
	/**
	 * Rounds the passed period to the next highest period granularity
	 * @param period The period to round
	 * @return the rounded up period
	 */
	public static int round(int period) {
		if(period < 1) throw new IllegalArgumentException("Invalid period [" + period + "]. Period must be at least 1 (and rounded to " + periodGranularity + ")");
		if(period > maxPeriod) throw new IllegalArgumentException("Invalid period [" + period + "]. Period must be equal to or less than " + maxPeriod);		
		int mod = period%periodGranularity;
		if(mod==0) return period;
		return periodGranularity-mod + period;
	}
	
	/**
	 * Creates a new PeriodImpl
	 * @param period the period in s.
	 */
	private IntervalPeriod(int period) {
		this.period = period;
	}

	public int getPeriod() {		
		return period;
	}
	
	public int getPeriodGranularity() {
		return periodGranularity;
	}
	
	public int getMaxPeriod() {
		return maxPeriod;
	}


}
