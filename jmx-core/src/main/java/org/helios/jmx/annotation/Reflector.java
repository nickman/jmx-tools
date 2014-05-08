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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;
import javax.management.MBeanAttributeInfo;

import org.helios.jmx.util.helpers.StringHelper;


/**
 * <p>Title: Reflector</p>
 * <p>Description: A class of static utility methods for processing class metadata regarding managed options in the StdComponent </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.annotation.Reflector</code></p>
 */

public class Reflector {
	/** A const empty array */
	public static final ManagedMetricImpl[] EMPTY_ARR = {};
	/** A const empty array */
	public static final Method[] EMPTY_M_ARR = {};
	/** A const empty array */
	public static final MBeanAttributeInfo[] EMPTY_MAI_ARR = {};
	/** Replacement pattern for the get/set leading a method name */
	public static final Pattern GETSET_PATTERN = Pattern.compile("get|set|is|has", Pattern.CASE_INSENSITIVE);
	
	/**
	 * Returns an array of ManagedMetricImpls extracted from the passed class
	 * @param clazz The class to extract from
	 * @return a [possibly zero length] array of ManagedMetricImpls 
	 */
	public static ManagedMetricImpl[] from(Class<?> clazz) {
		Method[] methods = getAnnotatedMethods(clazz, ManagedMetric.class);
		if(methods.length==0) return EMPTY_ARR;
		ManagedMetricImpl[] impls = new ManagedMetricImpl[methods.length];
		for(int i = 0; i < methods.length; i++) {
			impls[i] = managedMetricImplFrom(methods[i]);
		}
		return impls;
	}
	
	/**
	 * Creates a new ManagedMetricImpl from the passed method if it is annotated with {@link ManagedMetric}.
	 * @param method The method to extract a ManagedMetricImpl from 
	 * @return the ManagedMetricImpl created, or null if the method was not annotated
	 */
	public static ManagedMetricImpl managedMetricImplFrom(Method method) {
		ManagedMetric mm = nvl(method, "method").getAnnotation(ManagedMetric.class);
		if(mm==null) return null;
		String category = nws(mm.category());
		String displayName = nws(mm.displayName());
		if(displayName==null) {
			displayName = attr(method);
		}
		Class<?> clazz = method.getDeclaringClass();
		if(category==null) {
			MetricGroup mg = clazz.getAnnotation(MetricGroup.class);
			if(mg!=null) {
				category = nws(mg.category());
			}
			if(category==null) {
				mg = clazz.getPackage().getAnnotation(MetricGroup.class);
				if(mg!=null) {
					category = nws(mg.category());
				}
			}
		}
		if(category==null) {
			category = clazz.getSimpleName() + " Metric";
		}
		return null; //new ManagedMetricImpl(mm.description(), mm.displayName(), mm.metricType(), category, mm.unit(), mm.descriptor());
	}
	
	/**
	 * Creates a new ManagedAttributeImpl from the passed method if it is annotated with {@link ManagedAttribute}.
	 * @param method The method to extract a ManagedAttributeImpl from 
	 * @return the ManagedAttributeImpl created, or null if the method was not annotated
	 */
	public static ManagedAttributeImpl managedAttributeImplFrom(Method method) {
		ManagedAttribute ma = nvl(method, "method").getAnnotation(ManagedAttribute.class);
		if(ma==null) return null;
		String name = nws(ma.name())==null ? attr(method) : ma.name();
		String description = nws(ma.description())==null ? String.format("JMX Managed Attribute [%s.%s]", method.getDeclaringClass().getSimpleName(), name) : ma.description();
		return null; //new ManagedAttributeImpl(name, description);
	}
	
	
	
	/**
	 * Returns an array of MBeanAttributeInfos extracted from the passed class
	 * @param clazz The class to extract from
	 * @return a [possibly zero length] array of MBeanAttributeInfos 
	 */
	public static MBeanAttributeInfo[] mbeanMetrics(Class<?> clazz) {
		Method[] methods = getAnnotatedMethods(clazz, ManagedMetric.class);
		if(methods.length==0) return EMPTY_MAI_ARR;
		MBeanAttributeInfo[] impls = new MBeanAttributeInfo[methods.length];
		for(int i = 0; i < methods.length; i++) {
			impls[i] = mbeanMetric(methods[i]);
		}
		return impls;
	}
	
