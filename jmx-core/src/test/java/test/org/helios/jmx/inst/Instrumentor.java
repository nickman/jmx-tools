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

import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.concurrent.atomic.AtomicReference;

import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.bytecode.ConstPool;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

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

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("Instrumentor Test");
		
		Instrumentation instrumentation = LocalAgentInstaller.getInstrumentation();
		AtomicReference<byte[]> newByteCodeRef = new AtomicReference<byte[]>();
		
		ClassPool cp = new ClassPool();
		cp.appendClassPath(new LoaderClassPath(InstrumentMe.class.getClassLoader()));
		try {
			CtClass ctClazz = cp.get(EWMAWrapper.class.getName());
			byte[] ewmaByteCode = ctClazz.toBytecode();
			ctClazz = cp.get(InstrumentMe.class.getName());
			byte[] instrByteCode = ctClazz.toBytecode();
			
			ClassFileTransformer printer = printer();
			instrumentation.addTransformer(printer, true);
			Class<?> anonClass = UnsafeAdapter.defineAnonymousClass(InstrumentMe.class, ctClazz.toBytecode(), null);
			UnsafeAdapter.ensureClassInitialized(anonClass);
			instrumentation.removeTransformer(printer);
			
//			ctClazz = cp.get(anonClass.getName());
			ClassFileTransformer cft = ewmaInjector(InstrumentMe.class.getName().replace('.', '/'), ewmaByteCode, newByteCodeRef);
			
			instrumentation.addTransformer(cft, true);
			instrumentation.retransformClasses(anonClass);
			
			log("Inst Complete");
			
			
			
			log(new InstrumentMe(10).generateRandoms());
			
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
