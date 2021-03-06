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
package org.helios.jmx.managed;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.BooleanMemberValue;

import org.cliffc.high_scale_lib.NonBlockingHashMapLong;
import org.helios.jmx.annotation.ManagedMetric;
import org.helios.jmx.annotation.ManagedResource;
import org.helios.jmx.annotation.Popable;
import org.helios.jmx.annotation.Reflector;
import org.helios.jmx.util.helpers.StringHelper;
import org.helios.jmx.util.helpers.SystemClock;
import org.helios.jmx.util.helpers.SystemClock.ElapsedTime;

/**
 * <p>Title: ManagedByteCodeFactory</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.managed.ManagedByteCodeFactory</code></p>
 */

public class ManagedByteCodeFactory {
	/** The singleton instance */
	private static volatile ManagedByteCodeFactory instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The javassist classpool */
	private final ClassPool classPool = new ClassPool();
	
	private final CtClass objectCt;
	private final CtClass objectArray;
	private final CtClass invokerIface;
	private final CtClass absractInvoker;
	
	
	/** Maps a java primitive class to the equivalent javassist primitive class */
	public final Map<Class<?>, CtClass> primitives;
	/** Maps a java primitive class to the equivalent boxed class */
	public final Map<Class<?>, Class<?>> boxed;
	/** Maps a java primitive class to the appropriate unboxing expression */
	public final Map<Class<?>, String> unboxed;
	
	
	/** Empty ctclass array const */
	public static final CtClass[] EMPTY_CT_CLASS_ARRAY = {};

	/** A cache of invoker classes keyed by the long hashcode of the target method's toGenericString */
	private final NonBlockingHashMapLong<Constructor<? extends Invoker>> cachedInvokerClasses  = new NonBlockingHashMapLong<Constructor<? extends Invoker>>();
	/** Invoker instance serial number factory */
	private final AtomicLong invokerSerialFactory = new AtomicLong(0L);
	
	/**
	 * Acquires the singleton ManagedByteCodeFactory instance
	 * @return the singleton ManagedByteCodeFactory instance
	 */
	public static ManagedByteCodeFactory getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new ManagedByteCodeFactory();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a local classpool
	 * @return a local classpool
	 */
	protected ClassPool localClassPool() {
		return new ClassPool(classPool);
	}
	
	
	public static class Foo {
		Random random = new Random(System.currentTimeMillis());
		int maxRange = 10;
		public String generateRandoms() {
			int loops = Math.abs(random.nextInt(maxRange));
			ElapsedTime et = SystemClock.startClock();
			for(int i = 0; i < loops; i++) {
				String s = UUID.randomUUID().toString();
			}
			return et.printAvg("UUID rate", loops);
		}
		public String generateRandoms(int range) {
			int loops = Math.abs(random.nextInt(range));
			ElapsedTime et = SystemClock.startClock();
			for(int i = 0; i < loops; i++) {
				String s = UUID.randomUUID().toString();
			}
			return et.printAvg("UUID rate", loops);
		}
		
		public static int getPID() {
			return Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
		}
		
		public void generateRandomsVoid() {
			String s = generateRandoms();
			log("generateRandomsVoid: [" + s + "]");
		}
	
