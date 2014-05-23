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
package org.helios.jmx.remote.protocol.local;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorProvider;
import javax.management.remote.JMXServiceURL;


/**
 * <p>Title: ClientProvider</p>
 * <p>Description: JMX client provider for connecting to the local MBeanServer using a JMXServiceURL</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.remote.protocol.local.ClientProvider</code></p>
 */
public class ClientProvider implements JMXConnectorProvider {
	/** The protocol name */
	public static final String PROTOCOL_NAME = "local";

    /**
     * {@inheritDoc}
     * @see javax.management.remote.JMXConnectorProvider#newJMXConnector(javax.management.remote.JMXServiceURL, java.util.Map)
     */
    public JMXConnector newJMXConnector(JMXServiceURL serviceURL, Map environment) throws IOException {
		if (!serviceURL.getProtocol().equals(PROTOCOL_NAME)) {
			throw new MalformedURLException("Protocol not [" + PROTOCOL_NAME + "]: " +
						    serviceURL.getProtocol());
		}
		LocalJMXConnector connector = new LocalJMXConnector();
		connector.localURL = serviceURL;
		return connector;
    }

}
