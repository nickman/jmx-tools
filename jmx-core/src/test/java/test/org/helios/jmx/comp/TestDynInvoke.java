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
package test.org.helios.jmx.comp;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;

import org.helios.jmx.util.helpers.StringHelper;
import org.junit.Test;

import sun.invoke.util.BytecodeDescriptor;
import test.org.helios.jmx.BaseTest;

/**
 * <p>Title: TestDynInvoke</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.jmx.comp.TestDynInvoke</code></p>
 */

public class TestDynInvoke extends BaseTest {

	@Test
	public void testDynInvoke() throws Throwable {
		Lookup lookup = MethodHandles.lookup();
		Method mp = getClass().getDeclaredMethod("prim", int.class);
		Method mo = getClass().getDeclaredMethod("obj", Integer.class);
		Method oo = getClass().getDeclaredMethod("obj", Integer.class, int.class, String.class);
		MethodHandle p = lookup.unreflect(mp);
		MethodHandle o = lookup.unreflect(mo);
		p.bindTo(this).invoke(5);
		o.bindTo(this).invoke(new Integer(10));
		p.bindTo(this).invokeExact(5);
		o.bindTo(this).invokeExact(new Integer(10));
		o.bindTo(this).invoke(5);
		p.bindTo(this).invoke(new Integer(10));
		
		log("Primitive:[%s] [%s]", p.type().toMethodDescriptorString(), StringHelper.getMethodDescriptor(mp));
		log("Object:[%s] [%s]", o.type().toMethodDescriptorString(), StringHelper.getMethodDescriptor(mo));
		log("Object2:[%s]", StringHelper.getMethodDescriptor(oo));
		
		log("pSig:[%s]", BytecodeDescriptor.unparse(mp.getParameterTypes()[0]));
		
		
		
//		o.bindTo(this).invokeExact(5);
//		p.bindTo(this).invokeExact(new Integer(10));
		
	}
	
	public void prim(int x) {
		log("Int is:" + x);
	}
	
	public void obj(Integer x) {
		log("Int is:" + x);
	}
	
	public void obj(Integer x, int z, String w) {
		
	}
	

}
