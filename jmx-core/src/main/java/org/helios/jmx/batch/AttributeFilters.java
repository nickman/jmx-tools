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
package org.helios.jmx.batch;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;

import org.cliffc.high_scale_lib.NonBlockingHashMap;

/**
 * <p>Title: AttributeFilters</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.batch.AttributeFilters</code></p>
 */


public class AttributeFilters {
	private static final Pattern COMP_NAME_SPLITTER = Pattern.compile("/");
	private static final Pattern COMMA_SPLITTER = Pattern.compile(",");
	private static final NonBlockingHashMap<ObjectName, Map<String, Class<?>>> objectNameTypeMappings = new NonBlockingHashMap<ObjectName, Map<String, Class<?>>>(); 
	private static final NonBlockingHashMap<ObjectName, Map<String, Class<?>>> objectNameNumericTypeMappings = new NonBlockingHashMap<ObjectName, Map<String, Class<?>>>();
	private static final IAttributeFilter ALL_FILTER = new AllAttributes();
	private static final IAttributeFilter NAMES_FILTER = new AttributeNameParser();
	private static final IAttributeFilter NUMERICS_FILTER = new NumericAttributes();

	public static final Map<String, Class<?>> primitives;
	public static final Map<String, Class<?>> primitiveNumerics;
	
	static {
		Map<String, Class<?>> tmp = new HashMap<String, Class<?>>();
		Map<String, Class<?>> tmp2 = new HashMap<String, Class<?>>();
		tmp.put(Byte.TYPE.getName(), Byte.TYPE);
		tmp2.put(Byte.TYPE.getName(), Byte.TYPE);
		tmp.put(Boolean.TYPE.getName(), Boolean.TYPE);
		tmp.put(Character.TYPE.getName(), Character.TYPE);
		tmp.put(Short.TYPE.getName(), Short.TYPE);
		tmp.put(Integer.TYPE.getName(), Integer.TYPE);
		tmp.put(Float.TYPE.getName(), Float.TYPE);
		tmp.put(Long.TYPE.getName(), Long.TYPE);
		tmp.put(Double.TYPE.getName(), Double.TYPE);

		
		tmp2.put(Short.TYPE.getName(), Short.TYPE);
		tmp2.put(Integer.TYPE.getName(), Integer.TYPE);
		tmp2.put(Float.TYPE.getName(), Float.TYPE);
		tmp2.put(Long.TYPE.getName(), Long.TYPE);
		tmp2.put(Double.TYPE.getName(), Double.TYPE);

		primitives = Collections.unmodifiableMap(tmp);
		primitiveNumerics = Collections.unmodifiableMap(tmp2);
	}
	
	public static IAttributeFilter filter(String attributes) {
		if(attributes==null || attributes.trim().length() < 2) {
			throw new RuntimeException("Invalid attribute filter [" + attributes + "]");
		}
		String prefix = attributes.substring(0, 2).toLowerCase();
		if(AllAttributes.prefix.equals(prefix)) return ALL_FILTER; 
		if(NumericAttributes.prefix.equals(prefix)) return NUMERICS_FILTER;
		if(AttributeNameParser.prefix.equals(prefix)) return NAMES_FILTER;
		throw new RuntimeException("Unrecognized attribute filter [" + attributes + "]");
	}
	
	private AttributeFilters() {
	}
	
	
	public static interface IAttributeFilter {
		
		public Map<ObjectName, Map<String, Object>> getAttributes(MBeanServer server, ObjectName objectName, QueryExp queryExp, String filter);
	}
	
	public static class AttributeNameParser implements IAttributeFilter {
		public static final String prefix = "a:";

