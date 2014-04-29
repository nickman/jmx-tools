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
package org.helios.jmx.metrics.test;

import java.lang.reflect.Method;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.helios.jmx.IntervalAccumulatorInterceptor;
import org.helios.jmx.metrics.IntervalAccumulator;
import org.helios.jmx.util.helpers.SystemClock;
import org.springframework.aop.Advisor;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;

/**
 * <p>Title: Main</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.metrics.test.Main</code></p>
 */

public class Main {

	/**
	 * Creates a new Main
	 */
	public Main() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("AOP Test");
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);
		try {
			GenericApplicationContext ctx = new GenericApplicationContext();
			XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(ctx);
			xmlReader.loadBeanDefinitions(new ClassPathResource("spring.xml"));
			ctx.refresh();
			log("Ready");
//			ProxyFactoryBean pfb = (ProxyFactoryBean)ctx.getBean("InstPojo", ProxyFactoryBean.class);
			Instrumentable inst = (Instrumentable)ctx.getBean("InstPojo", Instrumentable.class);
			log("Inst Class: [%s]", inst.getClass().getName());
			log("Interfaces:========");
			for(Class<?> clazz : inst.getClass().getInterfaces()) {
				log("\t%s", clazz.getName());
			}
			
			Advised advised = (Advised)inst;
			log("Proxied Interfaces:========");
			for(Class<?> proxied: advised.getProxiedInterfaces()) {
				log("\t%s", proxied.getName());
			}
			log("Advisors:========");
			for(Advisor advisor: advised.getAdvisors()) {
				DefaultPointcutAdvisor dpa = (DefaultPointcutAdvisor)advisor;
				log("\tPerInstance:%s, Advice:%s", dpa.isPerInstance(), dpa.getAdvice());
				Pointcut pointcut = dpa.getPointcut();
				MethodMatcher mm = pointcut.getMethodMatcher();
				IntervalAccumulatorInterceptor interceptor = (IntervalAccumulatorInterceptor)advisor.getAdvice(); 
				for(Method method: Instrumentable.class.getDeclaredMethods()) {
					log("Method [%s] Matches: %s", method.getName(), mm.matches(method, advised.getTargetClass()));
					if(mm.matches(method, advised.getTargetClass())) {
						Class<?> target = advised.getTargetClass();
						IntervalAccumulator ia = new IntervalAccumulator(target.getPackage().getName(), target.getSimpleName(), method.getName(), method.getParameterTypes());
						interceptor.addAccumulator(method, ia);
					}
				}
				
			}
			while(true) {
				for(int i = 0; i < 10; i++) {
//					inst.getNthRandom(i);
					inst.sleep();
				}
				SystemClock.sleep(2000);
			}
			//log("Done");
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			System.exit(-1);
		}

	}

	
	public static void log(String format, Object...args) {
		System.out.println(String.format(format, args));
	}
}
