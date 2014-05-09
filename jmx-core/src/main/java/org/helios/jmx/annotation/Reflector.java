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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanFeatureInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ObjectName;
import javax.management.modelmbean.DescriptorSupport;

import org.cliffc.high_scale_lib.NonBlockingHashMapLong;
import org.helios.jmx.util.helpers.JMXHelper;
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

	
	public static MBeanInfo from(Class<?> clazz, final NonBlockingHashMapLong<MethodHandle[]> attrInvokers, final NonBlockingHashMapLong<MethodHandle> opInvokers, final NonBlockingHashMapLong<MethodHandle[]> metricInvokers) {
		final Map<Class<? extends Annotation>, Set<Method>> methodMap = getAnnotatedMethods(clazz, SEARCH_ANNOTATIONS);
		final Set<MBeanNotificationInfo> notificationInfo = new HashSet<MBeanNotificationInfo>();
		final Set<MBeanAttributeInfo> attrInfos = new HashSet<MBeanAttributeInfo>();
		Collections.addAll(attrInfos, getManagedAttributeInfos(notificationInfo, methodMap.get(ManagedAttribute.class), attrInvokers));
		Collections.addAll(attrInfos, getManagedMetricInfos(notificationInfo, methodMap.get(ManagedMetric.class), metricInvokers));
		final Set<MBeanOperationInfo> opInfos = new HashSet<MBeanOperationInfo>(Arrays.asList(
				getManagedOperationInfos(notificationInfo, methodMap.get(ManagedOperation.class), opInvokers)
		));
		ManagedResource mr = clazz.getAnnotation(ManagedResource.class);
		ObjectName on = null; 
		String description = null;
		if(mr!=null) {
			ManagedResourceImpl mri  = new ManagedResourceImpl(mr);
			description = mri.getDescription();
			on = mri.getObjectName();
			Collections.addAll(notificationInfo, ManagedNotificationImpl.from(mri.getNotifications()));
		}
		if(description==null) description = clazz.getName() + " Management Interface";
		if(on == null)  on = JMXHelper.objectName(clazz);
		Descriptor descriptor = new DescriptorSupport(new String[] {"objectName"}, new Object[]{on}); 
		return new MBeanInfo(
				clazz.getName(), description, 
				attrInfos.toArray(new MBeanAttributeInfo[attrInfos.size()]),
				new MBeanConstructorInfo[0],
				opInfos.toArray(new MBeanOperationInfo[opInfos.size()]),
				unify(notificationInfo),
				descriptor
		);
	}
	
	public static void addAttributeInvokers(final Set<Method> methods, final Map<String, MethodHandle> attrInvokers) {
		if(attrInvokers==null) return;
		for(Method method: methods) {
			
		}
	}
	
	/**
	 * Filters out the unique MBeanNotificationInfos from the passed set of infos
	 * @param infos The set to filter
	 * @return the unique infos
	 */
	public static MBeanNotificationInfo[] unify(Set<MBeanNotificationInfo> infos) {
		if(infos.isEmpty()) return new MBeanNotificationInfo[0]; 
		Set<MBeanNotificationInfo> uniqueInfos = new HashSet<MBeanNotificationInfo>();
		uniqueInfos.addAll(infos);
		// FIXME
		return uniqueInfos.toArray(new MBeanNotificationInfo[uniqueInfos.size()]);
	}
	
	/**
	 * Reflects out the MBeanOperationInfos for @ManagedOperation annotations the passed methods
	 * @param notificationInfo Any notification infos we find along the way get dropped in here
	 * @param methods The annotated methods in the class
	 * @param opInvokers The map of op invokers to populate
	 * @return a [possibly zero length] MBeanOperationInfo array
	 */
	public static MBeanOperationInfo[] getManagedOperationInfos(final Set<MBeanNotificationInfo> notificationInfo, final Set<Method> methods, final NonBlockingHashMapLong<MethodHandle> opInvokers) {
		Set<MBeanOperationInfo> infos = new HashSet<MBeanOperationInfo>(methods.size());
		for(Method m: methods) {
			ManagedOperation mo = m.getAnnotation(ManagedOperation.class);
			ManagedOperationImpl moi = new ManagedOperationImpl(m.getName(), mo);
			Collections.addAll(notificationInfo, ManagedNotificationImpl.from(moi.getNotifications()));
			infos.add(moi.toMBeanInfo(m, opInvokers));
		}
		return infos.toArray(new MBeanOperationInfo[infos.size()]);
	}
	

	/**
	 * Reflects out the ManagedAttributeInfos for @ManagedMetric annotations the passed methods
	 * @param notificationInfo Any notification infos we find along the way get dropped in here
	 * @param methods The annotated methods in the class
	 * @param metricInvokers A map of metric invokers to populate
	 * @return a [possibly zero length] MBeanAttributeInfo array
	 */
	public static MBeanAttributeInfo[] getManagedMetricInfos(final Set<MBeanNotificationInfo> notificationInfo, final Set<Method> methods, final NonBlockingHashMapLong<MethodHandle[]> metricInvokers) {
		Set<MBeanAttributeInfo> infos = new HashSet<MBeanAttributeInfo>(methods.size());
		for(Method m: methods) {
			ManagedMetric mm = m.getAnnotation(ManagedMetric.class);
			ManagedMetricImpl mmi = new ManagedMetricImpl(mm);
			Collections.addAll(notificationInfo, ManagedNotificationImpl.from(mmi.getNotifications()));
			infos.add(mmi.toMBeanInfo(m, metricInvokers));
		}
		return infos.toArray(new MBeanAttributeInfo[infos.size()]);
	}

	/**
	 * Reflects out the ManagedAttributeInfos for @ManagedAttribute annotations the passed methods
	 * @param notificationInfo Any notification infos we find along the way get dropped in here
	 * @param attrs The annotated methods in the class
	 * @param attrInvokers A map of attribute invoker pairs to populate
	 * @return a [possibly zero length] MBeanAttributeInfo array
	 */
	public static MBeanAttributeInfo[] getManagedAttributeInfos(final Set<MBeanNotificationInfo> notificationInfo, final Set<Method> attrs, final NonBlockingHashMapLong<MethodHandle[]> attrInvokers) {
		Set<MBeanAttributeInfo> infos = new HashSet<MBeanAttributeInfo>(attrs.size());
		Map<String, MBeanAttributeInfo[]> attributes = new HashMap<String, MBeanAttributeInfo[]>(attrs.size());
		for(Method m: attrs) { 
			final int index = m.getParameterTypes().length;
			if(index>1) throw new RuntimeException(String.format("The method [%s.%s] (%s)] is neither a getter or a setter but was annotated @ManagedAttribute", m.getDeclaringClass().getName(), m.getName(), StringHelper.getMethodDescriptor(m)));
			ManagedAttribute ma = m.getAnnotation(ManagedAttribute.class);
			ManagedAttributeImpl maImpl = new ManagedAttributeImpl(attr(m), ma);
			Collections.addAll(notificationInfo, ManagedNotificationImpl.from(maImpl.getNotifications()));
			Class<?> type = index==0 ?  m.getReturnType() : m.getParameterTypes()[0];
			MBeanAttributeInfo minfo = maImpl.toMBeanInfo(m, attrInvokers);
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
	
	
	
//	/**
//	 * Returns an array of ManagedMetricImpls extracted from the passed class
//	 * @param clazz The class to extract from
//	 * @return a [possibly zero length] array of ManagedMetricImpls 
//	 */
//	public static ManagedMetricImpl[] from(Class<?> clazz) {
//		Method[] methods = getAnnotatedMethods(clazz, ManagedMetric.class);
//		if(methods.length==0) return EMPTY_ARR;
//		ManagedMetricImpl[] impls = new ManagedMetricImpl[methods.length];
//		for(int i = 0; i < methods.length; i++) {
//			impls[i] = managedMetricImplFrom(methods[i]);
//		}
//		return impls;
//	}
	
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
	
	
	
//	/**
//	 * Returns an array of MBeanAttributeInfos extracted from the passed class
//	 * @param clazz The class to extract from
//	 * @return a [possibly zero length] array of MBeanAttributeInfos 
//	 */
//	public static MBeanAttributeInfo[] mbeanMetrics(Class<?> clazz) {
//		Method[] methods = getAnnotatedMethods(clazz, ManagedMetric.class);
//		if(methods.length==0) return EMPTY_MAI_ARR;
//		MBeanAttributeInfo[] impls = new MBeanAttributeInfo[methods.length];
//		for(int i = 0; i < methods.length; i++) {
//			impls[i] = mbeanMetric(methods[i]);
//		}
//		return impls;
//	}
	
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
	
	/**
	 * Inspects each descriptor and removes any non-serializable values
	 * @param info the MBeanInfo to clean
	 * @return The cleaned MBeanInfo
	 */
	public static MBeanInfo clean(MBeanInfo info) {
		Descriptor d = info.getDescriptor();
		if(d!=null) {
			for(String s: d.getFieldNames()) {
				if(!(d.getFieldValue(s) instanceof Serializable)) {
					d.removeField(s);
				}
			}
		}
		System.out.println("MBeanInfo Descriptor:" + Arrays.toString(d.getFieldNames()));
		for(MBeanAttributeInfo mi: info.getAttributes()) {
			d = mi.getDescriptor();
			if(d!=null) {
				for(String s: d.getFieldNames()) {
					if(!(d.getFieldValue(s) instanceof Serializable)) {
						d.setField(s, null);
					}
				}
			}
			
			System.out.println("MBeanAttributeInfo Descriptor:" + Arrays.toString(d.getFieldNames()));
		}
		for(MBeanOperationInfo mi: info.getOperations()) {
			d = mi.getDescriptor();
			if(d!=null) {
				for(String s: d.getFieldNames()) {
					if(!(d.getFieldValue(s) instanceof Serializable)) {
						d.removeField(s);
					}
				}
			}
		}
		
		return info;
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
	
	public static class MBeanAttributeInfoComp implements Comparator<MBeanAttributeInfo> {
		@Override
		public int compare(MBeanAttributeInfo o1, MBeanAttributeInfo o2) {
			return o1.getName().equals(o2.getName()) ? 0 : 1;			
		}
	}
	
	
	public static class MBeanOperationInfoComp implements Comparator<MBeanOperationInfo> {
		@Override
		public int compare(MBeanOperationInfo o1, MBeanOperationInfo o2) {
			return (o1.getName().equals(o2.getName()) && Arrays.equals(o1.getSignature(), o2.getSignature())) ? 0 : 1;			
		}
	}
	
	public static class MBeanConstructorInfoComp implements Comparator<MBeanConstructorInfo> {
		@Override
		public int compare(MBeanConstructorInfo o1, MBeanConstructorInfo o2) {
			return (o1.getName().equals(o2.getName()) && Arrays.equals(o1.getSignature(), o2.getSignature())) ? 0 : 1;			
		}
	}
	
	public static class MBeanNotificationInfoComp implements Comparator<MBeanNotificationInfo> {
		@Override
		public int compare(MBeanNotificationInfo o1, MBeanNotificationInfo o2) {
			return (o1.getName().equals(o2.getName()) && Arrays.equals(o1.getNotifTypes(), o2.getNotifTypes())) ? 0 : 1;			
		}
	}
	
	public static class DescriptorComp implements Comparator<Descriptor> {
		@Override
		public int compare(Descriptor o1, Descriptor o2) {
			return (o1.getFieldNames().equals(o2.getFieldNames()) && Arrays.equals(o1.getFieldValues(o1.getFieldNames()), o2.getFieldValues(o2.getFieldNames()) )) ? 0 : 1;			
		}
	}
	
	
	/** Static shareable MBeanAttributeInfo filter */
	public static final MBeanAttributeInfoComp ATTR_COMP = new MBeanAttributeInfoComp();
	/** Static shareable MBeanOperationInfo filter */
	public static final MBeanOperationInfoComp OPS_COMP = new MBeanOperationInfoComp();
	/** Static shareable MBeanConstructorInfo filter */
	public static final MBeanConstructorInfoComp CTOR_COMP = new MBeanConstructorInfoComp();
	/** Static shareable MBeanNotificationInfo filter */
	public static final MBeanNotificationInfoComp NOTIF_COMP = new MBeanNotificationInfoComp();
	/** Static shareable MBeanNotificationInfo filter */
	public static final DescriptorComp DESCRIPTOR_COMP = new DescriptorComp();
	
	
	
	/** Empty MBeanAttributeInfo array const */
	public static final MBeanAttributeInfo[] EMPTY_ATTRS_INFO = {};
	/** Empty MBeanConstructorInfo array const */
	public static final MBeanConstructorInfo[] EMPTY_CTORS_INFO = {};
	/** Empty MBeanOperationInfo array const */
	public static final MBeanOperationInfo[] EMPTY_OPS_INFO = {};
	/** Empty MBeanNotificationInfo array const */
	public static final MBeanNotificationInfo[] EMPTY_NOTIF_INFO = {};
	/** Empty MBeanInfo const */
	public static final MBeanInfo EMPTY_MBEAN_INFO = new MBeanInfo("", "" , EMPTY_ATTRS_INFO, EMPTY_CTORS_INFO, EMPTY_OPS_INFO, EMPTY_NOTIF_INFO); 
	
	public static MBeanInfoMerger newMerger(MBeanInfo baseInfo) {
		return new MBeanInfoMerger(baseInfo);
	}
	
	public static class MBeanInfoMerger {
		private String className = null;
		private String description = null;
		private Descriptor rootDescriptor = null;
		private final TreeSet<MBeanAttributeInfo> attributeInfos = new TreeSet<MBeanAttributeInfo>(ATTR_COMP);
		private final TreeSet<MBeanConstructorInfo> ctorInfos = new TreeSet<MBeanConstructorInfo>(CTOR_COMP);
		private final TreeSet<MBeanOperationInfo> opInfos = new TreeSet<MBeanOperationInfo>(OPS_COMP);
		private final TreeSet<MBeanNotificationInfo> notifInfos = new TreeSet<MBeanNotificationInfo>(NOTIF_COMP);
		private final TreeSet<Descriptor> descriptors = new TreeSet<Descriptor>(DESCRIPTOR_COMP);
		
		private Set decode(MBeanFeatureInfo info) {
			if(info instanceof MBeanAttributeInfo) return attributeInfos;
			else if(info instanceof MBeanConstructorInfo) return ctorInfos;
			else if(info instanceof MBeanOperationInfo) return opInfos;
			else if(info instanceof MBeanNotificationInfo) return notifInfos;
			else throw new RuntimeException("MBeanInfoMerger does not support [" + info.getClass().getName() + "]");
		}
		
		public MBeanInfoMerger() {
			
		}
		
		public MBeanInfoMerger(MBeanInfo baseInfo) {
			append(baseInfo);
		}
		
		/**
		 * Appends an array of MBeanFeatureInfos to the builder
		 * @param infos the MBeanFeatureInfos to append
		 * @return this builder
		 */
		public MBeanInfoMerger append(MBeanFeatureInfo...infos) {
			for(MBeanFeatureInfo info: infos) {
				if(info==null) continue;
				decode(info).add(info);				
			}
			return this;
		}

		/**
		 * Appends an array of MBeanInfos to the builder
		 * @param infos an array of MBeanInfos
		 * @return this builder
		 */
		public MBeanInfoMerger append(MBeanInfo...infos) {
			for(MBeanInfo info: infos) {
				if(infos==null) continue;
				if(className==null) className = info.getClassName();
				if(description==null) description = info.getDescription();
				append(info.getDescriptor());
				append(info.getAttributes());
				append(info.getConstructors());
				append(info.getOperations());
				append(info.getNotifications());
			}
			return this;
		}
		
		/**
		 * Appens descriptors to the builder
		 * @param descriptors the descriptors to append
		 * @return this builder
		 */
		public MBeanInfoMerger append(Descriptor...descriptors) {
			for(Descriptor descriptor : descriptors) {
				if(descriptor==null) continue;
				if(rootDescriptor==null) rootDescriptor = descriptor;
				else this.descriptors.add(descriptor);
			}
			return this;
		}		
		
		private Descriptor mergeDescriptors() {
			if(rootDescriptor==null) return new DescriptorSupport();			
			Descriptor base = rootDescriptor;
			for(Descriptor d: descriptors) {
				for(String fieldName: d.getFieldNames()) {
					if(Arrays.binarySearch(base.getFieldNames(), fieldName) >= 0) continue;
					base.setField(fieldName, d.getFieldValue(fieldName));
				}
			}
			return base;
		}
		
		
		/**
		 * Creates the final merged MBeanInfo
		 * @return the final merged MBeanInfo
		 */
		public MBeanInfo merge() {
			return new MBeanInfo(className, description, 
					attributeInfos.toArray(new MBeanAttributeInfo[0]),
					ctorInfos.toArray(new MBeanConstructorInfo[0]),
					opInfos.toArray(new MBeanOperationInfo[0]),
					notifInfos.toArray(new MBeanNotificationInfo[0]),
					mergeDescriptors()
			);
		}
	}
	
	

	private Reflector() {}

}
