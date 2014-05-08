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
package org.helios.jmx.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.management.MBeanOperationInfo;

/**
 * <p>Title: ManagedOperation</p>
 * <p>Description: Annotation to mark a method as a JMX operation, based on Spring's ManagedOperation</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Rob Harrop (Spring)
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.annotation.ManagedOperation</code></p>
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ManagedOperation {
	/**
	 * The operation name, defaulting to the method name when reflected
	 */
	String name() default "";

	/**
	 * The operation description
	 */
	String description() default "";
	
	/**
	 * The impact of the method
	 */
	int impact() default MBeanOperationInfo.UNKNOWN;
	
	/**
	 * The operation's managed parameters
	 */
	ManagedOperationParameter[] parameters() default {};
	
	/**
	 * An array of managed notifications that may be emitted by the annotated managed attribute 
	 */
	ManagedNotification[] notifications() default {};

	

}