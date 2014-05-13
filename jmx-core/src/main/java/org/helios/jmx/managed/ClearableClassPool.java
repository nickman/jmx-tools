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
package org.helios.jmx.managed;

import java.util.LinkedHashSet;
import java.util.Set;

import javassist.ClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

/**
 * <p>Title: ClearableClassPool</p>
 * <p>Description: Classpool that tracks added ctclasses so they can be cleaed</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhittehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.managed.ClearableClassPool</code></p>
 */

class ClearableClassPool extends ClassPool {
	
	private final ThreadLocal<Set<CtClass>> clearableClasses = new ThreadLocal<Set<CtClass>>() {
		@Override
		protected Set<CtClass> initialValue() {
			return new LinkedHashSet<CtClass>();
		}
		public void remove() {
			for(CtClass ct: get()) {
				ct.detach();
			}
			super.remove();
		};		
	};
	
	private final ThreadLocal<Set<ClassPath>> clearableClasspaths = new ThreadLocal<Set<ClassPath>>() {
		@Override
		protected Set<ClassPath> initialValue() {
			return new LinkedHashSet<ClassPath>();
		}
		public void remove() {
			for(ClassPath cp: get()) {
				removeClassPath(cp);
			}
			super.remove();
		};
	};
	
//	private final ThreadLocal<Set<String>> clearableImports = new ThreadLocal<Set<String>>() {
//		@Override
//		protected Set<String> initialValue() {
//			return new LinkedHashSet<String>();
//		}
//	};
	

	/**
	 * Creates a new ClearableClassPool
	 */
	public ClearableClassPool() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new ClearableClassPool
	 * @param useDefaultPath true for a default classpath
	 */
	public ClearableClassPool(boolean useDefaultPath) {
		super(useDefaultPath);
	}

	/**
	 * Creates a new ClearableClassPool
	 * @param parent the parent classpool
	 */
	public ClearableClassPool(ClassPool parent) {
		super(parent);
	}
	
	/**
	 * {@inheritDoc}
	 * @see javassist.ClassPool#appendClassPath(javassist.ClassPath)
	 */
	@Override
	public ClassPath appendClassPath(ClassPath cp) {
		clearableClasspaths.get().add(cp);
		return super.appendClassPath(cp);
	}
	
	/**
	 * {@inheritDoc}
	 * @see javassist.ClassPool#appendClassPath(java.lang.String)
	 */
	@Override
	public ClassPath appendClassPath(String pathname) throws NotFoundException {		
		ClassPath cp = super.appendClassPath(pathname);
		clearableClasspaths.get().add(cp);
		return cp;
	}
	
	
	public void reset() {
		this.clearImportedPackages();
		clearableClasses.remove();
		clearableClasspaths.remove();
	}
	
	

}