		@Override
		public Map<ObjectName, Map<String, Object>> getAttributes(MBeanServer server, ObjectName objectName, QueryExp queryExp, String filter) {
			Map<ObjectName, Map<String, Object>> results = new HashMap<ObjectName, Map<String, Object>>();
			if(prefix.equals(filter.substring(0, 2).toLowerCase())) {
				filter = filter.substring(2);
			}
			String[] splitNames = COMMA_SPLITTER.split(filter);
			Set<String> names = new HashSet<String>(splitNames.length);
			for(String s: splitNames) {
				if(!s.trim().isEmpty()) {
					names.add(s.trim());
				}
			}
			try {
				for(ObjectName on: server.queryNames(objectName, queryExp)) {
					Map<String, Class<?>> typeMap = getAttrTypes(server, objectName);
					Set<String> keySet = new HashSet<String>(typeMap.keySet());
					keySet.retainAll(names);
					results.put(on, getAttributeValues(server, on, keySet));
				}				
				return results;
			} catch (Exception ex) {
				throw new RuntimeException("Failed to get attribute list for filter [" + objectName + "/" + filter + "]", ex);
			}
		}
		
	}
	
	public static class AllAttributes implements IAttributeFilter {
		public static final String prefix = "*:";

	
		@Override
		public Map<ObjectName, Map<String, Object>> getAttributes(MBeanServer server, ObjectName objectName, QueryExp queryExp, String filter) {
			Map<ObjectName, Map<String, Object>> results = new HashMap<ObjectName, Map<String, Object>>();
			try {
				for(ObjectName on: server.queryNames(objectName, queryExp)) {
					Map<String, Class<?>> typeMap = getAttrTypes(server, objectName);
					results.put(on, getAttributeValues(server, on, typeMap.keySet()));
				}
				return results;
			} catch (Exception ex) {
				throw new RuntimeException("Failed to get attribute list for filter [" + objectName + "/" + filter + "]", ex);
			}
		}		
	}
	
	public static class NumericAttributes implements IAttributeFilter {
		public static final String prefix = "n:";


		@Override
		public Map<ObjectName, Map<String, Object>> getAttributes(MBeanServer server, ObjectName objectName, QueryExp queryExp, String filter) {
			Map<ObjectName, Map<String, Object>> results = new HashMap<ObjectName, Map<String, Object>>();
			try {
				for(ObjectName on: server.queryNames(objectName, queryExp)) {
					Map<String, Class<?>> typeMap = getNumericAttrTypes(server, on);
					results.put(on, getAttributeValues(server, on, typeMap.keySet()));
				}
				return results;
			} catch (Exception ex) {
				throw new RuntimeException("Failed to get numeric attribute list for filter [" + objectName + "/" + filter + "]", ex);
			}
		}		
	}	
	
	
	
	/**
	 * Returns a map of the attribute values for an MBean
	 * @param server The MBeanServer
	 * @param objectName The MBean's ObjectName
	 * @param attrNames The attribute names to retrieve
 	 * @return a map of attribute values keyed by the attribute name
	 */
	public static Map<String, Object> getAttributeValues(MBeanServer server, ObjectName objectName, Set<String> attrNames) {
		Map<String, Object> values = new HashMap<String, Object>(attrNames.size());
		Set<String[]> composites = new HashSet<String[]>();
		Set<String> compositeNames = new HashSet<String>();
		Set<String> flats = new HashSet<String>();
		for(String s: attrNames) {
			if(s.indexOf('/')!=-1) {
				String[] parts = COMP_NAME_SPLITTER.split(s);
				composites.add(parts);
				compositeNames.add(parts[0]);
			} else {
				flats.add(s);
			}
		}
		try {
			AttributeList attrs = server.getAttributes(objectName, flats.toArray(new String[flats.size()]));
			for(Attribute attr: attrs.asList()) {
				values.put(attr.getName(), attr.getValue());
			}
			if(!compositeNames.isEmpty()) {
				Map<String, CompositeData> cinstances = new HashMap<String, CompositeData>(compositeNames.size());
				attrs = server.getAttributes(objectName, compositeNames.toArray(new String[compositeNames.size()]));
				for(Attribute attr: attrs.asList()) {
					cinstances.put(attr.getName(), (CompositeData)attr.getValue());
				}				
				for(String[] cname: composites) {
					CompositeData cd = (CompositeData)cinstances.get(cname[0]);
					values.put(cname[0] + "/" + cname[1], cd.get(cname[1]));
				}
			}
		} catch (Exception ex) {
			throw new RuntimeException("Failed to get attribute values for [" + objectName + "]", ex);
		}
		
		return values;
	}

