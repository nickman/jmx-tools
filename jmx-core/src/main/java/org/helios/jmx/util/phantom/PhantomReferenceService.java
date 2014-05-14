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
package org.helios.jmx.util.phantom;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

/**
 * <p>Title: PhantomReferenceService</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.util.phantom.PhantomReferenceService</code></p>
 */

public class PhantomReferenceService implements Runnable {
	/** The singleton instance */
	private static volatile PhantomReferenceService instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The ref queue cleaner thread */
	private final Thread refQueueThread;
	/** The queue where enqueued references go to die */
	private final ReferenceQueue<Object> refQueue = new ReferenceQueue<Object>(); 
	
	/**
	 * Acquires the singleton PhantomReferenceService instance
	 * @return the singleton PhantomReferenceService instance
	 */
	public static PhantomReferenceService getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new PhantomReferenceService();
				}
			}
		}
		return instance;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		while(true) {
			try {
				PhantomReferenceWrapper phantomRef =   (PhantomReferenceWrapper) refQueue.remove();
				if(phantomRef!=null) {
					if(phantomRef.runOnClear!=null) {
						phantomRef.runOnClear.run();
					}
				}
			} catch (Exception ex) {
				if(Thread.interrupted()) Thread.interrupted();
			}
		}
	}
	
	/**
	 * Creates a new phantom reference
	 * @param referent The referent which when enqueued will trigger the passed task
	 * @param onEnqueueTask the task to run when the referent becomes phantom reachable
	 * @return the reference
	 */
	@SuppressWarnings("unchecked")
	public <T> PhantomReference<T> newPhantomReference(T referent, Runnable onEnqueueTask) {
		return (PhantomReference<T>) new PhantomReferenceWrapper(referent, onEnqueueTask);
	}
	
	/**
	 * Creates a new PhantomReferenceService
	 */
	private PhantomReferenceService() {
		refQueueThread = new Thread(this, getClass().getSimpleName() + "RefQueueThread");
		refQueueThread.setDaemon(true);
		refQueueThread.start();
	}

    private class PhantomReferenceWrapper extends PhantomReference<Object> {
    	private final Runnable runOnClear;
    	
		/**
		 * Creates a new PhantomReferenceWrapper
		 * @param referent The phantom referent
		 * @param onEnqueueTask a task to fire when the reference is cleared
		 */
		public PhantomReferenceWrapper(final Object referent, final Runnable onEnqueueTask) {
			super(referent, refQueue);
			runOnClear = onEnqueueTask;
		}    	
    }

}
