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
package org.helios.jmx.mbean;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.helios.jmx.concurrency.JMXManagedThreadPool;
import org.helios.jmx.util.helpers.JMXHelper;
import org.helios.jmx.util.helpers.SystemClock;

import com.sun.jmx.remote.util.ClassLogger;

/**
 * <p>Title: NotificationBroadcastService</p>
 * <p>Description: A centralized factory for creating thread pool backed notification broadcasters.
 * I've done this in part to share one common thread pool, but also because the standard NotificationBroadcasterSupport
 * hides too many useful details and it's not complicated enought to warrant reflecting them out.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.mbean.NotificationBroadcastService</code></p>
 */

public class NotificationBroadcastService {
	/** The singleton instance */
	private static volatile NotificationBroadcastService instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** A thread pool to execute async notification broadcast */
	private final JMXManagedThreadPool threadPool;
	
	
	/** The ref service JMX ObjectName */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName(NotificationBroadcastService.class);
	/** The ref service's thread pool JMX ObjectName */
	public static final ObjectName THREAD_POOL_OBJECT_NAME = JMXHelper.objectName(new StringBuilder(OBJECT_NAME.toString()).append("ThreadPool"));

    /**  */
    
	private static final Logger logger = LogManager.getLogManager().getLogger(NotificationBroadcastService.class.getName()); 
			

	
	/**
	 * Acquires the singleton ReferenceService instance
	 * @return the singleton ReferenceService instance
	 */
	public static NotificationBroadcastService getInstance() {		
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new NotificationBroadcastService();
//					JMXHelper.registerMBean(instance, OBJECT_NAME);
					JMXHelper.registerMBean(instance.threadPool, THREAD_POOL_OBJECT_NAME);							
				}
			}
		}
		return instance;
	}

	/**
	 * Creates a new NotificationBroadcastService
	 */
	private NotificationBroadcastService() {
		threadPool = new JMXManagedThreadPool(THREAD_POOL_OBJECT_NAME, "NotificationBroadcastService", 2, 10, 5000, 60000, 100, 99, false);
		threadPool.setRejectedExecutionHandler(new JMXManagedThreadPool.CallerRunsPolicy());					
	}
	
	/**
	 * Creates and returns a new NotificationEmitterSupport
	 * @param info The MBeanInfo to extract the notification infos from
	 * @return a new NotificationEmitterSupport
	 */
	public NotificationEmitterSupport newNotificationEmitter(MBeanInfo info) {
		if(info==null) throw new IllegalArgumentException("The passed MBeanInfo was null");
		return new NotificationEmitterSupport(info);
	}
	
	/**
	 * Creates and returns a new NotificationEmitterSupport
	 * @param infos The MBeanNotificationInfos initially supported by this emitter
	 * @return a new NotificationEmitterSupport
	 */
	public NotificationEmitterSupport newNotificationEmitter(MBeanNotificationInfo... infos) {		
		return new NotificationEmitterSupport(infos);
	}
	
	
	/**
	 * <p>Title: NotificationEmitterSupport</p>
	 * <p>Description: A {@link NotificationEmitter} implementation. Mostly copied from <b><code>javax.management.NotificationBroadcasterSupport</code></b></p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.jmx.mbean.NotificationEmitterSupport</code></p>
	 */
	class NotificationEmitterSupport implements NotificationEmitter {
		
		/** The registered listeners */
		private final List<ListenerInfo> listenerList = new CopyOnWriteArrayList<ListenerInfo>();
		
		/** The mutable notification info */
		private final List<MBeanNotificationInfo> notifInfo = new CopyOnWriteArrayList<MBeanNotificationInfo>();
		
		/** The notification sequence number provider */
		private final AtomicLong sequence = new AtomicLong(0L);
		
		/**
		 * Creates a new NotificationEmitterSupport
		 * @param info The MBeanInfo to extract the MBeanNotificationInfos initially supported by this emitter
		 */
		NotificationEmitterSupport(MBeanInfo info) {
			this(info.getNotifications());
		}

		
		/**
		 * Creates a new NotificationEmitterSupport
		 * @param infos The MBeanNotificationInfos initially supported by this emitter
		 */
		NotificationEmitterSupport(MBeanNotificationInfo...infos) {
			if(infos!=null && infos.length!=0) {
				for(MBeanNotificationInfo info: infos) {
					if(info!=null) {
						notifInfo.add(info);
					}
				}
			}
		}
		
		// ===============================================================================================
		//		New Stuff
		// ===============================================================================================
		
		/**
		 * Builds a new notification and sends it asynchronously
		 * @param type The notification type
		 * @param source The source of the notification
		 * @param message The notification message
		 * @param userData The optional user data
		 * @return The sent notification
		 */
		public Notification sendNotificationAsync(String type, Object source, String message, Object userData) {
			return sendNotification(true, type, source, message, userData);
		}
		
		/**
		 * Updates an existing notification with a new sequence number and timestamp and sends it asynchronously
		 * @param notif The notification to resend
		 * @return The sent notification
		 */
		public Notification resendNotifcationAsync(Notification notif) {
			if(notif!=null) {
				notif.setSequenceNumber(sequence.incrementAndGet());
				notif.setTimeStamp(SystemClock.time());
			}
			sendNotification(notif, true);
			return notif;
		}
		
		/**
		 * Builds a new notification and sends it synchronously
		 * @param type The notification type
		 * @param source The source of the notification
		 * @param message The notification message
		 * @param userData The optional user data
		 * @return The sent notification
		 */
		public Notification sendNotificationSync(String type, Object source, String message, Object userData) {
			return sendNotification(false, type, source, message, userData);
		}
		
		/**
		 * Updates an existing notification with a new sequence number and timestamp and sends it synchronously
		 * @param notif The notification to resend
		 * @return The sent notification
		 */
		public Notification resendNotifcationSync(Notification notif) {
			if(notif!=null) {
				notif.setSequenceNumber(sequence.incrementAndGet());
				notif.setTimeStamp(SystemClock.time());
			}
			sendNotification(notif, false);
			return notif;
		}
		
		
		/**
		 * Builds a new notification and sends it 
		 * @param async if true, notification is sent asynchronously, otherwise it is sent synchronously 
		 * @param type The notification type
		 * @param source The source of the notification
		 * @param message The notification message
		 * @param userData The optional user data
		 * @return The sent notification
		 */
		public Notification sendNotification(boolean async, String type, Object source, String message, Object userData) {
			Notification notif = new Notification(type, source, sequence.incrementAndGet(), SystemClock.time(), message);
			if(userData!=null) notif.setUserData(userData);
			sendNotification(notif, async);
			return notif;
		}
		
		/**
		 * Removes all registered listeners
		 */
		public void clearAllListeners() {
			listenerList.clear();
		}
		
		/**
		 * Returns the number of registered listeners
		 * @return the number of registered listeners
		 */
		public int getListenerCount() {
			return listenerList.size();
		}
		
		/**
		 * Indicates if there are any registered listeners
		 * @return true if there are any registered listeners, false otherwise
		 */
		public boolean hasListeners() {
			return !listenerList.isEmpty();
		}
		
		/**
		 * The base notification sender
		 * @param notification the notification to send
		 * @param async true to send asynchronously, false to send synchronously
		 */
		protected void sendNotification(Notification notification, boolean async) {
	        if (notification == null) {
	            return;
	        }
	        boolean enabled;
	        for (ListenerInfo li : listenerList) {
	            try {
	                enabled = li.filter == null ||
	                    li.filter.isNotificationEnabled(notification);
	            } catch (Exception e) {
//	                if (logger.debugOn()) {
//	                    logger.debug("sendNotification", e);
//	                }
	                continue;
	            }
	            
	            if (enabled) {
	            	if(async) {
	            		threadPool.execute(new SendNotifJob(notification, li));
	            	} else {
	            		li.listener.handleNotification(notification, li.handback);
	            	}
	            }
	        }
			
		}
		
	    /**
	     * Sends a notification synchronously
	     * @param notification The notification to send.
	     */
	    public void sendNotificationSynchronously(Notification notification) {
	    	sendNotification(notification, false);
	    }
		
		
		// ===============================================================================================
		
		/**
		 * {@inheritDoc}
		 * @see javax.management.NotificationBroadcaster#addNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
		 */
		@Override
		public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws IllegalArgumentException {
	        if (listener == null) {
	            throw new IllegalArgumentException ("Listener can't be null") ;
	        }
	        listenerList.add(new ListenerInfo(listener, filter, handback));
		}

		/**
		 * {@inheritDoc}
		 * @see javax.management.NotificationBroadcaster#removeNotificationListener(javax.management.NotificationListener)
		 */
		@Override
		public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
	        ListenerInfo wildcard = new WildcardListenerInfo(listener);
	        boolean removed =
	            listenerList.removeAll(Collections.singleton(wildcard));
	        if (!removed)
	            throw new ListenerNotFoundException("Listener not registered");
			
		}
		
		/**
		 * {@inheritDoc}
		 * @see javax.management.NotificationEmitter#removeNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
		 */
		@Override
		public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException {
	        ListenerInfo li = new ListenerInfo(listener, filter, handback);
	        boolean removed = listenerList.remove(li);
	        if (!removed) {
	            throw new ListenerNotFoundException("Listener not registered " +
	                                                "(with this filter and " +
	                                                "handback)");
	            // or perhaps not registered at all
	        }			
		}
		

		/**
		 * {@inheritDoc}
		 * @see javax.management.NotificationBroadcaster#getNotificationInfo()
		 */
		@Override
		public MBeanNotificationInfo[] getNotificationInfo() {
			return notifInfo.toArray(new MBeanNotificationInfo[0]);
		}

		
	    /**
	     * Sends a notification asynchronously
	     * @param notification The notification to send.
	     */
	    public void sendNotification(Notification notification) {
	    	sendNotification(notification, true);
	    }
		
		
	    /**
	     * <p>This method is called by {@link #sendNotification
	     * sendNotification} for each listener in order to send the
	     * notification to that listener.  It can be overridden in
	     * subclasses to change the behavior of notification delivery,
	     * for instance to deliver the notification in a separate
	     * thread.</p>
	     *
	     * <p>The default implementation of this method is equivalent to
	     * <pre>
	     * listener.handleNotification(notif, handback);
	     * </pre>
	     *
	     * @param listener the listener to which the notification is being
	     * delivered.
	     * @param notif the notification being delivered to the listener.
	     * @param handback the handback object that was supplied when the
	     * listener was added.
	     *
	     */
	    protected void handleNotification(NotificationListener listener, Notification notif, Object handback) {
	        listener.handleNotification(notif, handback);
	    }
		
	    /**
	     * <p>Title: SendNotifJob</p>
	     * <p>Description: A runnable task for sending async notifications. Copied from <b><code>javax.management.NotificationBroadcasterSupport</code></b></p> 
	     * <p>Company: Helios Development Group LLC</p>
	     * @author Whitehead (nwhitehead AT heliosdev DOT org)
	     * <p><code>org.helios.jmx.mbean.NotificationBroadcasterService.NotificationEmitterSupport.SendNotifJob</code></p>
	     */
	    private class SendNotifJob implements Runnable {
	        public SendNotifJob(Notification notif, ListenerInfo listenerInfo) {
	            this.notif = notif;
	            this.listenerInfo = listenerInfo;
	        }

	        public void run() {
	            try {
	                handleNotification(listenerInfo.listener,
	                                   notif, listenerInfo.handback);
	            } catch (Exception e) {
//	                if (logger.debugOn()) {
//	                    logger.debug("SendNotifJob-run", e);
//	                }
	            }
	        }

	        private final Notification notif;
	        private final ListenerInfo listenerInfo;
	    }

		
	    /**
	     * <p>Title: ListenerInfo</p>
	     * <p>Description: Wraps a registered notification listener. Copied from <b><code>javax.management.NotificationBroadcasterSupport</code></b></p> 
	     * <p>Company: Helios Development Group LLC</p>
	     * @author Whitehead (nwhitehead AT heliosdev DOT org)
	     * <p><code>org.helios.jmx.mbean.NotificationBroadcasterService.NotificationEmitterSupport.ListenerInfo</code></p>
	     */
	    private class ListenerInfo {
	        NotificationListener listener;
	        NotificationFilter filter;
	        Object handback;

	        ListenerInfo(NotificationListener listener,
	                     NotificationFilter filter,
	                     Object handback) {
	            this.listener = listener;
	            this.filter = filter;
	            this.handback = handback;
	        }

	        public boolean equals(Object o) {
	            if (!(o instanceof ListenerInfo))
	                return false;
	            ListenerInfo li = (ListenerInfo) o;
	            if (li instanceof WildcardListenerInfo)
	                return (li.listener == listener);
	            else
	                return (li.listener == listener && li.filter == filter
	                        && li.handback == handback);
	        }
	    }

	    /**
	     * <p>Title: WildcardListenerInfo</p>
	     * <p>Description: Wraps a registered wildcard notification listener. Copied from <b><code>javax.management.NotificationBroadcasterSupport</code></b></p> 
	     * <p>Company: Helios Development Group LLC</p>
	     * @author Whitehead (nwhitehead AT heliosdev DOT org)
	     * <p><code>org.helios.jmx.mbean.NotificationBroadcasterService.NotificationEmitterSupport.WildcardListenerInfo</code></p>
	     */
	    private class WildcardListenerInfo extends ListenerInfo {
	        WildcardListenerInfo(NotificationListener listener) {
	            super(listener, null, null);
	        }

	        public boolean equals(Object o) {
	            assert (!(o instanceof WildcardListenerInfo));
	            return o.equals(this);
	        }
	    }

		
	}

}
