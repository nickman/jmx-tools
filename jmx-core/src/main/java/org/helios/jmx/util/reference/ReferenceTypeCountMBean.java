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

/**
 * <p>Title: ReferenceTypeCountMBean</p>
 * <p>Description: MXBean interface for the ReferenceService ref type count</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.util.reference.ReferenceTypeCountMBean</code></p>
 */

public interface ReferenceTypeCountMBean {
	/**
	 * Returns the name of the cleared reference type
	 * @return the name of the cleared reference type
	 */
	public String getName();
	/**
	 * The count of cleared references of the associated type, since the last reset
	 * @return the count of cleared references
	 */
	public long getLong();
	
	/**
	 * Increments the cleared reference count
	 */
	public void increment();
	
	/**
	 * Sets the count to zero
	 */
	public void reset();
}
