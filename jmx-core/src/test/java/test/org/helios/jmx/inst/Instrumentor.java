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
package test.org.helios.jmx.inst;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.concurrent.atomic.AtomicReference;

import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;

import org.helios.jmx.util.unsafe.UnsafeAdapter;
import org.helios.vm.attach.agent.LocalAgentInstaller;

/**
 * <p>Title: Instrumentor</p>
 * <p>Description: Instrumentation test</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.jmx.inst.Instrumentor</code></p>
 */

public class Instrumentor {

	/**
	 * Creates a new Instrumentor
	 */
	public Instrumentor() {
		// TODO Auto-generated constructor stub
	}
	
	public class Interceptor {
		
		
		public Interceptor() {

		}
		
		public Object proceed() {
			return "foo";
		}
		
		public Object invoke() {
			try {
				// action before
				Object r = proceed(); // add $args
				// action after
				return r;
			} catch (Throwable t) {
				// action error
				UnsafeAdapter.throwException(t);
				throw new RuntimeException();
			} 
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("Instrumentor Test");
		CtClass.debugDump = "c:/temp/a";
		
		Instrumentation instrumentation = LocalAgentInstaller.getInstrumentation();
		AtomicReference<byte[]> newByteCodeRef = new AtomicReference<byte[]>();
		
		final ClassPool cp = new ClassPool();
		cp.appendClassPath(new LoaderClassPath(InstrumentMe.class.getClassLoader()));
		try {
			
			
			
			
			
			CtClass ctClazz = cp.get(InstrumentMe.class.getName());			
			CtMethod targetMethod = ctClazz.getDeclaredMethod("generateRandoms");
			
			
			final CtClass INTERCEPTOR = cp.get(Interceptor.class.getName());			
			for(CtConstructor ctor: INTERCEPTOR.getDeclaredConstructors()) {
				INTERCEPTOR.removeConstructor(ctor);
			}
			
			CtMethod interceptedMethod = CtNewMethod.copy(targetMethod, INTERCEPTOR, null);
			INTERCEPTOR.addMethod(interceptedMethod);
			
			
			StringBuilder argCallSig = new StringBuilder("(");
			StringBuilder pSaver = new StringBuilder("{\n");
			
			int arg = 0;
			for(CtClass param: targetMethod.getParameterTypes()) {
				CtField f = new CtField(param, "param" + arg, INTERCEPTOR);
				INTERCEPTOR.addField(f);
				argCallSig.append(("param" + arg)).append(",");
				pSaver.append(("param" + arg)).append("=").append(("$" + (arg+1))).append(";\n");
			}
			
			if(argCallSig.length() > 0) argCallSig.deleteCharAt(argCallSig.length()-1);
			argCallSig.append(");");
			pSaver.append("}");
			
			log("ArgSigCall: [%s]", argCallSig);
			log("pSaver: [%s]", pSaver);
			
			CtConstructor interceptorCtor = new CtConstructor(new CtClass[0], INTERCEPTOR);
			INTERCEPTOR.addConstructor(interceptorCtor);
			for(CtClass param: targetMethod.getParameterTypes()) {
				interceptorCtor.addParameter(param);
			}			
			interceptorCtor.setBody(pSaver.toString());
			
			
			
			CtMethod proceedMethod = INTERCEPTOR.getDeclaredMethod("proceed");
			if(targetMethod.getReturnType()==CtClass.voidType) {
				proceedMethod.setBody(targetMethod.getName() + argCallSig.toString());
			} else {
				proceedMethod.setBody("return " + targetMethod.getName() + argCallSig.toString());
			}
			 
			
			
			INTERCEPTOR.setName(InstrumentMe.class.getName() + "$InterceptorX");
			
			INTERCEPTOR.writeFile(CtClass.debugDump);
			log("Interceptor Written");
			
			
			final byte[] _INTERCEPTOR = INTERCEPTOR.toBytecode();
			
			final String binaryName = ctClazz.getName().replace('.', '/');
			final String name = ctClazz.getName();
			ClassFileTransformer eet = new ClassFileTransformer() {
				@Override
				public byte[] transform(ClassLoader loader, String className,
						Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
						byte[] classfileBuffer) throws IllegalClassFormatException {
					if(className.equals(binaryName)) {
						try {
							ClassPool icp = new ClassPool();
							icp.appendClassPath(new LoaderClassPath(loader));
							icp.appendClassPath(new ByteArrayClassPath(name, classfileBuffer));
							icp.appendClassPath(new ByteArrayClassPath(name + "$Interceptor", _INTERCEPTOR));
							CtClass iceptor = cp.get(INTERCEPTOR.getName());
							for(CtConstructor ctor: iceptor.getDeclaredConstructors()) {
								log("CTOR:" + ctor + " /"  + ctor.getGenericSignature() +  " params:" + ctor.getParameterTypes().length);
							}
							CtClass ctClass = cp.get(name);
							CtMethod tMethod = ctClass.getDeclaredMethod("generateRandoms");
							
							if(tMethod.getReturnType()==CtClass.voidType) {
								tMethod.setBody("{ new " + INTERCEPTOR.getName() + "($$).invoke(); }");
							} else {
								tMethod.setBody("{ return new " + INTERCEPTOR.getName() + "($$).invoke(); }");
							}
							ctClass.writeFile(CtClass.debugDump);
							log("Interceptee [%s] Written", ctClass.getName());
							
							log("Transformed [%s]", name);
							return ctClass.toBytecode();
						} catch (Exception x) {
							x.printStackTrace(System.err);
						}
					}
					return null;
				}
			};
			instrumentation.addTransformer(eet, true);
			instrumentation.retransformClasses(InstrumentMe.class);
			instrumentation.removeTransformer(eet);

			
			ctClazz = cp.get(InstrumentMe.class.getName());
			ctClazz.writeFile(CtClass.debugDump);
			log("Interceptee [%s] Written", ctClazz.getName());

			
//			CtField argsField = new CtField(cp.get(Object[].class.getName()), "fargs", INTERCEPTOR);			
//			INTERCEPTOR.addField(argsField);
//			
//			CtConstructor interceptorCtor = new CtConstructor(targetMethod.getParameterTypes(), INTERCEPTOR);
//			INTERCEPTOR.addConstructor(interceptorCtor);
//			interceptorCtor.setBody("{fargs = $args}");
			
			
			
//			CtClass interceptor = cp.makeClass(InstrumentMe.class.getName() + "Interceptor");
//			//CtMethod interceptorMethod = CtNewMethod.copy(targetMethod, interceptor, null);
//			CtMethod interceptorMethod = CtNewMethod.wrapped(targetMethod.getReturnType(), "original", targetMethod.getParameterTypes(), null, targetMethod, null, interceptor);
////			interceptorMethod.setName("original");
//			
//			
//			interceptor.addMethod(interceptorMethod);
//			interceptor.writeFile(CtClass.debugDump);
//			log("Interceptor Written");
			
			
			ClassFileTransformer printer = printer();
			instrumentation.addTransformer(printer, true);
			Class<?> anonClass = UnsafeAdapter.defineAnonymousClass(InstrumentMe.class, ctClazz.toBytecode(), null);
			UnsafeAdapter.ensureClassInitialized(anonClass);
			instrumentation.removeTransformer(printer);
			
//			ctClazz = cp.get(anonClass.getName());
//			ClassFileTransformer cft = ewmaInjector(InstrumentMe.class.getName().replace('.', '/'), ewmaByteCode, newByteCodeRef);
//			
//			instrumentation.addTransformer(cft, true);
//			instrumentation.retransformClasses(anonClass);
//			
//			log("Inst Complete");
//			
//			
//			
//			log(new InstrumentMe(10).generateRandoms(3));
			
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}

	}
	
