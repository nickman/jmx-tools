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
package org.helios.jmx.opentypes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.ObjectName;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularType;

import org.helios.jmx.opentypes.annotations.ACompositeType;
import org.helios.jmx.opentypes.annotations.ACompositeTypeItem;
import org.helios.jmx.opentypes.annotations.AOpenType;

import com.sun.jmx.mbeanserver.MXBeanMappingFactory;



/**
 * <p>Title: OpenTypeFactory</p>
 * <p>Description: Factory for creating open type instances from annotated concrete classes.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.opentypes.OpenTypeFactory</code></p>
 */

public class OpenTypeFactory {
	/** The singleton instance */
	private static volatile OpenTypeFactory instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();

	/** The com.sun internal MXBeanMappingFactory class name */
	public static final String MX_MAPPING_FACTORY_NAME = "com.sun.jmx.mbeanserver.MXBeanMappingFactory";
	/** The com.sun internal DefaultMXBeanMappingFactory class name */
	public static final String MX_DEFAULT_MAPPING_FACTORY_NAME = "com.sun.jmx.mbeanserver.DefaultMXBeanMappingFactory";
	
	/** The com.sun internal MXBeanMapping class name */
	public static final String MX_MAPPING_NAME = "com.sun.jmx.mbeanserver.MXBeanMapping";
	
	public static final Map<Class<?>, SimpleType> SIMPLE_TYPE_MAPPING;
	
	/** The com.sun internal MXBeanMappingFactory instance  */
	protected static final Object mxMappingFactory;
	/** The com.sun internal MXBeanMappingFactory mappingForType method  */
	protected static final Method mappingForType;
	/** The com.sun internal MXBeanMapping getOpenType method  */
	protected static final Method getOpenType;
	
	protected final MXBeanMappingFactory mxBeanMappingFactory;
	
	static {
		Map<Class<?>, SimpleType> simpleTypes = new HashMap<Class<?>, SimpleType>();
		simpleTypes.put(Void.class, SimpleType.VOID);
		simpleTypes.put(void.class, SimpleType.VOID);
		simpleTypes.put(Boolean.class, SimpleType.BOOLEAN);
		simpleTypes.put(boolean.class, SimpleType.BOOLEAN);
		simpleTypes.put(Character.class, SimpleType.CHARACTER);
		simpleTypes.put(char.class, SimpleType.CHARACTER);
		simpleTypes.put(Byte.class, SimpleType.BYTE);
		simpleTypes.put(byte.class, SimpleType.BYTE);
		simpleTypes.put(Short.class, SimpleType.SHORT);
		simpleTypes.put(short.class, SimpleType.SHORT);
		simpleTypes.put(Integer.class, SimpleType.INTEGER);
		simpleTypes.put(int.class, SimpleType.INTEGER);
		simpleTypes.put(Long.class, SimpleType.LONG);
		simpleTypes.put(long.class, SimpleType.LONG);
		simpleTypes.put(Float.class, SimpleType.FLOAT);
		simpleTypes.put(float.class, SimpleType.FLOAT);
		simpleTypes.put(Double.class, SimpleType.DOUBLE);
		simpleTypes.put(double.class, SimpleType.DOUBLE);
		simpleTypes.put(String.class, SimpleType.STRING);
		simpleTypes.put(BigDecimal.class, SimpleType.BIGDECIMAL);
		simpleTypes.put(BigInteger.class, SimpleType.BIGINTEGER);
		simpleTypes.put(Date.class, SimpleType.DATE);
		simpleTypes.put(ObjectName.class, SimpleType.OBJECTNAME);

		SIMPLE_TYPE_MAPPING = Collections.unmodifiableMap(simpleTypes);
		Object defaultMapper = null;
		Method mappingMethod = null;
		Method getOpenTypeMethod = null;
		try {
			Class<?> defaultFactoryClazz = Class.forName(MX_DEFAULT_MAPPING_FACTORY_NAME);
			Class<?> factoryClazz = Class.forName(MX_MAPPING_FACTORY_NAME);			
			defaultMapper = factoryClazz.getField("DEFAULT").get(null);
			
			mappingMethod = defaultFactoryClazz.getDeclaredMethod("mappingForType", Type.class, factoryClazz);
			getOpenTypeMethod = Class.forName(MX_MAPPING_NAME).getDeclaredMethod("getOpenType");
			System.out.println("Internal com.sun MXMapping Initialized");
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			System.err.println("Internal com.sun MXMapping Not Available");
		}
		mxMappingFactory = defaultMapper;
		mappingForType = mappingMethod;
		getOpenType = getOpenTypeMethod;
		
	}
	
	/** A cache of created composite types keyed by the type name */
	protected final Map<String, CompositeType> compositeTypes;
	/** A cache of simple types keyed by the underlying java type name */
	protected final Map<String, SimpleType<?>> simpleTypes;
	/** A cache of created array types keyed by the type name */
	protected final Map<String, ArrayType<?>> arrayTypes;
	/** A cache of created tabular types keyed by the type name */
	protected final Map<String, TabularType> tabularTypes;
	/** A cache of created open types that are not composite, simple, array or tabular keyed by the type name */
	protected final Map<String, OpenType<?>> openTypes;
	/** A master cache of open type caches */
	protected final Map<String, Map<String, ? extends OpenType<?>>> masterIndex;
	
	
	
