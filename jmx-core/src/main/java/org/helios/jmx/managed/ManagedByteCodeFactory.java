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
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.LoaderClassPath;

import org.cliffc.high_scale_lib.NonBlockingHashMapLong;
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
	
	private final CtClass[] objectArray;
	private final CtClass invokerIface;
	private final CtClass absractInvoker;
	
	/** Maps a java primitive class to the equivalent javassist primitive class */
	public final Map<Class<?>, CtClass> primitives;
	
	/** Empty ctclass array const */
	public static final CtClass[] EMPTY_CT_CLASS_ARRAY = {};
	
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
	
	public static void main(String[] args) {
		log("Test Invoker");
		try {
			 Method m = Foo.class.getDeclaredMethod("generateRandoms");
			 getInstance().newInvoker(m, null);
			 log("Done");
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	public class Foo {
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
	
	
	}
	
	
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	/** A cache of invoker classes keyed by the long hashcode of the target method's toGenericString */
	private final NonBlockingHashMapLong<Class<? extends Invoker<?, ?>>> cachedInvokerClasses  = new NonBlockingHashMapLong<Class<? extends Invoker<?, ?>>>();
	/** Invoker instance serial number factory */
	private final AtomicLong invokerSerialFactory = new AtomicLong(0L);
	
	public Invoker<?, ?> newInvoker(Method method, Object target) {
		if(method==null) throw new IllegalArgumentException("The passed method was null");
		final long methodId = StringHelper.longHashCode(method.toGenericString());
		final int methodModifiers = method.getModifiers();
		if(Modifier.isAbstract(methodModifiers) || Modifier.isPrivate(methodModifiers)) {
			throw new IllegalStateException("Abstract or private method cannot be Invoker Wrapped [" + method.toGenericString() + "]");
		}
		final boolean isStatic = Modifier.isStatic(methodModifiers);
		Class<? extends Invoker<?, ?>> invokerClass = cachedInvokerClasses.get(methodId);
		if(invokerClass==null) {
			synchronized(cachedInvokerClasses) {
				invokerClass = cachedInvokerClasses.get(methodId);
				if(invokerClass==null) {
					try {
						final long serial = invokerSerialFactory.incrementAndGet();
						ClassPool cp = localClassPool();
						CtClass invokerCtClass = cp.makeClass(method.getName() + "Invoker" + serial, absractInvoker);
						CtClass returnType = convert(cp, method.getReturnType())[0]; 
						CtClass[] sig = convert(cp, method.getParameterTypes());
						CtMethod invokeMethod = new CtMethod(returnType, "invoke", objectArray, invokerCtClass);
						invokeMethod.setExceptionTypes(convert(cp, method.getExceptionTypes()));
						
						invokerCtClass.addMethod(invokeMethod);
						CtConstructor ctor = new CtConstructor(EMPTY_CT_CLASS_ARRAY, invokerCtClass);
						invokerCtClass.addConstructor(ctor);
						StringBuilder m = new StringBuilder("{");
						int argId = 0;
						for(Class<?> argType: method.getParameterTypes()) {
							m.append("\n\t").append(argType.getName()).append("arg").append(argId).append(" = $1[").append(argId).append("]");
							argId++;
						}
						m.append("\n\t");
						if(!method.getReturnType().equals(Void.class) && !method.getReturnType().equals(void.class)) {
							m.append("return ");
						}
						m.append("((").append(method.getDeclaringClass().getName()).append(")target).").append(method.getName()).append("(");
						
						
						argId = 0;
						for(Class<?> argType: method.getParameterTypes()) {
							m.append("arg").append(argId).append(",");
							argId++;
						}
						if(argId>0) m.deleteCharAt(m.length()-1);
						m.append(");");
						m.append("\n\t}");
						log(m.toString());
						invokeMethod.setBody(m.toString());
						invokeMethod.setModifiers(invokeMethod.getModifiers() & ~Modifier.ABSTRACT);
						invokerCtClass.setModifiers(invokerCtClass.getModifiers() & ~Modifier.ABSTRACT);
						invokerCtClass.writeFile("/tmp/a");
						invokerClass = invokerCtClass.toClass();
						cachedInvokerClasses.put(methodId, invokerClass);
					} catch (Exception x) {
						throw new RuntimeException("Failed to create new invoker class for [" + method.toGenericString() + "]", x);
					}
				}
			}
		}		
		try {			
			return invokerClass.newInstance(); 
		} catch (Exception x) {
			throw new RuntimeException("Failed to create new invoker for [" + method.toGenericString() + "]", x);
		}		
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
		
		try {
			objectArray = new CtClass[] {classPool.get(Object[].class.getName())};
			invokerIface = classPool.get(Invoker.class.getName());
			absractInvoker = classPool.get(AbstractInvoker.class.getName());
		} catch (Exception x) {
			throw new RuntimeException("Failed to initialize ManagedByteCodeFactory base pool", x);
		}
	}

}
