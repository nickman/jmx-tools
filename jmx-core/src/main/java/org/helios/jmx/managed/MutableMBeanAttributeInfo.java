/**
 * 
 */
package org.helios.jmx.managed;

import java.io.ObjectStreamException;
import java.lang.reflect.Method;

import javax.management.Descriptor;
import javax.management.DescriptorKey;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;

/**
 * <p>Title: MutableMBeanAttributeInfo</p>
 * <p>Description: An extension of {@link MBeanAttributeInfo} that allows updates to the name and description</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>org.helios.jmx.managed.MutableMBeanAttributeInfo</code></b>
 */

public class MutableMBeanAttributeInfo extends MBeanAttributeInfo {

	/**  */
	private static final long serialVersionUID = 7484376763757896297L;

	/**
	 * Constructs an array of <CODE>MutableMBeanAttributeInfo</CODE> object from an array of existing <CODE>MBeanAttributeInfo</CODE>s
	 * @param prefix An optional prefix to prepend to each name
	 * @param infos an array of existing <CODE>MBeanAttributeInfo</CODE>s to convert
	 * @return an array of <CODE>MutableMBeanAttributeInfo</CODE>
	 */
	public static MutableMBeanAttributeInfo[] from(String prefix, MBeanAttributeInfo[] infos) {
		MutableMBeanAttributeInfo[] mutableInfos = new MutableMBeanAttributeInfo[infos.length];
		for(int i = 0; i < infos.length; i++) {
			mutableInfos[i] = new MutableMBeanAttributeInfo(infos[i]).prefixName(prefix);
		}
		return mutableInfos;
	}
	
	/**
	 * Converts any mutable instances in the passed array back into a standard MBeanAttributeInfo
	 * @param infos The array to convert
	 * @return tyhe converted array
	 */
	public static MBeanAttributeInfo[] toImmutable(MBeanAttributeInfo[] infos) {
		MBeanAttributeInfo[] afos = new MBeanAttributeInfo[infos.length];
		for(int i = 0; i < afos.length; i++) {
			if(infos[i] instanceof MutableMBeanAttributeInfo) {
				afos[i] = ((MutableMBeanAttributeInfo)infos[i]).toMBeanAttributeInfo();
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
	public static MutableMBeanAttributeInfo[] prefixAllNames(String prefix, MutableMBeanAttributeInfo...infos) {
		for(MutableMBeanAttributeInfo info: infos) {
			info.prefixName(prefix);
		}
		return infos;
	}

	/**
	 * Constructs a <CODE>MutableMBeanAttributeInfo</CODE> object from an existing <CODE>MBeanAttributeInfo</CODE>
	 * @param info the existing info to create this guy from
	 */
	public MutableMBeanAttributeInfo(MBeanAttributeInfo info) {
		this(info.getName(), info.getType(), info.getDescription(), info.isReadable(), info.isWritable(), info.isIs(), info.getDescriptor());
	}

    /**
     * Constructs a <CODE>MutableMBeanAttributeInfo</CODE> object.
     * <p>This constructor takes the name of a simple attribute, and Method
     * objects for reading and writing the attribute.  The {@link Descriptor}
     * of the constructed object will include fields contributed by any
     * annotations on the {@code Method} objects that contain the
     * {@link DescriptorKey} meta-annotation.
     *
     * @param name The programmatic name of the attribute.
     * @param description A human readable description of the attribute.
     * @param getter The method used for reading the attribute value.
     *          May be null if the property is write-only.
     * @param setter The method used for writing the attribute value.
     *          May be null if the attribute is read-only.
     * @exception IntrospectionException There is a consistency
     * problem in the definition of this attribute.
     */
	public MutableMBeanAttributeInfo(String name, String description,
			Method getter, Method setter) throws IntrospectionException {
		super(name, description, getter, setter);
	}

    /**
     * Constructs a <CODE>MutableMBeanAttributeInfo</CODE> object.
     * @param name The name of the attribute.
     * @param type The type or class name of the attribute.
     * @param description A human readable description of the attribute.
     * @param isReadable True if the attribute has a getter method, false otherwise.
     * @param isWritable True if the attribute has a setter method, false otherwise.
     * @param isIs True if this attribute has an "is" getter, false otherwise.
     *
     * @throws IllegalArgumentException if {@code isIs} is true but
     * {@code isReadable} is not, or if {@code isIs} is true and
     * {@code type} is not {@code boolean} or {@code java.lang.Boolean}.
     * (New code should always use {@code boolean} rather than
     * {@code java.lang.Boolean}.)
     */
	public MutableMBeanAttributeInfo(String name, String type,
			String description, boolean isReadable, boolean isWritable,
			boolean isIs) {
		this(name, type, description, isReadable, isWritable, isIs, null);
	}

    /**
     * Constructs a <CODE>MutableMBeanAttributeInfo</CODE> object.
     * @param name The name of the attribute.
     * @param type The type or class name of the attribute.
     * @param description A human readable description of the attribute.
     * @param isReadable True if the attribute has a getter method, false otherwise.
     * @param isWritable True if the attribute has a setter method, false otherwise.
     * @param isIs True if this attribute has an "is" getter, false otherwise.
     * @param descriptor The descriptor for the attribute.  This may be null
     * which is equivalent to an empty descriptor.
     *
     * @throws IllegalArgumentException if {@code isIs} is true but
     * {@code isReadable} is not, or if {@code isIs} is true and
     * {@code type} is not {@code boolean} or {@code java.lang.Boolean}.
     * (New code should always use {@code boolean} rather than
     * {@code java.lang.Boolean}.)
     */
	public MutableMBeanAttributeInfo(String name, String type,
			String description, boolean isReadable, boolean isWritable,
			boolean isIs, Descriptor descriptor) {
		super(name, type, description, isReadable, isWritable, isIs, descriptor);
	}

	/**
	 * Sets a new name
	 * @param name The new name
	 * @return this MutableMBeanAttributeInfo
	 */
	public MutableMBeanAttributeInfo setName(String name) {
		this.name = name;
		return this;
	}
	
	/**
	 * Prefixes the current name
	 * @param prefix The prefix to insert before the name
	 * @return this MutableMBeanAttributeInfo
	 */
	public MutableMBeanAttributeInfo prefixName(String prefix) {
		if(prefix!=null && !prefix.trim().isEmpty()) {
			this.name = prefix + name;
		}
		return this;
	}
	
	
	/**
	 * Sets a new description
	 * @param description The new description
	 * @return this MutableMBeanAttributeInfo
	 */
	public MutableMBeanAttributeInfo setDescription(String description) {
		this.description = description;
		return this;
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanAttributeInfo#clone()
	 */
	public MutableMBeanAttributeInfo clone() {
		return new MutableMBeanAttributeInfo(name, getType(), description, isReadable(), isWritable(), isIs(), getDescriptor());
	}
	
	/**
	 * Creates and returns a new immutable instance of the parent class from this info
	 * @return a new immutable instance of the parent class 
	 */	
	public MBeanAttributeInfo toMBeanAttributeInfo() {
		return new MBeanAttributeInfo(name, getType(), description, isReadable(), isWritable(), isIs(), getDescriptor());
	}
	
	/**
	 * Replaces this instance with an instance of the parent MBeanAttributeInfo when serialized 
	 * @return an instance of the parent MBeanAttributeInfo with the same properties
	 * @throws ObjectStreamException
	 */
	MBeanAttributeInfo writeReplace() throws ObjectStreamException {
		return toMBeanAttributeInfo(); 
	}
	
	
	
}
