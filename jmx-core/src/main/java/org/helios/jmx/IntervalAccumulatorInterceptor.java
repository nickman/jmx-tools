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
package org.helios.jmx;

import java.lang.reflect.AccessibleObject;
import java.util.HashMap;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.helios.jmx.metrics.IntervalAccumulator;

import com.sun.istack.internal.logging.Logger;

/**
 * <p>Title: IntervalAccumulatorInterceptor</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.IntervalAccumulatorInterceptor</code></p>
 */

public class IntervalAccumulatorInterceptor implements MethodInterceptor {
	/** Static class logger */
	static final Logger LOG = Logger.getLogger(IntervalAccumulatorInterceptor.class);
	/** The accumulator the interceptor writes to */
	protected final Map<AccessibleObject, IntervalAccumulator> accumulators = new HashMap<AccessibleObject, IntervalAccumulator>();
	
	/**
	 * Adds an interval accumulator for the passed accessible object
	 * @param accessibleObject the accessible object of the instrumented class
	 * @param accumulator The accumulator to write to for the passed accessible object  
	 */
	public void addAccumulator(AccessibleObject accessibleObject, IntervalAccumulator accumulator) {
		accumulators.put(accessibleObject, accumulator);
		LOG.info("Added acc for [" + accessibleObject + "]");
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
	 */
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		IntervalAccumulator accumulator = accumulators.get(invocation.getMethod());
		long start = System.currentTimeMillis();
		Object v = invocation.proceed();
		long elapsed = System.currentTimeMillis()-start;
		accumulator.append(elapsed);
		return v;
	}

}
