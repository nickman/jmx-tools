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

import static org.helios.jmx.annotation.Reflector.attr;
import static org.helios.jmx.annotation.Reflector.nvl;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.openmbean.OpenType;

import org.cliffc.high_scale_lib.NonBlockingHashMapLong;
import org.helios.jmx.managed.Invoker;
import org.helios.jmx.managed.ManagedByteCodeFactory;
import org.helios.jmx.managed.MutableMBeanAttributeInfo;
import org.helios.jmx.managed.MutableMBeanOperationInfo;
import org.helios.jmx.opentypes.OpenTypeFactory;
import org.helios.jmx.util.helpers.StringHelper;
/**
 * <p>Title: ManagedMetricImpl</p>
 * <p>Description: A concrete bean representing an extracted {@link ManagedMetric}.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.annotation.ManagedMetricImpl</code></p>
 */

public class ManagedMetricImpl {
	/** A description of the metric  */
	protected final String description;
	/** The name of the metric as exposed in the MBeanAttributeInfo. */
	protected final String displayName;
	/** The type of the metric */
	protected final MetricType metricType;
	/** The optional unit of the metric */
	protected final String unit;
	/** An array of managed notifications that may be emitted by the annotated managed attribute */
	protected final ManagedNotificationImpl[] notifications;
	/**
	 * Indicates if the metric type is rendered as a CompositeType 
	 * and can be enhanced to surface the type's fields as first class mbean attributes 
	 */
	protected final boolean popable;
	
	
	/**
	 * The metric category describing the class or package that the metric is grouped into.
	 * The default blamk value indicates that the containing class's 
	 * {@link MetricGroup} annotation should be read for this value.
	 */
	protected final String category;
	/** An optional arbitrary content descriptor for this metric which could be JSON, XML or CSV etc. */
	protected final String descriptor;
	/** The metric subkeys */
	protected final String[] subkeys;
	/** The metric submetrics */
	protected final String[] subMetricKeys;
	/** MBean attribute info descriptor content */
	protected final Map<String, Object> descriptorMap = new HashMap<String, Object>();
	
	/** empty const array */
	public static final ManagedMetricImpl[] EMPTY_ARR = {};
	/** empty const array */
	public static final MBeanAttributeInfo[] EMPTY_INFO_ARR = {};
	
	

	
	/**
	 * Converts an array of ManagedMetrics to an array of ManagedMetricImpls
	 * @param methods The annotated methods
	 * @param metrics the array of ManagedMetrics to convert
	 * @return a [possibly zero length] array of ManagedMetricImpls
	 */
	public static ManagedMetricImpl[] from(Method[] methods, ManagedMetric...metrics) {
		if(metrics==null || metrics.length==0 || methods==null || methods.length==0) return EMPTY_ARR;
		if(methods.length != metrics.length) {
			throw new IllegalArgumentException("Method/Metrics Array Size Mismatch. Types:" + methods.length + ", Metrics:" + metrics.length);
		}		
		ManagedMetricImpl[] mopis = new ManagedMetricImpl[metrics.length];
		for(int i = 0; i < metrics.length; i++) {
			mopis[i] = new ManagedMetricImpl(methods[i], metrics[i]);
		}
		return mopis;		
	}
	
