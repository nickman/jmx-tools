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

import static org.helios.jmx.annotation.Reflector.descriptor;
import static org.helios.jmx.annotation.Reflector.managedMetricImplFrom;
import static org.helios.jmx.annotation.Reflector.nvl;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.modelmbean.DescriptorSupport;
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
	 * The metric category describing the class or package that the metric is grouped into.
	 * The default blamk value indicates that the containing class's 
	 * {@link MetricGroup} annotation should be read for this value.
	 */
	protected final String category;
	/** An optional arbitrary content descriptor for this metric which could be JSON, XML or CSV etc. */
	protected final String descriptor;
	/** The metric subkeys */
	protected final String[] subkeys;
	
	/** empty const array */
	public static final ManagedMetricImpl[] EMPTY_ARR = {};
	/** empty const array */
	public static final MBeanAttributeInfo[] EMPTY_INFO_ARR = {};

	
	/**
	 * Converts an array of ManagedMetrics to an array of ManagedMetricImpls
	 * @param metrics the array of ManagedMetrics to convert
	 * @return a [possibly zero length] array of ManagedMetricImpls
	 */
	public static ManagedMetricImpl[] from(ManagedMetric...metrics) {
		if(metrics==null || metrics.length==0) return EMPTY_ARR;
		ManagedMetricImpl[] mopis = new ManagedMetricImpl[metrics.length];
		for(int i = 0; i < metrics.length; i++) {
			mopis[i] = new ManagedMetricImpl(metrics[i]);
		}
		return mopis;		
	}
	
	/**
	 * Generates an array of MBeanAttributeInfos for the passed array of ManagedMetricImpls
	 * @param attrTypes An array of metric types, one for each managed ManagedMetricImpl
	 * @param metrics The ManagedMetricImpls to convert
	 * @return a [possibly zero length] array of MBeanAttributeInfos
	 */
	public static MBeanAttributeInfo[] from(Class<?>[] attrTypes, ManagedMetricImpl...metrics) {
		if(metrics==null || metrics.length==0 || attrTypes==null || attrTypes.length==0) return EMPTY_INFO_ARR;
		if(attrTypes.length != metrics.length) {
			throw new IllegalArgumentException("Type/Metric Array Size Mismatch. Types:" + attrTypes.length + ", Metrics:" + metrics.length);
		}
		
		MBeanAttributeInfo[] infos = new MBeanAttributeInfo[metrics.length];
		for(int i = 0; i < infos.length; i++) {
			infos[i] = metrics[i].toMBeanInfo(attrTypes[i]);
		}		
		return infos;		
	}

	
	/**
	 * Creates a new ManagedMetricImpl
	 * @param description A description of the metric
	 * @param displayName The name of the metric as exposed in the MBeanAttributeInfo
	 * @param metricType The type of the metric
	 * @param category The metric category describing the class or package that the metric is grouped into.
	 * @param unit The optional unit of the metric
	 * @param descriptor An optional arbitrary content descriptor for this metric which could be JSON, XML or CSV etc.
	 * @param notifications An array of managed notifications that may be emitted by the annotated managed attribute
	 * @param subkeys The metric subkeys
	 */
	ManagedMetricImpl(String description, String displayName, MetricType metricType, String category, String unit, String descriptor, ManagedNotificationImpl[] notifications, String...subkeys) {
		this.description = nvl(description, "description");
		this.displayName = nvl(displayName, "displayName");
		this.metricType = nvl(metricType, "metricType");
		this.category = nvl(category, "category");
		this.unit = unit;		
		this.descriptor = descriptor;
		this.subkeys = subkeys;
		this.notifications = notifications==null ? ManagedNotificationImpl.EMPTY_ARR : notifications; 		
	}
	
	/**
	 * Creates a new ManagedMetricImpl
	 * @param description A description of the metric
	 * @param displayName The name of the metric as exposed in the MBeanAttributeInfo
	 * @param metricType The type of the metric
	 * @param category The metric category describing the class or package that the metric is grouped into.
	 * @param unit The optional unit of the metric
	 * @param descriptor An optional arbitrary content descriptor for this metric which could be JSON, XML or CSV etc.
	 * @param notifications An array of managed notifications that may be emitted by the annotated managed attribute
	 * @param subkeys The metric subkeys
	 */
	ManagedMetricImpl(String description, String displayName, MetricType metricType, String category, String unit, String descriptor, ManagedNotification[] notifications, String...subkeys) {
		this(description, displayName, metricType, category, unit, descriptor,ManagedNotificationImpl.from(notifications),  subkeys);
	}
	
	/**
	 * Creates a new ManagedMetricImpl
	 * @param managedMetric The managed metric instance to ingest
	 */
	public ManagedMetricImpl(String methodName, ManagedMetric managedMetric) {
		if(managedMetric==null) throw new IllegalArgumentException("The passed managed metric was null");
		description = managedMetric.description();
		displayName = managedMetric.displayName();
		metricType = managedMetric.metricType();
		category = managedMetric.category();		
		descriptor = managedMetric.descriptor().isEmpty() ? null : managedMetric.descriptor();
		unit = managedMetric.unit().isEmpty() ? null : managedMetric.unit();
		subkeys = managedMetric.subkeys();
		notifications = ManagedNotificationImpl.from(managedMetric.notifications());
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
	 * @param attrType The type of the attribute
	 * @return MBeanAttributeInfo rendered form this ManagedMetricImpl
	 */
	public MBeanAttributeInfo toMBeanInfo(Class<?> attrType) {		
		return new MBeanAttributeInfo(
				getDisplayName(),
				attrType.getName(),
				getDescription(),
				true,
				false, 
				false,
				toDescriptor()
		);		
	}
	
	
	/**
	 * Generates a mutable MBean descriptor for this ManagedMeticImpl
	 * @return a MBean descriptor
	 */
	public Descriptor toDescriptor() {
		return toDescriptor(false);
	}
	
	/**
	 * Generates a MBean descriptor for this ManagedMeticImpl
	 * @param immutable true for an immutable descriptor, false otherwise
	 * @return a MBean descriptor
	 */
	public Descriptor toDescriptor(boolean immutable) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("category", getCategory());
		if(getDescriptor()!=null) map.put("descriptor", getDescriptor());
		map.put("metricType", getMetricType().name());		
		map.put("subkeys", getSubkeys());
		map.put("unit", getUnit());
		return !immutable ?  new ImmutableDescriptor(map) : new DescriptorSupport(map.keySet().toArray(new String[map.size()]), map.values().toArray(new Object[map.size()]));	
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
		builder.append("]");
		return builder.toString();
	}

}
