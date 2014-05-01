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
package org.helios.jmx.metrics.ewma;

import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.Map;

import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

/**
 * <p>Title: EWMAGroup</p>
 * <p>Description: A gropup of related ewmas, e.g. contains all the ewmas for a class</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.metrics.ewma.EWMAGroup</code></p>
 */

public class EWMAGroup extends StandardMBean {
	
	/**
	 * Creates a new EWMAGroup
	 * @throws NotCompliantMBeanException (should not usually be thrown)
	 */
	private EWMAGroup() throws NotCompliantMBeanException {
		super(EWMAGroupMXBean.class);
	}

	
	public static class EWMAKeyBuilder {
		private final Class<?> clazz;
		private final String packageName;
		
		private String name = null;
		private Member member = null;
		private Class<?>[] signature = null;
		
		private final Map<Integer, String> parameterziedArgs = new HashMap<Integer, String>();
		
		public ObjectName build() {
			if(name==null && member==null && signature == null) throw new IllegalStateException("Must provide at least one of name, member or signature");
			
			return null;
		}

		/**
		 * Creates a new EWMAKeyBuilder
		 * @param clazz The class the key will be based on
		 */
		private EWMAKeyBuilder(Class<?> clazz) {
			this.clazz = clazz;
			this.packageName = null;
		}
		/**
		 * Creates a new EWMAKeyBuilder
		 * @param packageName Notionally the same as a class but allows for some arbitrary dot notated name.
		 */
		private EWMAKeyBuilder(String packageName) {
			this.packageName= packageName;
			this.clazz = null;
		}

		
		/**
		 * Sets the name qualifier for this EWMA key.
		 * If combined with a signature and a class,
		 * will attempt to relfect out the implied member.
		 * @param name the name qualifier
		 * @return this builder
		 */
		public EWMAKeyBuilder name(String name) {
			this.name = name;
			return this;
		}

		/**
		 * Sets the member qualifier for this EWMA key 
		 * @param member The member
		 * @return this builder
		 */
		public EWMAKeyBuilder member(Member member) {
			this.member = member;
			return this;
		}
		
		/**
		 * Sets the signature of the member to reflect out to qualify for this EWMA key 
		 * @param signature The class signature of the member
		 * @return this builder
		 */
		public EWMAKeyBuilder signature(Class<?>... signature) {
			this.signature = signature;
			return this;
		}
		
		
		/**
		 * Adds a parameter index and name which will add a parameterized qualifier to the key
		 * @param index The arg index (zero based)
		 * @param name The logical name for the arg (i.e. the parameter name)
		 * @return this builder
		 */
		public EWMAKeyBuilder paramIndex(int index, String name) {
			parameterziedArgs.put(index, name);
			return this;
		}

		/**
		 * Adds a map of parameter indexes and names which will add parameterized qualifiers to the key
		 * @param paramIndexes a map of param indexes
		 * @return this builder
		 */
		public EWMAKeyBuilder paramIndex(Map<Integer, String> paramIndexes) {
			parameterziedArgs.putAll(paramIndexes);
			return this;
		}
		
	}
}