	/**
	 * Generates an array of MBeanAttributeInfos for the passed array of ManagedMetricImpls
	 * @param methods An array of metric accessor methods, one for each managed ManagedMetricImpl
	 * @param metricInvokers A map of invokers to populate
	 * @param metrics The ManagedMetricImpls to convert
	 * @return a [possibly zero length] array of MBeanAttributeInfos
	 */
	public static MBeanAttributeInfo[] from(Method[] methods, final NonBlockingHashMapLong<Invoker[]> metricInvokers, ManagedMetricImpl...metrics) {
		if(metrics==null || metrics.length==0 || methods==null || methods.length==0) return EMPTY_INFO_ARR;
		if(methods.length != metrics.length) {
			throw new IllegalArgumentException("Method/Metric Array Size Mismatch. Methods:" + methods.length + ", Metrics:" + metrics.length);
		}
		
		MBeanAttributeInfo[] infos = new MBeanAttributeInfo[metrics.length];
		for(int i = 0; i < infos.length; i++) {
			infos[i] = metrics[i].toMBeanInfo(methods[i], metricInvokers);
		}		
		return infos;		
	}

	
	/**
	 * Creates a new ManagedMetricImpl
	 * @param popable true if the metric is popable, false otherwise
	 * @param description A description of the metric
	 * @param displayName The name of the metric as exposed in the MBeanAttributeInfo
	 * @param metricType The type of the metric
	 * @param category The metric category describing the class or package that the metric is grouped into.
	 * @param unit The optional unit of the metric
	 * @param descriptor An optional arbitrary content descriptor for this metric which could be JSON, XML or CSV etc.
	 * @param notifications An array of managed notifications that may be emitted by the annotated managed attribute
	 * @param subkeys The metric subkeys
	 */
	ManagedMetricImpl(boolean popable, String description, String displayName, MetricType metricType, String category, String unit, String descriptor, ManagedNotificationImpl[] notifications, String...subkeys) {
		this.description = nvl(description, "description");
		this.displayName = nvl(displayName, "displayName");
		this.metricType = nvl(metricType, "metricType");
		this.category = nvl(category, "category");
		this.unit = unit;		
		this.descriptor = descriptor;
		this.subkeys = subkeys;
		subMetricKeys = new String[0];
		this.notifications = notifications==null ? ManagedNotificationImpl.EMPTY_ARR : notifications; 	
		this.popable = popable;
	}
	
	/**
	 * Creates a new ManagedMetricImpl
	 * @param popable true if the metric is popable, false otherwise
	 * @param description A description of the metric
	 * @param displayName The name of the metric as exposed in the MBeanAttributeInfo
	 * @param metricType The type of the metric
	 * @param category The metric category describing the class or package that the metric is grouped into.
	 * @param unit The optional unit of the metric
	 * @param descriptor An optional arbitrary content descriptor for this metric which could be JSON, XML or CSV etc.
	 * @param notifications An array of managed notifications that may be emitted by the annotated managed attribute
	 * @param subkeys The metric subkeys
	 */
	ManagedMetricImpl(boolean popable, String description, String displayName, MetricType metricType, String category, String unit, String descriptor, ManagedNotification[] notifications, String...subkeys) {
		this(popable, description, displayName, metricType, category, unit, descriptor,ManagedNotificationImpl.from(notifications),  subkeys);
	}
	
	/**
	 * Creates a new ManagedMetricImpl
	 * @param method The annotated method
	 * @param managedMetric The managed metric instance to ingest
	 */
	public ManagedMetricImpl(Method method, ManagedMetric managedMetric) {
		if(managedMetric==null) throw new IllegalArgumentException("The passed managed metric was null");
		MetricGroup mg = method.getDeclaringClass().getAnnotation(MetricGroup.class);
		displayName = managedMetric.displayName();
		metricType = managedMetric.metricType();			
		descriptor = managedMetric.descriptor().isEmpty() ? null : managedMetric.descriptor();
		unit = managedMetric.unit().isEmpty() ? null : managedMetric.unit();		
		notifications = ManagedNotificationImpl.from(managedMetric.notifications());
		popable = managedMetric.popable();
		//===========
		if(mg==null) {
			description = managedMetric.description();
			category = managedMetric.category();
			subkeys = managedMetric.subkeys();
		} else {
			description = mg.description() + " | " + managedMetric.description();
			category = mg.category() + " | " + managedMetric.category();
			String[] _subKeys1 = managedMetric.subkeys();
			String[] _subKeys2 = mg.subkeys();
			subkeys = new String[_subKeys1.length + _subKeys2.length];
			System.arraycopy(_subKeys1, 0, subkeys, 0, _subKeys1.length);
			System.arraycopy(_subKeys2, 0, subkeys, _subKeys1.length+1, _subKeys2.length);			
		}
		MBeanInfo subMBeanInfo = subObject(method, displayName);
		Set<String> attrNames = new HashSet<String>(); 
		if(subMBeanInfo!=null) {
			for(MBeanAttributeInfo i: subMBeanInfo.getAttributes()) {
				attrNames.add(i.getName());
			}
		}
		
		subMetricKeys = attrNames.toArray(new String[attrNames.size()]);
		descriptorMap.put("subMetricKeys", subMetricKeys);
		descriptorMap.put("subMetricInfo", subMBeanInfo);
	}
	
