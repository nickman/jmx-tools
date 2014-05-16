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
package org.helios.jmx.mbean;

import javax.management.AttributeNotFoundException;

import org.helios.jmx.annotation.ManagedMetric;
import org.helios.jmx.annotation.ManagedNotification;
import org.helios.jmx.annotation.ManagedOperation;
import org.helios.jmx.annotation.ManagedOperationParameter;

/**
 * <p>Title: PopOperationsMBean</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.mbean.PopOperationsMBean</code></p>
 */

public interface PopOperationsMBean {

	/**
	 * Explodes a {@link ManagedMetric#popable} attribute into its individual attributes and adds them as first-class attributes to this MBean.
	 * The MBean is not bounced on completion.
	 * @param name The name of the popable composite attribute
	 * @return true if the attribute was popped, false if it was not, meaning it may not have been poppable, or was already popped.
	 * @throws AttributeNotFoundException thrown if the passed name is not recognized as an attribute
	 */
	@ManagedOperation(name="popAttribute", description="Pops the named attribute", 
			notifications={ @ManagedNotification(notificationTypes={"jmx.mbean.info.changed"}, name="MBeanInfoChanged", description="Notification emitted when the MBeanInfo is updated") },
			parameters={@ManagedOperationParameter(name="AttributeName", description="The name of the attribute to pop")}
	)		
	public abstract boolean pop(String name) throws AttributeNotFoundException;

	/**
	 * Explodes a {@link ManagedMetric#popable} attribute into its individual attributes and adds them as first-class attributes to this MBean.
	 * @param name The name of the popable composite attribute
	 * @param bounce If true, the mbean will be bounced to force a meta-data refresh in the clients.
	 * @return true if the attribute was popped, false if it was not, meaning it may not have been poppable, or was already popped.
	 * @throws AttributeNotFoundException thrown if the passed name is not recognized as an attribute
	 */
	@ManagedOperation(name="popAttribute", description="Pops the named attribute", 
			notifications={ @ManagedNotification(notificationTypes={"jmx.mbean.info.changed"}, name="MBeanInfoChanged", description="Notification emitted when the MBeanInfo is updated") },
			parameters={
				@ManagedOperationParameter(name="AttributeName", description="The name of the attribute to pop"),
				@ManagedOperationParameter(name="Bounce", description="Indicates if the MBean should be bounced on completion")
			}
	)		
	public abstract boolean pop(String name, boolean bounce)
			throws AttributeNotFoundException;

	/**
	 * Unpops a popped attribute, removing the exposed sub-attributes.
	 * @param name The name of the popable composite attribute
	 * @param bounce If true, the mbean will be bounced to force a meta-data refresh in the clients.
	 * @return true if the attribute was unpopped, false if it was not, meaning it may not have been poppable, or was not already popped.
	 * @throws AttributeNotFoundException thrown if the passed name is not recognized as an attribute
	 */
	@ManagedOperation(name="unpopAttribute", description="Unpops the named attribute", 
			notifications={ @ManagedNotification(notificationTypes={"jmx.mbean.info.changed"}, name="MBeanInfoChanged", description="Notification emitted when the MBeanInfo is updated") },
			parameters={
				@ManagedOperationParameter(name="AttributeName", description="The name of the attribute to unpop"),
				@ManagedOperationParameter(name="Bounce", description="Indicates if the MBean should be bounced on completion")
			}
	)						
	public abstract boolean unpop(String name, boolean bounce)
			throws AttributeNotFoundException;

	/**
	 * Unpops a popped attribute, removing the exposed sub-attributes.
	 * The MBean is not bounced on completion.
	 * @param name The name of the popable composite attribute
	 * @return true if the attribute was unpopped, false if it was not, meaning it may not have been poppable, or was not already popped.
	 * @throws AttributeNotFoundException thrown if the passed name is not recognized as an attribute
	 */
	@ManagedOperation(name="unpopAttribute", description="Unpops the named attribute", 
			notifications={ @ManagedNotification(notificationTypes={"jmx.mbean.info.changed"}, name="MBeanInfoChanged", description="Notification emitted when the MBeanInfo is updated") },
			parameters={@ManagedOperationParameter(name="AttributeName", description="The name of the attribute to unpop")}
	)							
	public abstract boolean unpop(String name)
			throws AttributeNotFoundException;

	/**
	 * Pops all the poppable and unpopped attributes in this bean
	 * The MBean is not bounced on completion.
	 * @return the number of attributes popped
	 */
	@ManagedOperation(name="popAll", description="Pops all the poppable attributes", 
			notifications={ @ManagedNotification(notificationTypes={"jmx.mbean.info.changed"}, name="MBeanInfoChanged", description="Notification emitted when the MBeanInfo is updated") }				
	)						
	public abstract int popAll();

	/**
	 * Pops all the poppable and unpopped attributes in this bean
	 * @param bounce If true, the mbean will be bounced to force a meta-data refresh in the clients.
	 * @return the number of attributes popped
	 */
	@ManagedOperation(name="popAll", description="Pops all the poppable attributes", 
			notifications={ @ManagedNotification(notificationTypes={"jmx.mbean.info.changed"}, name="MBeanInfoChanged", description="Notification emitted when the MBeanInfo is updated") },
			parameters={@ManagedOperationParameter(name="Bounce", description="Indicates if the MBean should be bounced on completion")}
	)							
	public abstract int popAll(boolean bounce);

	/**
	 * Unpops all the popped attributes
	 * The MBean is not bounced on completion.
	 * @return the number of unpopped attributes
	 */
	@ManagedOperation(name="unPopAll", description="Unpops all the poppable attributes", 
			notifications={ @ManagedNotification(notificationTypes={"jmx.mbean.info.changed"}, name="MBeanInfoChanged", description="Notification emitted when the MBeanInfo is updated") }				
	)							
	public abstract int unPopAll();

	/**
	 * Unpops all the popped attributes
	 * @param bounce If true, the mbean will be bounced to force a meta-data refresh in the clients.
	 * @return the number of unpopped attributes
	 */
	@ManagedOperation(name="unPopAll", description="Unpops all the poppable attributes", 
			notifications={ @ManagedNotification(notificationTypes={"jmx.mbean.info.changed"}, name="MBeanInfoChanged", description="Notification emitted when the MBeanInfo is updated") },
			parameters={@ManagedOperationParameter(name="Bounce", description="Indicates if the MBean should be bounced on completion")}
	)								
	public abstract int unPopAll(boolean bounce);

	/**
	 * Unregisters, then re-registers this MBean without releasing any resources.
	 * This is primarilly intended to force some clients to re-render the MBean
	 * when the MBeanInfo changes because the clients ignore the <b><code>jmx.mbean.info.changed</code></b>
	 * notification. (Lookin' at you, JConsole, VisualVM and Java Mission Control)
	 */
	@ManagedOperation(name="Bounce", description="Unregisters, then re-registers this MBean without releasing any resources", 
			notifications={ @ManagedNotification(notificationTypes={"jmx.mbean.info.changed"}, name="MBeanInfoChanged", description="Notification emitted when the MBeanInfo is updated") }				
	)								
	public abstract void bounce();

}