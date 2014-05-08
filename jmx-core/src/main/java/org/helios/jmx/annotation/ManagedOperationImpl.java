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

import static org.helios.jmx.annotation.Reflector.nvl;
import static org.helios.jmx.annotation.Reflector.nws;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.modelmbean.DescriptorSupport;

/**
 * <p>Title: ManagedOperationImpl</p>
 * <p>Description: A concrete bean representing an extracted {@link ManagedOperation}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.annotation.ManagedOperationImpl</code></p>
 */

public class ManagedOperationImpl {
	/** The managed operation name. Defaults to the method name */
	protected final String name;
	/** The managed operation description */
	protected final String description;
	/** The managed operation impact */
	protected final int impact;
	/** The operation's managed parameters  */
	protected final ManagedOperationParameterImpl[] parameters;	
	/** An array of managed notifications that may be emitted by the annotated managed operation */
	protected final ManagedNotificationImpl[] notifications;
	
	/** empty const array */
	public static final ManagedOperationImpl[] EMPTY_ARR = {};
	
	/** empty const array */
	public static final MBeanOperationInfo[] EMPTY_INFO_ARR = {};
	
	
	/**
	 * Converts an array of ManagedOperations to an array of ManagedOperationImpls
	 * @param ops the array of ManagedOperations to convert
	 * @return a [possibly zero length] array of ManagedOperationImpls
	 */
	public static ManagedOperationImpl[] from(ManagedOperation...ops) {
		if(ops==null || ops.length==0) return EMPTY_ARR;
		ManagedOperationImpl[] mopis = new ManagedOperationImpl[ops.length];
		for(int i = 0; i < ops.length; i++) {
			mopis[i] = new ManagedOperationImpl(ops[i]);
		}
		return mopis;		
	}
	
	/**
	 * Generates an array of MBeanOperationInfos for the passed array of ManagedOperationImpls
	 * @param methods An array of methods, one for each ManagedOperationImpls
	 * @param ops The ManagedOperationImpls to convert
	 * @return a [possibly zero length] array of MBeanOperationInfos
	 */
	public static MBeanOperationInfo[] from(Method[] methods, ManagedOperationImpl...ops) {
		if(ops==null || ops.length==0) return EMPTY_INFO_ARR;
		if(methods.length != ops.length) {
			throw new IllegalArgumentException("Method/Ops Array Size Mismatch. Methods:" + methods.length + ", ManagedOps:" + ops.length);
		}
		MBeanOperationInfo[] infos = new MBeanOperationInfo[ops.length];
		for(int i = 0; i < infos.length; i++) {
			infos[i] = ops[i].toMBeanInfo(methods[i]);
		}		
		return infos;		
	}
	


	/**
	 * Creates a new ManagedOperationImpl
	 * @param mo The managed operation to extract from
	 */
	public ManagedOperationImpl(ManagedOperation mo) {
		name = nws(nvl(mo, "Managed Operation").name());
		description = nws(mo.description());
		impact = mo.impact();
		parameters = ManagedOperationParameterImpl.from(mo);
		notifications = ManagedNotificationImpl.from(mo.notifications());
	}
	
	/**
	 * Creates a new ManagedOperationImpl
	 * @param name The operation name specification
	 * @param description The operation description
	 * @param impact the operation impact
	 * @param notifications An array of managed notifications that may be emitted by the annotated managed operation
	 * @param parameters an array of the operation's managed parameters
	 */
	ManagedOperationImpl(CharSequence name, CharSequence description, int impact, ManagedNotification[] notifications, ManagedOperationParameter...parameters) {
		this.name = nws(name);
		this.description = nws(description);
		this.impact = impact;
		this.parameters = ManagedOperationParameterImpl.from(parameters);
		this.notifications = ManagedNotificationImpl.from(notifications); 
	}
	
	/**
	 * Creates a new ManagedOperationImpl
	 * @param name The operation name specification
	 * @param description The operation description
	 * @param impact the operation impact
	 * @param notifications An array of managed notifications that may be emitted by the annotated managed operation
	 * @param parameters an array of the operation's managed parameters
	 */
	ManagedOperationImpl(CharSequence name, CharSequence description, int impact, ManagedNotificationImpl[] notifications, ManagedOperationParameterImpl...parameters) {		
		this.name = nws(name);
		this.description = nws(description);
		this.impact = impact;
		this.notifications = notifications==null ? ManagedNotificationImpl.EMPTY_ARR : notifications; 
		this.parameters = parameters;
	}	
	

	/**
	 * Returns the managed operation name. Defaults to the method name 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the managed operation description
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * Returns the managed operation impact
	 * @return the impact
	 */
	public int getImpact() {
		return impact;
	}
	
	/**
	 * Returns the operation's managed parameters
	 * @return the parameters
	 */
	public ManagedOperationParameterImpl[] getParameters() {
		return parameters;
	}	
	
	/**
	 * Returns the array of managed notifications that may be emitted by the annotated managed operations
	 * @return the notifications
	 */
	public ManagedNotificationImpl[] getNotifications() {
		return notifications;
	}	
	
	/**
	 * Returns an MBeanOperationInfo rendered form this ManagedOperationImpl.
	 * @param method The method that this ManagedOperationImpl represents
	 * @return a MBeanOperationInfo rendered form this ManagedOperationImpl
	 */
	public MBeanOperationInfo toMBeanInfo(Method method) {		
		Class<?>[] sig = method.getParameterTypes();
		if(sig.length != parameters.length) {
			throw new IllegalArgumentException("Parameter Mismatch. Method:" + sig.length + ", ManagedParams:" + parameters.length);
		}
		return new MBeanOperationInfo(
				name,
				description,
				ManagedOperationParameterImpl.from(method.getParameterTypes(), parameters),
				method.getReturnType().getName(),
				impact,
				toDescriptor()
		);		
	}
	
	
	/**
	 * Generates a mutable MBean descriptor for this ManagedOperationImpl
	 * @return a MBean descriptor
	 */
	public Descriptor toDescriptor() {
		return toDescriptor(false);
	}
	
	/**
	 * Generates a MBean descriptor for this ManagedOperationImpl
	 * @param immutable true for an immutable descriptor, false otherwise
	 * @return a MBean descriptor
	 */
	public Descriptor toDescriptor(boolean immutable) {
		Map<String, Object> map = new HashMap<String, Object>();
		// FIXME:
		return !immutable ?  new ImmutableDescriptor(map) : new DescriptorSupport(map.keySet().toArray(new String[map.size()]), map.values().toArray(new Object[map.size()]));	
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("ManagedOperationImpl [name:%s, description:%s]", name==null ? "none" : name, description==null ? "none" : description);
	}

}