	/**
	 * Attempts to gather an MBeanInfo for the type returned by the passed method
	 * @param method The method to inspect
	 * @param prefix The prefix to prepend to the attribute and op names
	 * @return a sub MBeanInfo or null if the method had no sub meta-data
	 */
	protected MBeanInfo subObject(Method method, String prefix) {
		Class<?> clazz = method.getReturnType();
		MBeanInfo info = Reflector.from(clazz, null, null, null);
		MutableMBeanAttributeInfo[] prefixedAttrs = MutableMBeanAttributeInfo.from(prefix, info.getAttributes());		
		MutableMBeanOperationInfo[] prefixedOps = MutableMBeanOperationInfo.from(prefix, info.getOperations());
		return new MBeanInfo(info.getClassName(), info.getDescription(), MutableMBeanAttributeInfo.toImmutable(prefixedAttrs), info.getConstructors(), MutableMBeanOperationInfo.toImmutable(prefixedOps), info.getNotifications(), info.getDescriptor());
	}
	
	/**
	 * Returns the metric's subkeys
	 * @return the subkeys
	 */
	public String[] getSubkeys() {
		return subkeys;
	}

	/**
	 * Returns the metric description 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the name of the metric as exposed in the MBeanAttributeInfo
	 * @return the displayName
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Returns the metric type
	 * @return the metricType
	 */
	public MetricType getMetricType() {
		return metricType;
	}

	/**
	 * Returns the metric unit
	 * @return the unit or null if not defined
	 */
	public String getUnit() {
		return unit;
	}

	/**
	 * Returns the metric category
	 * @return the category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * Returns the metric descriptor.
	 * An arbitrary content descriptor for this metric which could be JSON, XML or CSV etc.
	 * @return the descriptor or null if one was not defined
	 */
	public String getDescriptor() {
		return descriptor;
	}
	
	/**
	 * Returns the array of managed notifications that may be emitted by the annotated managed attribute
	 * @return the notifications
	 */
	public ManagedNotificationImpl[] getNotifications() {
		return notifications;
	}
	
	/**
	 * Returns an MBeanAttributeInfo rendered form this ManagedMetricImpl.
	 * The readable flag is set to true, and the isIs and writable flag are set to false.
	 * @param method The method accessing the metric implementation
	 * @param metricInvokers  A map to add the invoker for this metric to
	 * @return MBeanAttributeInfo rendered form this ManagedMetricImpl
	 */
	public MBeanAttributeInfo toMBeanInfo(Method method, final NonBlockingHashMapLong<Invoker[]> metricInvokers) {		
		if(metricInvokers!=null) {
			long hash = StringHelper.longHashCode(getDisplayName());
			long mhash = StringHelper.longHashCode(method.getName());		
			try {
				Invoker[] methodHandles = new Invoker[]{ManagedByteCodeFactory.getInstance().newInvoker(method)};
				metricInvokers.put(hash, methodHandles);
				metricInvokers.put(mhash, methodHandles);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}						
		}
		OpenType<?> ot = null;
		try {
			ot=OpenTypeFactory.getInstance().openTypeForType(method.getReturnType());
		} catch (Exception ex) {}
		
		return new MBeanAttributeInfo(
				getDisplayName(),
				ot==null ? method.getReturnType().getName() : ot.getClassName(),
				getDescription(),
				true,
				false, 
				false,
				toDescriptor(method)
		);		
	}
	
	
	/**
	 * Generates a mutable MBean descriptor for this ManagedMeticImpl
	 * @param method The method we're creating a descriptor for
	 * @return a MBean descriptor
	 */
	public Descriptor toDescriptor(Method method) {
		return toDescriptor(method, false);
	}
	
