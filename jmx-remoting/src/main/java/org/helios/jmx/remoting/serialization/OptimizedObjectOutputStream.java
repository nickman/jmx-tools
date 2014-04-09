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
package org.helios.jmx.remoting.serialization;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;

import javax.management.remote.message.Message;

/**
 * <p>Title: OptimizedObjectOutputStream</p>
 * <p>Description: Extension of {@link ObjectOutputStream} that compresses the output stream for instances implementing {@link Message}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.remoting.serialization.OptimizedObjectOutputStream</code></p>
 */

public class OptimizedObjectOutputStream extends ObjectOutputStream {


	/**
	 * Creates a new OptimizedObjectOutputStream
	 * @param out The output stream the object will be written to
	 * @throws IOException thrown on any IO errors
	 */
	public OptimizedObjectOutputStream(OutputStream out) throws IOException {
		super(out);
	}
	
	protected void writeClassDescriptor(ObjectStreamClass desc) {
		if(!Message.class.isAssignableFrom(desc.forClass()) {
			super.writeClassDescriptor(desc);
		}
	}

}