	/**
	 * Returns a map of the attribute name to type mappings for the MBean in the passed MBeanServer identified by the passed ObjectName
	 * @param server The MBeanServer
	 * @param objectName The MBean's ObjectName
	 * @return a map of attribute to type mappings
	 */
	public static Map<String, Class<?>> getAttrTypes(MBeanServer server, ObjectName objectName) {
		Map<String, Class<?>> map =  objectNameTypeMappings.get(objectName);
		if(map==null) {
			synchronized(objectNameTypeMappings) {
				map =  objectNameTypeMappings.get(objectName);
				if(map==null) {
					map = new HashMap<String, Class<?>>();
					try {
						MBeanInfo info = server.getMBeanInfo(objectName);			
						ClassLoader classLoader = server.getClassLoaderFor(objectName);
						for(MBeanAttributeInfo ainfo: info.getAttributes()) {
							String name = ainfo.getName();
							String type = ainfo.getType();
							Class<?> clazz = primitives.get(type);
							if(clazz==null) clazz = Class.forName(type, true, classLoader);
							if(CompositeData.class.isAssignableFrom(clazz)) {
								CompositeData cd = (CompositeData)server.getAttribute(objectName, name);
								CompositeType ct = cd.getCompositeType();
								for(String key: ct.keySet()) {
									name = name + "/" + key;
									clazz = primitives.get(ct.getType(key).getClassName());
									if(clazz==null) clazz = Class.forName(ct.getType(key).getClassName(), true, classLoader);
								}
							}
							map.put(name,  clazz);
						}
						objectNameTypeMappings.put(objectName, map);
						return map;
					} catch (Exception ex) {
						throw new RuntimeException("Failed to get Attribute Types for objectName [" + objectName + "]");
					}									
				}				
			}
		}
		return map;
	}
	
	//objectNameNumericTypeMappings
	/**
	 * Returns a map of the numeric attribute name to type mappings for the MBean in the passed MBeanServer identified by the passed ObjectName
	 * @param server The MBeanServer
	 * @param objectName The MBean's ObjectName
	 * @return a map of attribute to type mappings
	 */
	public static Map<String, Class<?>> getNumericAttrTypes(MBeanServer server, ObjectName objectName) {
		Map<String, Class<?>> map =  objectNameNumericTypeMappings.get(objectName);
		if(map==null) {
			synchronized(objectNameNumericTypeMappings) {
				map =  objectNameNumericTypeMappings.get(objectName);
				if(map==null) {
					map = new HashMap<String, Class<?>>();
					try {
						MBeanInfo info = server.getMBeanInfo(objectName);			
						ClassLoader classLoader = server.getClassLoaderFor(objectName);
						for(MBeanAttributeInfo ainfo: info.getAttributes()) {
							String name = ainfo.getName();
							String type = ainfo.getType();
							Class<?> clazz = primitives.get(type);
							if(clazz==null) clazz = Class.forName(type, true, classLoader);
							if(Number.class.isAssignableFrom(clazz) || primitiveNumerics.containsKey(clazz.getName())) {
								map.put(name,  (Class<?>) clazz);
								continue;
							} 
							
							if(CompositeData.class.isAssignableFrom(clazz)) {
								CompositeData cd = (CompositeData)server.getAttribute(objectName, name);
								if(cd==null) continue;
								CompositeType ct = cd.getCompositeType();
								for(String key: ct.keySet()) {
									name = name + "/" + key;
									clazz = primitives.get(ct.getType(key).getClassName());
									if(clazz==null) clazz = Class.forName(ct.getType(key).getClassName(), true, classLoader);
									if(Number.class.isAssignableFrom(clazz) || primitiveNumerics.containsKey(clazz.getName())) {
										map.put(name,  (Class<?>) clazz);
									}
								}
							}
							
						}
						objectNameNumericTypeMappings.put(objectName, map);
						return map;
					} catch (Exception ex) {
						throw new RuntimeException("Failed to get Numeric Attribute Types for objectName [" + objectName + "]", ex);
					}									
				}				
			}
		}
		return map;
	}
	
	
}
