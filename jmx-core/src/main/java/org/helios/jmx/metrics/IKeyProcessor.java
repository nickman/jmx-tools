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
package org.helios.jmx.metrics;

import org.aopalliance.intercept.MethodInvocation;

/**
 * <p>Title: IKeyProcessor</p>
 * <p>Description: Defines a class that knows how to extract the accumulator key from an intercepted method invocation</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.metrics.IKeyProcessor</code></p>
 */

public interface IKeyProcessor {
	
	/** The key returned if the method call should not be accumulated */
	public static final String NO_KEY = "no-key";
	
	/**
	 * Returns the accumulator key for the passed invocation, or {@value #NO_KEY} if the method call should not be accumulated 
	 * @param invocation The method invocation
	 * @return the accumulator key
	 */
	public String getKey(MethodInvocation invocation);
	
	/**
	 * Returns the key specificaction this processor is processing
	 * @return the key specificaction
	 */
	public String getKeyMask();
}

/*
import java.util.regex.*;

values = ["43", "Jan 4, 2011", "John Smith"] as String[]
className = "Snafu"
packageName = "org.helios"
methodName = "doThis"

p = Pattern.compile('((?:\\{(a):(\\d+?)\\})|(?:\\{p\\})|(?:\\{c\\})|(?:\\{m\\}))')
v = "foo{a:0}XX{a:2}bar -- {c}";
//v = "foo{m}"
m = p.matcher(v);
int cnt = 0;
StringBuffer b = new StringBuffer();
while(m.find()) {
    cnt++;
    type = m.group(1);
    println type
    
    //index = Integer.parseInt(m.group(1));
    //m.appendReplacement(b, values[index]);
}
m.appendTail(b);

if(cnt==0) println "NOMATCH";
else {
    println "$cnt Matches"
    println b;   
}    
*/
