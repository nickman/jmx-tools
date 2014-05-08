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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.modelmbean.DescriptorSupport;

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
	
	/** An array of the method annotations we'll search for */
	@SuppressWarnings("unchecked")
	private static final Class<? extends Annotation>[] SEARCH_ANNOTATIONS = new Class[]{
		ManagedAttribute.class, ManagedMetric.class, ManagedOperation.class
	};

	
	public static MBeanInfo from(Class<?> clazz) {
		final Map<Class<? extends Annotation>, Set<Method>> methodMap = getAnnotatedMethods(clazz, SEARCH_ANNOTATIONS);
		final Set<MBeanNotificationInfo> notificationInfo = new HashSet<MBeanNotificationInfo>();
		final Set<MBeanAttributeInfo> attrInfos = new HashSet<MBeanAttributeInfo>();
		Collections.addAll(attrInfos, getManagedAttributeInfos(notificationInfo, methodMap.get(ManagedAttribute.class)));
		Collections.addAll(attrInfos, getManagedAttributeInfos(notificationInfo, methodMap.get(ManagedMetric.class)));		
	}
	
	/**
	 * Reflects out the MBeanOperationInfos for @ManagedOperation annotations the passed methods
	 * @param notificationInfo Any notification infos we find along the way get dropped in here
	 * @param methods The annotated methods in the class
	 * @return a [possibly zero length] MBeanOperationInfo array
	 */
	public static MBeanOperationInfo[] getManagedOperationInfos(final Set<MBeanNotificationInfo> notificationInfo, final Set<Method> methods) {
		Set<MBeanOperationInfo> infos = new HashSet<MBeanOperationInfo>(methods.size());
		for(Method m: methods) {
			ManagedOperation mo = m.getAnnotation(ManagedOperation.class);
			ManagedMetricImpl mmi = new ManagedMetricImpl(mm);
			Collections.addAll(notificationInfo, ManagedNotificationImpl.from(mmi.getNotifications()));
			infos.add(mmi.toMBeanInfo(m.getReturnType()));
		}
		return infos.toArray(new MBeanOperationInfo[infos.size()]);
	}
	

	/**
	 * Reflects out the ManagedAttributeInfos for @ManagedMetric annotations the passed methods
	 * @param notificationInfo Any notification infos we find along the way get dropped in here
	 * @param methods The annotated methods in the class
	 * @return a [possibly zero length] MBeanAttributeInfo array
	 */
	public static MBeanAttributeInfo[] getManagedMetricInfos(final Set<MBeanNotificationInfo> notificationInfo, final Set<Method> methods) {
		Set<MBeanAttributeInfo> infos = new HashSet<MBeanAttributeInfo>(methods.size());
		for(Method m: methods) {
			ManagedMetric mm = m.getAnnotation(ManagedMetric.class);
			ManagedMetricImpl mmi = new ManagedMetricImpl(mm);
			Collections.addAll(notificationInfo, ManagedNotificationImpl.from(mmi.getNotifications()));
			infos.add(mmi.toMBeanInfo(m.getReturnType()));
		}
		return infos.toArray(new MBeanAttributeInfo[infos.size()]);
	}

	/**
	 * Reflects out the ManagedAttributeInfos for @ManagedAttribute annotations the passed methods
	 * @param notificationInfo Any notification infos we find along the way get dropped in here
	 * @param attrs The annotated methods in the class
	 * @return a [possibly zero length] MBeanAttributeInfo array
	 */
	public static MBeanAttributeInfo[] getManagedAttributeInfos(final Set<MBeanNotificationInfo> notificationInfo, final Set<Method> attrs) {
		Set<MBeanAttributeInfo> infos = new HashSet<MBeanAttributeInfo>(attrs.size());
		Map<String, MBeanAttributeInfo[]> attributes = new HashMap<String, MBeanAttributeInfo[]>(attrs.size());
		for(Method m: attrs) { 
			final int index = m.getParameterTypes().length;
			if(index>1) throw new RuntimeException(String.format("The method [%s.%s] (%s)] is neither a getter or a setter but was annotated @ManagedAttribute", m.getDeclaringClass().getName(), m.getName(), StringHelper.getMethodDescriptor(m)));
			ManagedAttribute ma = m.getAnnotation(ManagedAttribute.class);
			ManagedAttributeImpl maImpl = new ManagedAttributeImpl(attr(m), ma);
			Collections.addAll(notificationInfo, ManagedNotificationImpl.from(maImpl.getNotifications()));
			Class<?> type = index==0 ?  m.getReturnType() : m.getParameterTypes()[0];
			MBeanAttributeInfo minfo = maImpl.toMBeanInfo(type);
			minfo.getDescriptor().setField(index==0 ? "getMethod" : "setMethod", m.getName());
			String attributeName = minfo.getName();
			MBeanAttributeInfo[] pair = attributes.get(attributeName); if(pair==null) { pair = new MBeanAttributeInfo[2];  attributes.put(attributeName, pair); }
						
			if(pair[index]!=null) System.err.println("Duplicate attribute names on [" + m.getDeclaringClass().getName() + "] : [" + attributeName + "]");
			pair[index] = minfo;
		}			
		for(Map.Entry<String, MBeanAttributeInfo[]> entry: attributes.entrySet()) {
			String name = entry.getKey();
			MBeanAttributeInfo[] pair = entry.getValue();
			if(pair[0]!=null && pair[1]!=null) {
				infos.add(new MBeanAttributeInfo(name, pair[0].getType(), pair[0].getDescription(), true, true, isIs(pair[0]), merge(pair[0].getDescriptor(), pair[1].getDescriptor())));
			} else {
				if(pair[0]!=null) {
					infos.add(new MBeanAttributeInfo(name, pair[0].getType(), pair[0].getDescription(), true, false, isIs(pair[0]), pair[0].getDescriptor()));
				} else {
					infos.add(new MBeanAttributeInfo(name, pair[1].getType(), pair[1].getDescription(), false, true, false, pair[1].getDescriptor()));
				}
			}
		}
		return infos.toArray(new MBeanAttributeInfo[infos.size()]);
	}
	
	/**
	 * Does a bill clinton on the passed info to determine the meaning of "is"
	 * @param info The info to inspect
	 * @return true if "is" means "is" (or "has"), false otherwise
	 */
	public static boolean isIs(MBeanAttributeInfo info) {
		Descriptor d = info.getDescriptor();
		if(d==null) return false;
		String getMeth = (String)d.getFieldValue("getMethod");
		if(getMeth==null) return false;
		return (getMeth.startsWith("is") || getMeth.startsWith("has"));
	}
	
	/**
	 * Merges two descriptors with dups in d2 overwritten by d1
	 * @param d1 The overriding descriptor
	 * @param d2 The other descriptor
	 * @return The merged descriptor
	 */
	public static Descriptor merge(Descriptor d1, Descriptor d2) {
		Descriptor merged = new DescriptorSupport();
		merged.setFields(d2.getFieldNames(), d2.getFieldValues(d2.getFieldNames()));
		merged.setFields(d1.getFieldNames(), d1.getFieldValues(d1.getFieldNames()));
		return merged;
	}
	
	
	
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
	 * Returns a map of sets of methods in the passed class that are annotated with the passed annotation types, keyed by the annotation type
	 * @param clazz The class to inspect
	 * @param annotationTypes The annotations to inspect for
	 * @return a [possibly empty] map of [possibly empty] sets of annotated methods
	 */
	@SafeVarargs
	public static Map<Class<? extends Annotation>, Set<Method>> getAnnotatedMethods(Class<?> clazz, Class<? extends Annotation>...annotationTypes) {
		Map<Class<? extends Annotation>, Set<Method>> methodMap = new HashMap<Class<? extends Annotation>, Set<Method>>();
		for(Class<? extends Annotation> annType: annotationTypes) {
			methodMap.put(annType, new HashSet<Method>());
		}
				
		for(Method m: clazz.getMethods()) {
			for(Class<? extends Annotation> annType: annotationTypes) {
				if(m.getAnnotation(annType)!=null) {
					methodMap.get(annType).add(m);
				}
			}
		}
		for(Method m: clazz.getDeclaredMethods()) {
			for(Class<? extends Annotation> annType: annotationTypes) {
				if(m.getAnnotation(annType)!=null) {
					methodMap.get(annType).add(m);
				}
			}
		}
		return methodMap;
	}	
	
	
	/**
	 * Returns an attribute name for the passed getter method
	 * @param m The method to get an attribute name for
	 * @return the attribute name
	 */
	public static String attr(Method m) {
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