		public static void generateRandomsVoidStatic() {
			Foo foo = new Foo();
			String s = foo.generateRandoms();
			log("generateRandomsVoidStatic: [" + s + "]");
		}
	
	}
	
	
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	
	/**
	 * Returns a dynamically generated unbound invoker for the passed method
	 * @param method The method to create the invoker for 
	 * @return the invoker instance
	 */
	public Invoker newInvoker(Method method) {
		return newInvoker(method, null);
	}
	
	
	/**
	 * Returns a dynamically generated invoker for the passed method, optionally bound to the passed optional target
	 * @param method The method to create the invoker for 
	 * @param target The optional invocation target
	 * @return the invoker instance
	 */
	public Invoker newInvoker(Method method, Object target) {
		if(method==null) throw new IllegalArgumentException("The passed method was null");
		final long methodId = StringHelper.longHashCode(method.toGenericString());
		final int methodModifiers = method.getModifiers();
		Class<?> annotatedClass = Reflector.getAnnotated(method.getDeclaringClass(), ManagedResource.class);
		Method annotatedMethod = Reflector.getTargetMethodMatching(annotatedClass, method);
		final boolean popable;
		final String logicalName = Reflector.getLogicalName(annotatedMethod);
		ManagedMetric mm = annotatedMethod.getAnnotation(ManagedMetric.class);
		if(mm==null) {
			popable = false;
		} else {
			popable = mm.popable();
		}
		if(Modifier.isAbstract(methodModifiers) || Modifier.isPrivate(methodModifiers)) {
			throw new IllegalStateException("Abstract or private method cannot be Invoker Wrapped [" + method.toGenericString() + "]");
		}
		final boolean isStatic = Modifier.isStatic(methodModifiers);
		Constructor<? extends Invoker> invokerCtor = cachedInvokerClasses.get(methodId);
		if(invokerCtor==null) {
			synchronized(cachedInvokerClasses) {
				invokerCtor = cachedInvokerClasses.get(methodId);
				if(invokerCtor==null) {
					try {
						final long serial = invokerSerialFactory.incrementAndGet();
						ClassPool cp = localClassPool();
						CtClass invokerCtClass = cp.makeClass(method.getDeclaringClass().getPackage().getName() + "." + method.getName() + "Invoker" + serial, absractInvoker);
						CtClass returnType = convert(cp, method.getReturnType())[0]; 
						CtClass[] sig = convert(cp, method.getParameterTypes());
						CtMethod invokeMethod = new CtMethod(objectCt, "invoke", new CtClass[]{objectArray}, invokerCtClass);
						invokeMethod.setExceptionTypes(convert(cp, method.getExceptionTypes()));
						
						invokerCtClass.addMethod(invokeMethod);
//						CtConstructor ctor = new CtConstructor(new CtClass[]{cp.get(String.class.getName())}, invokerCtClass);
//						invokerCtClass.addConstructor(ctor);
//						ctor.setBody("{}");
						StringBuilder m = new StringBuilder("{");
//						m.append("\n\tSystem.out.println(\"ARGS:\" + java.util.Arrays.toString($1));");
						int argId = 0;
						for(Class<?> argType: method.getParameterTypes()) {
							String argName = "arg" + argId;
							if(argType.isPrimitive()) {
								m.append("\n\t").append(argType.getName()).append(" ").append(argName).append(" = ").append(cast(argType, argId)).append(";");
							} else {
								m.append("\n\t").append(argType.getName()).append(" ").append(argName).append(" = ").append(cast(argType, argId)).append("$1[").append(argId).append("];");
							}
							
							argId++;
						}
						m.append("\n\t");
						Class<?> RET_TYPE = method.getReturnType(); 
						if(!RET_TYPE.equals(Void.class) && !RET_TYPE.equals(void.class)) {
							m.append("return ($w)");
//							if(RET_TYPE.isPrimitive()) {
////								m.append("((").append(boxed.get(RET_TYPE).getName()).append(") ");
//								m.append(" ($w)");
//							}
						}
						
						if(!isStatic) {
							m.append("((").append(method.getDeclaringClass().getName()).append(")target).").append(method.getName()).append("(");
						} else {
							m.append(method.getDeclaringClass().getName()).append(".").append(method.getName()).append("(");
						}
						
//						if(RET_TYPE.isPrimitive()) {
//							m.append(")");
//						}
						
						
						argId = 0;
						for(Class<?> argType: method.getParameterTypes()) {
							m.append("arg").append(argId).append(",");
							argId++;
						}
						if(argId>0) m.deleteCharAt(m.length()-1);
						m.append(");");
						if(RET_TYPE.equals(Void.class) || RET_TYPE.equals(void.class)) {
							m.append("\n\treturn null;");
						}
						
						m.append("\n\t}");						
//						log(m.toString());
						invokeMethod.setBody(m.toString());
						invokeMethod.setModifiers(invokeMethod.getModifiers() & ~javassist.Modifier.ABSTRACT);
						
						invokeMethod.setModifiers(invokeMethod.getModifiers() | javassist.Modifier.VARARGS);
						invokerCtClass.setModifiers(invokerCtClass.getModifiers() & ~Modifier.ABSTRACT);
//						invokerCtClass.writeFile("C:/temp/a");
						ClassFile cf = invokerCtClass.getClassFile();
						ConstPool constPool = cf.getConstPool();
						AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
						javassist.bytecode.annotation.Annotation a = new javassist.bytecode.annotation.Annotation(Popable.class.getName(), constPool);
						a.addMemberValue("value", new BooleanMemberValue(popable, constPool));
						attr.setAnnotation(a);
						cf.addAttribute(attr);
						cf.setVersionToJava5(); 
						invokerCtor = invokerCtClass.toClass().getDeclaredConstructor(String.class);
						cachedInvokerClasses.put(methodId, invokerCtor);
					} catch (Exception x) {
						throw new RuntimeException("Failed to create new invoker class for [" + method.toGenericString() + "]", x);
					}
				}
			}
		}		
		try {			
			Invoker invoker = invokerCtor.newInstance(logicalName);
			if(target != null) invoker.bindTo(target);
			return invoker; 
		} catch (Exception x) {
			throw new RuntimeException("Failed to create new invoker for [" + method.toGenericString() + "]", x);
		}		
	}
	