	public static ClassFileTransformer printer() {
		return new ClassFileTransformer() {
			public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] buff) throws IllegalClassFormatException {
				if(className==null) {
					log("++++++++++++++ %s, %s bytes", className, buff.length);
					try {
						ClassPool cp = new ClassPool();
						cp.appendSystemPath();
						cp.appendClassPath(new ByteArrayClassPath("foo", buff));
						CtClass clazz = cp.get("foo");
						clazz.writeFile("/tmp/a");
					} catch (Exception ex) {
						ex.printStackTrace(System.err);
					}
				}
				return null;
			}
		};
	}
	
	public static ClassFileTransformer ewmaInjector(final String target, final byte[] ewmaByteCode, final AtomicReference<byte[]> newByteCode) {
		final String originalClassName = target.replace('/', '.');
		return new ClassFileTransformer() {
			@Override
			public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] buff) throws IllegalClassFormatException {
//				log("Examining [%s] for match to [%s]", className, target);
				if(target.equals(className)) {
					try {
						ClassPool cp = new ClassPool();
						cp.appendSystemPath();
						cp.importPackage("test.org.helios.jmx.inst");
						cp.appendClassPath(new ByteArrayClassPath(originalClassName, buff));
						CtClass clazz = cp.get(originalClassName);
						clazz.defrost();
						
						log("Done");
						CtMethod method = clazz.getDeclaredMethod("generateRandoms");
						
//						clazz.removeMethod(method);
						//method.addCatch("test.org.helios.jmx.inst.EWMAWrapper.error();", cp.get(Throwable.class.getName()));
						method.insertBefore("test.org.helios.jmx.inst.EWMAWrapper.start();");
						method.insertAfter("test.org.helios.jmx.inst.EWMAWrapper.end();");
						
//						method.insertBefore("test.org.helios.jmx.inst.EWMAWrapper.start();");
//						method.insertAfter("test.org.helios.jmx.inst.EWMAWrapper.end();", false);
						
//						clazz.addMethod(method);
						clazz.writeFile("/tmp/a");
						return clazz.toBytecode();
					} catch (Exception x) {
						x.printStackTrace(System.err);
					}
				} else if(className==null) {
					log("++++++++++++++ %s, %s bytes", className, buff.length);
				}
				return null;
			}
		};
	}
	
	public static void log(Object fmt, Object...args) {
		System.out.println("[" + Thread.currentThread().getName() + "]" + String.format(fmt.toString(), args));
	}

}