	/**
	 * Creates and returns a new {@link MBeanAttributeInfo} for the ManagedMetric annotation data on the passed method.
	 * @param method The method to extract a MBeanAttributeInfo from 
	 * @return the MBeanAttributeInfo created, or null if the method was not annotated
	 */
	public static MBeanAttributeInfo mbeanMetric(Method method) {
		ManagedMetricImpl mmi = managedMetricImplFrom(method);
		if(mmi==null) return null;
		return new MBeanAttributeInfo(
				mmi.getDisplayName(),
				method.getReturnType().getName(),
				mmi.getDescription(),
				true,
				false, 
				false,
				descriptor(mmi)
		);
		
	}
	
//	/**
//	 * Creates and returns a new {@link MBeanAttributeInfo} for the ManagedAttribute annotation data on the passed method.
//	 * @param method The method to extract a MBeanAttributeInfo from 
//	 * @return the MBeanAttributeInfo created, or null if the method was not annotated
//	 */
//	public static MBeanAttributeInfo mbeanAttributes(Method method) {
//		ManagedAttributeImpl mai = from(method);
//		if(mmi==null) return null;
//		return new MBeanAttributeInfo(
//				mmi.getDisplayName(),
//				method.getReturnType().getName(),
//				mmi.getDescription(),
//				true,
//				false, 
//				false,
//				descriptor(mmi)
//		);
//	}
	
	
	/**
	 * Creates an MBean attribute descriptor for the passed method, if it is annotated with a managed metric
	 * @param method The method to extract a descriptor from
	 * @return an MBean attribute descriptor or null if the method was not annotated with a managed metric
	 */
	public static Descriptor descriptor(Method method) {
		ManagedMetricImpl mml = managedMetricImplFrom(method);
		return mml==null ? null : descriptor(mml);
	}
	
	/**
	 * Creates an MBean attribute descriptor for the ManagedMetricImpl
	 * @param managedMetric The ManagedMetricImpl to generate a descriptor for
	 * @return an MBean attribute descriptor
	 */
	public static Descriptor descriptor(ManagedMetricImpl managedMetric) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("category", managedMetric.getCategory());
		if(managedMetric.getDescriptor()!=null) map.put("descriptor", managedMetric.getDescriptor());
		map.put("metricType", managedMetric.getMetricType().name());		
		map.put("subkeys", managedMetric.getSubkeys());
		map.put("unit", managedMetric.getUnit());
		return new ImmutableDescriptor(map);		
	}
	
	
	/**
	 * Returns an array of methods in the passed class that are annotated with the passed annotation type
	 * @param clazz The class to inspect
	 * @param annotationType The annotation to inspect for
	 * @return a [possibly empty] array of annotated methods
	 */
	public static Method[] getAnnotatedMethods(Class<?> clazz, Class<? extends Annotation> annotationType) {
		
		Map<String, Method> methods = new HashMap<String, Method>();		
		for(Method m: clazz.getMethods()) {
			if(m.getAnnotation(annotationType)!=null) {
				methods.put(StringHelper.getMethodDescriptor(m), m);
			}
		}
		for(Method m: clazz.getDeclaredMethods()) {
			if(m.getAnnotation(annotationType)!=null) {
				methods.put(StringHelper.getMethodDescriptor(m), m);
			}
		}
		if(methods.isEmpty()) return EMPTY_M_ARR;
		return methods.values().toArray(new Method[methods.size()]);
	}	
	
	
	/**
	 * Returns an attribute name for the passed getter method
	 * @param m The method to get an attribute name for
	 * @return the attribute name
	 */
	private static String attr(Method m) {
		String name = m.getName();
		name = GETSET_PATTERN.matcher(name).replaceFirst("");
		return String.format("%s%s", name.substring(0,1).toUpperCase(), name.substring(1));
	}
	
	/**
	 * Inspects the passed stringy and returns null if the value is null or empty
	 * @param cs The value to inspect
	 * @return the value or null
	 */
	public static String nws(CharSequence cs) {
		if(cs==null) return null;
		String s = cs.toString().trim();
		return s.isEmpty() ? null : s;
	}

	/**
	 * Null param checker
	 * @param t The objetct to test
	 * @param name The name of the object to embedd in the exception method
	 * @return the passed object if not null
	 */
	public static <T> T nvl(T t, String name) {
		if(t==null) {
			throw new IllegalArgumentException(String.format("The passed %s was null or empty", name));
		}
		if(t instanceof CharSequence) {
			if(((CharSequence)t).toString().trim().isEmpty()) {
				throw new IllegalArgumentException(String.format("The passed %s was null or empty", name));
			}
		}
		return t;		
	}
	
	

	private Reflector() {}

}