	protected String cast(Class<?> argType, int argId) {
		if(argType.isPrimitive()) {
			return String.format(unboxed.get(argType), argId);
		} 
		return String.format("(%s)", argType.getName());
	}
	
	/**
	 * Converts the passed java class signature to a javassist ct class signature
	 * @param cp The classpool to use
	 * @param signature The java class signature
	 * @return the javassist ct class signature
	 */
	protected CtClass[] convert(ClassPool cp, Class<?> ...signature) {
		if(signature==null || signature.length==0) return EMPTY_CT_CLASS_ARRAY;
		CtClass[] ctSignature = new CtClass[signature.length];
		for(int i = 0; i < signature.length; i++) {
			if(signature[i].isPrimitive()) {
				ctSignature[i] = primitives.get(signature[i]);
			} else {
				try {
					ctSignature[i] = cp.get(signature[i].getName());
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}			
		}
		return ctSignature;
	}
	
	/**
	 * Creates a new ManagedByteCodeFactory
	 */
	private ManagedByteCodeFactory() {
		
		if(System.getProperties().containsKey("org.helios.bytecode.dir")) {
			CtClass.debugDump=System.getProperty("org.helios.bytecode.dir");
		}
		
		classPool.appendSystemPath();
		classPool.appendClassPath(new LoaderClassPath(Invoker.class.getClassLoader()));
		Map<Class<?>, CtClass> tmp = new HashMap<Class<?>, CtClass>(9);
		tmp.put(boolean.class, CtClass.booleanType);
		tmp.put(char.class, CtClass.charType);
		tmp.put(byte.class, CtClass.byteType);
		tmp.put(short.class, CtClass.shortType);
		tmp.put(int.class, CtClass.intType);
		tmp.put(long.class, CtClass.longType);
		tmp.put(float.class, CtClass.floatType);
		tmp.put(double.class, CtClass.doubleType);
		tmp.put(void.class, CtClass.voidType);
		primitives = Collections.unmodifiableMap(tmp);
		Map<Class<?>, Class<?>> btmp = new HashMap<Class<?>, Class<?>>(9);
		btmp.put(boolean.class, Boolean.class);
		btmp.put(char.class, Character.class);
		btmp.put(byte.class, Byte.class);
		btmp.put(short.class, Short.class);
		btmp.put(int.class, Integer.class);
		btmp.put(long.class, Long.class);
		btmp.put(float.class, Float.class);
		btmp.put(double.class, Double.class);
		btmp.put(void.class, Void.class);
		boxed = Collections.unmodifiableMap(btmp);
		
		
		Map<Class<?>, String> utmp = new HashMap<Class<?>, String>(8);
		utmp.put(boolean.class, "((Boolean)$1[%s]).booleanValue()");
		utmp.put(char.class, "((Character)$1[%s]).charValue()");
		utmp.put(byte.class, "((Byte)$1[%s]).byteValue()");
		utmp.put(short.class, "((Short)$1[%s]).shortValue()");
		utmp.put(int.class, "((Integer)$1[%s]).intValue()");
		utmp.put(long.class, "((Long)$1[%s]).longValue()");
		utmp.put(float.class, "((Float)$1[%s]).floatValue()");
		utmp.put(double.class, "((Double)$1[%s]).doubleValue()");
		unboxed = Collections.unmodifiableMap(utmp);
		
		
		try {
			objectArray = classPool.get(Object[].class.getName());
			invokerIface = classPool.get(Invoker.class.getName());
			absractInvoker = classPool.get(AbstractInvoker.class.getName());
			objectCt = classPool.get(Object.class.getName());
		} catch (Exception x) {
			throw new RuntimeException("Failed to initialize ManagedByteCodeFactory base pool", x);
		}
	}

}