	/**
	 * Generates a MBean descriptor for this ManagedMeticImpl
	 * @param method The method we're creating a descriptor for
	 * @param immutable true for an immutable descriptor, false otherwise
	 * @return a MBean descriptor
	 */
	public Descriptor toDescriptor(Method method, boolean immutable) {
		
		descriptorMap.put("signature", StringHelper.getMethodDescriptor(method));
		descriptorMap.put("getMethod", method.getName());
		if(getCategory()!=null) {
			descriptorMap.put("category", getCategory());
		} else {
			descriptorMap.put("category", category(method));
		}
		if(getDescriptor()!=null) descriptorMap.put("descriptor", getDescriptor());
		descriptorMap.put("metricType", getMetricType().name());		
		descriptorMap.put("subkeys", getSubkeys());
		descriptorMap.put("unit", getUnit());
		if(method.getName().startsWith("get") && method.getParameterTypes().length==0 && OpenTypeFactory.SIMPLE_TYPE_MAPPING.containsKey(method.getReturnType())) {
			descriptorMap.put("openType", OpenTypeFactory.SIMPLE_TYPE_MAPPING.get(method.getReturnType()));
			descriptorMap.put("originalType", method.getReturnType());
		} else {
			try {
				OpenType<?> ot = OpenTypeFactory.getInstance().openTypeForType(method.getReturnType());
				descriptorMap.put("openType", ot);
				descriptorMap.put("originalType", method.getReturnType().getName());
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
			}
		}
		return !immutable ?  new ImmutableDescriptor(descriptorMap) : new DescriptorSupport(descriptorMap.keySet().toArray(new String[descriptorMap.size()]), descriptorMap.values().toArray(new Object[descriptorMap.size()]));	
	}
	
	/**
	 * Checks the class and package of the annotated method for a backup category
	 * @param method the annotated method
	 * @return the category
	 */
	public static String category(Method method) {
		String cat = null;
		Class<?> clazz = method.getDeclaringClass();
		MetricGroup mg = clazz.getAnnotation(MetricGroup.class);
		if(mg!=null) {
			cat = mg.category();
		}
		if(cat==null || cat.isEmpty()) {
			mg = clazz.getPackage().getAnnotation(MetricGroup.class);
			if(mg!=null) {
				cat = mg.category();
			}			
		}		
		if(cat==null || cat.isEmpty()) {
			cat = clazz.getSimpleName() + "-" + attr(method);
		}
		return cat;
	}
	
	/**
	 * Indicates if the metric type is rendered as a CompositeType 
	 * and can be enhanced to surface the type's fields as first class mbean attributes 
	 * @return true if popable, false otherwise
	 */
	public boolean isPopable() {
		return popable;
	}		
	

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ManagedMetricImpl [description=");
		builder.append(description);
		builder.append(", displayName:");
		builder.append(displayName);
		builder.append(", metricType:");
		builder.append(metricType);
		builder.append(", category:");
		builder.append(category);
		if(unit!=null) {
			builder.append(", unit:");
			builder.append(unit);			
		}
		if(descriptor!=null) {
			builder.append(", descriptor:");
			builder.append(descriptor);
		}
		builder.append(", popable:").append(popable);
		builder.append(" ]");
		return builder.toString();
	}

}
