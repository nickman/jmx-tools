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

import java.util.HashMap;
import java.util.Map;

import javax.management.remote.message.CloseMessage;
import javax.management.remote.message.HandshakeBeginMessage;
import javax.management.remote.message.HandshakeEndMessage;
import javax.management.remote.message.HandshakeErrorMessage;
import javax.management.remote.message.MBeanServerRequestMessage;
import javax.management.remote.message.MBeanServerResponseMessage;
import javax.management.remote.message.Message;
import javax.management.remote.message.NotificationRequestMessage;
import javax.management.remote.message.NotificationResponseMessage;
import javax.management.remote.message.SASLMessage;
import javax.management.remote.message.TLSMessage;

/**
 * <p>Title: MessageReference</p>
 * <p>Description: Reference class for all known types of {@link javax.management.remote.message.Message}.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.remoting.serialization.MessageReference</code></p>
 */

public class MessageReference {
	
	static {
		Map<Class<? extends Message>, Byte> tmp = new HashMap<Class<? extends Message>, Byte>();
		byte seed = 0;
		tmp.put(CloseMessage.class, ++seed);
		tmp.put(HandshakeBeginMessage.class, ++seed);
		tmp.put(HandshakeEndMessage.class, ++seed);
		tmp.put(HandshakeErrorMessage.class, ++seed);
		tmp.put(MBeanServerRequestMessage.class, ++seed);
		tmp.put(MBeanServerResponseMessage.class, ++seed);
		tmp.put(NotificationRequestMessage.class, ++seed);
		tmp.put(NotificationResponseMessage.class, ++seed);
		tmp.put(SASLMessage.class, ++seed);
		tmp.put(TLSMessage.class, ++seed);
	}
}
