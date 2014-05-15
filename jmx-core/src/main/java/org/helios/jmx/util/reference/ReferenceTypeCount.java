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

import java.io.Serializable;
import java.util.Map;

import org.helios.jmx.util.helpers.NumericCounter;

/**
 * <p>Title: ReferenceTypeCount</p>
 * <p>Description: A count tracker for type of references cleared in the ReferenceService</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.util.reference.ReferenceTypeCount</code></p>
 */

public class ReferenceTypeCount implements ReferenceTypeCountMBean, Serializable {
	/**  */
	private static final long serialVersionUID = 5683688389706572174L;
	/** The name of a cleared reference type */
	private final String typeName;
	/** The count of cleared references */
	private final NumericCounter counter = new NumericCounter();
	
	/**
	 * Creates a new ReferenceTypeCount
	 * @param typeName The name of a cleared reference type
	 */
	public ReferenceTypeCount(String typeName) {
		this.typeName = typeName;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.util.reference.ReferenceTypeCountMBean#reset()
	 */
	public void reset() {
		counter.set(0);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.util.reference.ReferenceTypeCountMBean#increment()
	 */
	public void increment() {
		counter.increment();
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.util.reference.ReferenceTypeCountMBean#getName()
	 */
	@Override
	public String getName() {
		return typeName;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jmx.util.reference.ReferenceTypeCountMBean#getLong()
	 */
	@Override
	public long getLong() {
		return counter.get();
	}



	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((typeName == null) ? 0 : typeName.hashCode());
		return result;
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ReferenceTypeCount))
			return false;
		ReferenceTypeCount other = (ReferenceTypeCount) obj;
		if (typeName == null) {
			if (other.typeName != null)
				return false;
		} else if (!typeName.equals(other.typeName))
			return false;
		return true;
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("ReferenceTypeCount [name:%s, count:%s]", typeName, counter.get());
	}

}
