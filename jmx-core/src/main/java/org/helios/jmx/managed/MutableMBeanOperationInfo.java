/**
 * 
 */
package org.helios.jmx.managed;

import java.io.ObjectStreamException;
import java.lang.reflect.Method;

import javax.management.Descriptor;
import javax.management.DescriptorKey;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;

/**
 * <p>Title: MutableMBeanOperationInfo</p>
 * <p>Description: An extension of {@link MBeanOperationInfo} that allows updates to the name and description</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>org.helios.jmx.managed.MutableMBeanOperationInfo</code></b>
 */

public class MutableMBeanOperationInfo extends MBeanOperationInfo {
	
	/**  */
	private static final long serialVersionUID = 8711902910226312916L;

	/**
	 * Constructs an array of <CODE>MutableMBeanOperationInfo</CODE> object from an array of existing <CODE>MBeanOperationInfo</CODE>s
	 * @param prefix An optional prefix to prepend to each name
	 * @param infos an array of existing <CODE>MBeanOperationInfo</CODE>s to convert
	 * @return an array of <CODE>MutableMBeanOperationInfo</CODE>
	 */
	public static MutableMBeanOperationInfo[] from(String prefix, MBeanOperationInfo[] infos) {
		MutableMBeanOperationInfo[] mutableInfos = new MutableMBeanOperationInfo[infos.length];
		for(int i = 0; i < infos.length; i++) {
			mutableInfos[i] = new MutableMBeanOperationInfo(infos[i]).prefixName(prefix);
		}
		return mutableInfos;
	}
	
	/**
	 * Converts any mutable instances in the passed array back into a standard MBeanOperationInfo
	 * @param infos The array to convert
	 * @return tyhe converted array
	 */
	public static MBeanOperationInfo[] toImmutable(MBeanOperationInfo[] infos) {
		MBeanOperationInfo[] afos = new MBeanOperationInfo[infos.length];
		for(int i = 0; i < afos.length; i++) {
			if(infos[i] instanceof MutableMBeanOperationInfo) {
				afos[i] = ((MutableMBeanOperationInfo)infos[i]).toMBeanOperationInfo();
			} else {
				afos[i] = infos[i];
			}
		}
		return afos;
	}
	
	
	/**
	 * Prefixes all the names in the passed infos with the passed prefix
	 * @param prefix The prefix to prepend before each of the name
	 * @param infos the infos to prefix
	 * @return the prefix infos
	 */
	public static MutableMBeanOperationInfo[] prefixAllNames(String prefix, MutableMBeanOperationInfo...infos) {
		for(MutableMBeanOperationInfo info: infos) {
			info.prefixName(prefix);
		}
		return infos;
	}
	
	

	/**
	 * Creates a new <CODE>MutableMBeanOperationInfo</CODE> from an existing <CODE>MBeanOperationInfo</CODE> 
	 * @param info an existing <CODE>MBeanOperationInfo</CODE>
	 */
	public MutableMBeanOperationInfo(MBeanOperationInfo info) {
		this(info.getName(), info.getDescription(), info.getSignature(), info.getReturnType(), info.getImpact(), info.getDescriptor());
	}
	
    /**
     * Constructs an <CODE>MutableMBeanOperationInfo</CODE> object.  The
     * {@link Descriptor} of the constructed object will include
     * fields contributed by any annotations on the {@code Method}
     * object that contain the {@link DescriptorKey} meta-annotation.
     *
     * @param method The <CODE>java.lang.reflect.Method</CODE> object
     * describing the MBean operation.
     * @param description A human readable description of the operation.
     */
	public MutableMBeanOperationInfo(String description, Method method) {
		super(description, method);
	}

    /**
     * Constructs an <CODE>MutableMBeanOperationInfo</CODE> object.
     * @param name The name of the method.
     * @param description A human readable description of the operation.
     * @param signature <CODE>MBeanParameterInfo</CODE> objects
     * describing the parameters(arguments) of the method.  This may be
     * null with the same effect as a zero-length array.
     * @param type The type of the method's return value.
     * @param impact The impact of the method, one of
     * {@link #INFO}, {@link #ACTION}, {@link #ACTION_INFO},
     * {@link #UNKNOWN}.
     */
	public MutableMBeanOperationInfo(String name, String description,
			MBeanParameterInfo[] signature, String type, int impact) {
		super(name, description, signature, type, impact);
	}

    /**
     * Constructs an <CODE>MutableMBeanOperationInfo</CODE> object.
     * @param name The name of the method.
     * @param description A human readable description of the operation.
     * @param signature <CODE>MBeanParameterInfo</CODE> objects
     * describing the parameters(arguments) of the method.  This may be
     * null with the same effect as a zero-length array.
     * @param type The type of the method's return value.
     * @param impact The impact of the method, one of
     * {@link #INFO}, {@link #ACTION}, {@link #ACTION_INFO},
     * {@link #UNKNOWN}.
     * @param descriptor The descriptor for the operation.  This may be null
     * which is equivalent to an empty descriptor.
     */
	public MutableMBeanOperationInfo(String name, String description,
			MBeanParameterInfo[] signature, String type, int impact,
			Descriptor descriptor) {
		super(name, description, signature, type, impact, descriptor);
	}
	
	/**
	 * Sets a new name
	 * @param name The new name
	 * @return this MutableMBeanOperationInfo
	 */
	public MutableMBeanOperationInfo setName(String name) {
		this.name = name;
		return this;
	}
	
	/**
	 * Prefixes the current name
	 * @param prefix The prefix to insert before the name
	 * @return this MutableMBeanOperationInfo
	 */
	public MutableMBeanOperationInfo prefixName(String prefix) {
		if(prefix!=null && !prefix.trim().isEmpty()) {
			this.name = prefix + name;
		}
		return this;
	}
	
	
	/**
	 * Sets a new description
	 * @param description The new description
	 * @return this MutableMBeanOperationInfo
	 */
	public MutableMBeanOperationInfo setDescription(String description) {
		this.description = description;
		return this;
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanOperationInfo#clone()
	 */
	public MutableMBeanOperationInfo clone() {
		return new MutableMBeanOperationInfo(name, description, getSignature(), getReturnType(), getImpact(), getDescriptor());
	}
	
	/**
	 * Creates and returns a new immutable instance of the parent class from this info
	 * @return a new immutable instance of the parent class 
	 */
	public MBeanOperationInfo toMBeanOperationInfo() {
		return new MBeanOperationInfo(name, description, getSignature(), getReturnType(), getImpact(), getDescriptor());
	}
	
	/**
	 * Replaces this instance with an instance of the parent MBeanOperationInfo when serialized 
	 * @return an instance of the parent MBeanOperationInfo with the same properties
	 * @throws ObjectStreamException
	 */
	MBeanOperationInfo writeReplace() throws ObjectStreamException {
		return toMBeanOperationInfo(); 
	}
	
	

}