	/**
	 * Acquires the OpenTypeFactory instance
	 * @return the OpenTypeFactory instance
	 */
	public static OpenTypeFactory getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new OpenTypeFactory();
				}
			}
		}
		return instance;
	}
	/**
	 * Creates a new OpenTypeFactory
	 */
	private OpenTypeFactory() {
		compositeTypes = new ConcurrentHashMap<String, CompositeType>();
		simpleTypes = new ConcurrentHashMap<String, SimpleType<?>>();
		arrayTypes = new ConcurrentHashMap<String, ArrayType<?>>();
		tabularTypes = new ConcurrentHashMap<String, TabularType>();
		openTypes = new ConcurrentHashMap<String, OpenType<?>>();
		masterIndex = new ConcurrentHashMap<String, Map<String, ? extends OpenType<?>>>();
		mxBeanMappingFactory = MXBeanMappingFactory.DEFAULT;
		
		
		try {
			for(Field f: SimpleType.class.getDeclaredFields()) {
				if(!Modifier.isStatic(f.getModifiers())) continue;
				if(!f.getType().equals(SimpleType.class)) continue;
				SimpleType<?> ot = (SimpleType<?>)f.get(null);
				simpleTypes.put(ot.getClassName(), ot);
			}
		} catch (Exception ex) {
			throw new RuntimeException("Failed to initialize OpenTypeFactory OpenType Cache", ex);
		}
	}
	
	public OpenType<?> openTypeForType(Type t) {
		try {
			return mxBeanMappingFactory.mappingForType(t, MXBeanMappingFactory.DEFAULT).getOpenType();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	protected OpenType<?> getOpenType(Class<?> clazz) {
		if(clazz==null) throw new IllegalArgumentException("The passed class was null");
		try {
			Object mxMapping = mappingForType.invoke(mxMappingFactory, clazz, mxMappingFactory);
			return (OpenType<?>)getOpenType.invoke(mxMapping);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to invoke MXFactory for open type on [" + clazz.getName() + "]", ex);
		}
	}
	
	public CompositeType compositeTypeFor(Class<?> annotatedClass) {
		if(annotatedClass==null) throw new IllegalArgumentException("The passed class was null");
		ACompositeType typeAnn = annotatedClass.getAnnotation(ACompositeType.class);
		if(typeAnn==null) throw new IllegalArgumentException("The class [" + annotatedClass.getName() + "] is not annotated with @ACompositeType");
		String typeName = typeAnn.name();
		if(typeName.isEmpty()) typeName = annotatedClass.getName() + "CompositeType";
		CompositeType compType = compositeTypes.get(typeName);
		if(compType==null) {
			synchronized(compositeTypes) {
				compType = compositeTypes.get(typeName);
				if(compType==null) {
					
				}
			}
		}
		return compType;
	}
	
	
	/**
	 * Builds a composite type for the passed class and type annotation
	 * @param clazz The class to build the composite type for
	 * @param typeAnn The classes type annotation
	 * @param typeName The composite type name
	 * @return the composite type
	 */
	protected CompositeType buildCompositeType(Class<?> clazz, ACompositeType typeAnn, String typeName) {
		CompositeType compType = null;
		try {			
			String typeDescriptions = typeAnn.description();
			Set<String> itemNames = new LinkedHashSet<String>();
			Set<String> itemDescriptions = new LinkedHashSet<String>();
			Set<OpenType<?>> itemTypes = new LinkedHashSet<OpenType<?>>();
			ACompositeTypeItem annot = null;
			String itemName = null;
			String itemDescription = null;
			
			for(Method m: getAnnotatedMethods(clazz, ACompositeTypeItem.class)) {
				annot = m.getAnnotation(ACompositeTypeItem.class);
				itemName = annot.name();
				itemDescription = annot.description();
				if(itemName.isEmpty()) {
					itemName = m.getName();
				}
				itemNames.add(itemName);
				if(itemDescription.isEmpty()) {
					itemDescription = m.getName() + " Value";
				}
				itemDescriptions.add(itemDescription);
				//itemTypes.add(annot.type());
				
				
			}
			return compType;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to build CompositeType for class [" + clazz.getName() + "]", ex);
		}
	}
	
	public OpenType<?> getOpenType(AOpenType typeAnn) {
		if(typeAnn==null) throw new IllegalArgumentException("The passed AOpenType was null");
		
		try {
			Class<?> javaType = typeAnn.type();
			String javaTypeName = javaType.getName();
			SimpleType<?> simpleType = simpleTypes.get(javaTypeName);
			if(simpleType==null) {
				synchronized(simpleTypes) {
					simpleType = simpleTypes.get(javaTypeName);
					if(simpleType==null) {
						String name = typeAnn.name();
						String description = typeAnn.description();
						if(name.isEmpty()) {
							name = javaTypeName + "OpenType";
						}
						if(description.isEmpty()) {
							 description = javaTypeName + "OpenType Value";
						}
						//openType = new OpenType(javaTypeName, name, description);
					}
				}
			}
//				Class<?> type();
//				String name() default "";
//				String  description() default "";
			
			
			return simpleType;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to build OpenType", ex);
		}
	}
	
	/**
	 * Returns the methods in the passed class that are annotated with the passed annotation type
	 * @param clazz The class to reflect
	 * @param annotationType The annotation type to search for
	 * @return a [possibly empty] set of annotated methods
	 */
	protected Collection<Method> getAnnotatedMethods(Class<?> clazz, Class<? extends Annotation> annotationType) {
		Map<String, Method> methods = new TreeMap<String, Method>();
		for(Method m: clazz.getMethods()) {
			Annotation ann = m.getAnnotation(annotationType);
			if(ann==null) continue;
			methods.put(getMethodDescriptor(m), m);
		}
		for(Method m: clazz.getDeclaredMethods()) {
			Annotation ann = m.getAnnotation(annotationType);
			if(ann==null) continue;
			methods.put(getMethodDescriptor(m), m);
		}
		return methods.values();
	}
	
	/**
	 * Returns the fields in the passed class that are annotated with the passed annotation type
	 * @param clazz The class to reflect
	 * @param annotationType The annotation type to search for
	 * @return a [possibly empty] set of annotated fields
	 */
	protected Collection<Field> getAnnotatedFields(Class<?> clazz, Class<? extends Annotation> annotationType) {
		Map<String, Field> fields = new TreeMap<String, Field>();
		for(Field f: clazz.getFields()) {
			Annotation ann = f.getAnnotation(annotationType);
			if(ann==null) continue;
			fields.put(getFieldDescriptor(f), f);
		}
		for(Field f: clazz.getDeclaredFields()) {
			Annotation ann = f.getAnnotation(annotationType);
			if(ann==null) continue;
			fields.put(getFieldDescriptor(f), f);			
		}
		return fields.values();
	}
	
	
	/**
     * Returns the descriptor corresponding to the given method.
     * @param m a {@link Method Method} object.
     * @return the descriptor of the given method.
     * (All credit to ObjectWeb ASM)
     * @author Eric Bruneton  
     * @author Chris Nokleberg
     */
    public static String getMethodDescriptor(final Method m) {
        Class<?>[] parameters = m.getParameterTypes();
        StringBuffer buf = new StringBuffer();
        buf.append('(');
        for (int i = 0; i < parameters.length; ++i) {
            getDescriptor(buf, parameters[i]);
        }
        buf.append(')');
        getDescriptor(buf, m.getReturnType());
        buf.insert(0, m.getName());
        return buf.toString();
    }
    
    /**
     * Returns a unique field descriptor comprised of the field name and type
     * @param f The field to describe
     * @return the field descriptor
     */
    public static String getFieldDescriptor(final Field f) {
    	StringBuffer buf = new StringBuffer(f.getName());
    	buf.append("(");
    	getDescriptor(buf, f.getType());
    	buf.append(")");
    	return buf.toString();
    }
    
    /**
     * Appends the descriptor of the given class to the given string buffer.
     * @param buf the string buffer to which the descriptor must be appended.
     * @param c the class whose descriptor must be computed.
     * (All credit to ObjectWeb ASM)
     * @author Eric Bruneton  
     * @author Chris Nokleberg
     */
    private static void getDescriptor(final StringBuffer buf, final Class<?> c) {
        Class<?> d = c;
        while (true) {
            if (d.isPrimitive()) {
                char car;
                if (d == Integer.TYPE) {
                    car = 'I';
                } else if (d == Void.TYPE) {
                    car = 'V';
                } else if (d == Boolean.TYPE) {
                    car = 'Z';
                } else if (d == Byte.TYPE) {
                    car = 'B';
                } else if (d == Character.TYPE) {
                    car = 'C';
                } else if (d == Short.TYPE) {
                    car = 'S';
                } else if (d == Double.TYPE) {
                    car = 'D';
                } else if (d == Float.TYPE) {
                    car = 'F';
                } else /* if (d == Long.TYPE) */{
                    car = 'J';
                }
                buf.append(car);
                return;
            } else if (d.isArray()) {
                buf.append('[');
                d = d.getComponentType();
            } else {
                buf.append('L');
                String name = d.getName();
                int len = name.length();
                for (int i = 0; i < len; ++i) {
                    char car = name.charAt(i);
                    buf.append(car == '.' ? '/' : car);
                }
                buf.append(';');
                return;
            }
        }
    }
    
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("Test");
		try {
			log("Abstract: [%s]", getMethodDescriptor(AbstractMap.class.getDeclaredMethod("put", Object.class, Object.class)));
			log("Hash: [%s]", getMethodDescriptor(HashMap.class.getDeclaredMethod("put", Object.class, Object.class)));
			
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}

	}
	
	public static void log(Object format, Object...args) {
		System.out.println(String.format(format.toString(), args));
	}

}
