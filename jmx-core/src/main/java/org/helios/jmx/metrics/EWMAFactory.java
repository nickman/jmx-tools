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
package org.helios.jmx.metrics;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.ObjectName;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.helios.jmx.batch.BatchAttributeService;
import org.helios.jmx.util.helpers.JMXHelper;

/**
 * <p>Title: EWMAFactory</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.metrics.EWMAFactory</code></p>
 */

public class EWMAFactory {
	/** A map of ewmas keyed by the ObjectName */
	private static final NonBlockingHashMap<ObjectName, DirectEWMAMBean> ewmas = new NonBlockingHashMap<ObjectName, DirectEWMAMBean>();
	private static final AtomicBoolean batchRegistered = new AtomicBoolean(false);
	/**
	 * Creates or acquires a EWMA for the passed class and method name
	 * @param concurrent true for a concurrent version, false otherwise
	 * @param clazz The class being tracked
	 * @param methodName The method name being tracked
	 * @return the appender for the created ewma
	 */
	public static EWMAAppender ewma(boolean concurrent, Class<?> clazz, String methodName) {
		if(batchRegistered.compareAndSet(false, true)) {
			BatchAttributeService.getInstance();
		}
		final ObjectName on = JMXHelper.objectName(clazz, methodName);
		DirectEWMAMBean mbean = ewmas.get(on);
		if(mbean==null) {
			synchronized(ewmas) {
				mbean = ewmas.get(on);
				if(mbean==null) {
					if(concurrent) {
						mbean = new ConcurrentDirectEWMA(128, on);
					} else {
						mbean = new DirectEWMA(128, on);
					}
					ewmas.put(on, mbean);
				}
			}
		}
		return mbean;
	}
	
	private EWMAFactory() {
	}


}
