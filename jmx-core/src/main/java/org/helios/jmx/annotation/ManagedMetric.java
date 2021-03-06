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
package org.helios.jmx.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Title: ManagedMetric</p>
 * <p>Description: Annotation to mark a method/attribute as a metric, based on Spring's org.springframework.jmx.export.metadata.ManagedMetric</p> 
 * <p>Company: Helios Development Group LLC</p>
 *  @author Jennifer Hickey (Spring)
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.annotation.ManagedMetric</code></p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ManagedMetric {
	/**
	 * A description of the metric
	 */
	String description();
	/**
	 * The name of the metric as exposed in the MBeanAttributeInfo.
	 */
	String displayName();
	/**
	 * The type of the metric 
	 */
	MetricType metricType() default MetricType.GAUGE;
	/**
	 * The optional unit of the metric
	 */
	String unit() default "";
	/**
	 * The metric category describing the class or package that the metric is grouped into.
	 * The default blamk value indicates that the containing class's 
	 * {@link MetricGroup} annotation should be read for this value.
	 */
	String category() default "";
	/**
	 * An arbitrary content descriptor for this metric which could be JSON, XML or CSV etc.
	 */
	String descriptor() default "";	
	/**
	 * The optional subkeys for this metric
	 */
	String[] subkeys() default {};
	
	/**
	 * An array of managed notifications that may be emitted by the annotated managed metric 
	 */
	ManagedNotification[] notifications() default {};
	
	/**
	 * Indicates if the metric type is rendered as a CompositeType 
	 * and can be enhanced to surface the type's fields as first class mbean attributes 
	 */
	boolean popable() default false;
	
	/**
	 * The window size for GAUGE type metrics
	 */
	int windowSize() default 0;
	
	/**
	 * The initial value for COUNTER type metrics
	 */
	long initialValue() default -1L;


}
