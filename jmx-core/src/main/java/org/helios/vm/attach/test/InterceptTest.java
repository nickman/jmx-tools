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
package org.helios.vm.attach.test;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.helios.vm.attach.agent.LocalAgentInstaller;

/**
 * <p>Title: InterceptTest</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.vm.attach.test.InterceptTest</code></p>
 */

public class InterceptTest {

	/**
	 * Creates a new InterceptTest
	 */
	public InterceptTest() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TestClass tc = new TestClass("Smith");
		log("Name:%s", tc.getName());
//		log(tc.greet("Hello"));
		Instrumentation instr = LocalAgentInstaller.getInstrumentation();
		try {
			instrument(tc.getClass());
		} catch (Exception  ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	public static byte[] instrument(Class<?> clazz) {
		try {
			byte[] byteCode = null;
			ClassPool cp = new ClassPool();
			cp.appendClassPath(new LoaderClassPath(clazz.getClassLoader()));
			CtClass ctClazz = cp.get(clazz.getName());
			int cnt = 0;
			Set<CtClass> invokers = new HashSet<CtClass>();
			for(Method method: clazz.getDeclaredMethods()) {
				log("Instrumenting [%s]", method.getName());
				CtMethod ctm = ctClazz.getDeclaredMethod(method.getName(), getSignature(cp, method.getParameterTypes()));
				String invokerClazzName = clazz.getPackage().getName() + "." + clazz.getSimpleName() + cnt;
				CtClass invoker = cp.makeClass(invokerClazzName);
				CtMethod invokerMethod = CtNewMethod.copy(ctm, "invoke", invoker, null);
				CtConstructor ctor = new CtConstructor(new CtClass[] {}, invoker);
				invoker.addConstructor(ctor);
				ctor.setBody("{}");
				invoker.addMethod(invokerMethod);
				invokers.add(invoker);

				
				ctClazz.removeMethod(ctm);
				ctClazz.addField(new CtField(invoker, "invoker", ctClazz), " new " + invokerClazzName + "();");
//				ctm.addLocalVariable("invoker", invoker);
				log("Invoker: [%s]", invoker.getName());
//				ctm.insertBefore("invoker = new " + invoker.getName() + "();");
				ctm.setBody("{ return invoker.invoke($$); }");
				ctClazz.addMethod(ctm);				
//				ctm.getMethodInfo().				
				cnt++;
			}
			ctClazz.writeFile("/tmp/intercept");
			for(CtClass ct: invokers) {
				ct.writeFile("/tmp/intercept");
			}
			byteCode = ctClazz.toBytecode();
			return byteCode;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/*
	 * class Invoker {
	 * 		public <ReturnType> invoke(args) {
	 * 			<original body>
	 * 		}
	 * }
	 */
	
	public Object foo(String s, int i) {
		String result = s + i;
		return result;
	}
	
	public static LinkedList<MethodInterceptor> createStack() {
		return new LinkedList<MethodInterceptor>();
	}
	
	public abstract static class InvocationStack {
		final Method method;
		
		final LinkedList<MethodInterceptor> stack;
		
		public InvocationStack(Method method, LinkedList<MethodInterceptor> stack) {
			this.method = method;
			this.stack = stack;
		}
		
		public void enter(Object...args) {
			
		}
		
		public Object exit(Object...args) {
			return null;
		}
		
		public abstract Object invoke(Object...args);
	}
	
	public static CtClass[] getSignature(ClassPool cp, Class<?>...sig) throws NotFoundException {
		CtClass[] cts = new CtClass[sig.length];
		for(int i = 0; i < sig.length; i++) {
			Class<?> clazz = sig[i];
			if(clazz.isPrimitive()) {
				
			} else {
				cts[i] = cp.get(clazz.getName());
			}
		}
		return cts;
	}
	
	private static class MethodInterceptorImpl implements MethodInterceptor {

		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	
	private static class MethodInvocationImpl implements MethodInvocation {
		
		final Object[] args;
		final Method method;
		final Object instance;
		
		private MethodInvocationImpl(Object instance, Method method, Object...args) {
			this.instance = instance;
			this.method = method;
			this.args = args;
		}

		@Override
		public Object[] getArguments() {
			return args;
		}

		@Override
		public Object proceed() throws Throwable {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object getThis() {
			return instance;
		}

		@Override
		public AccessibleObject getStaticPart() {
			return method;
		}

		@Override
		public Method getMethod() {
			return method;
		}
		
	}
	
	public static void log(String format, Object...args) {
		System.out.println(String.format(format, args));
	}	

}
