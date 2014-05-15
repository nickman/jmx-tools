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
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>Title: ReferenceService</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.util.phantom.ReferenceService</code></p>
 */

public class ReferenceService implements Runnable {
	/** The singleton instance */
	private static volatile ReferenceService instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The ref queue cleaner thread */
	private final Thread refQueueThread;
	/** A thread pool to run the ref cleaner runnables */
	final ExecutorService threadPool = Executors.newFixedThreadPool(4, new ThreadFactory() {
		final AtomicInteger serial = new AtomicInteger(0);
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "ReferenceServiceThread#" + serial.incrementAndGet());
			t.setDaemon(true);
			return t;
		}
	});
	/** The queue where enqueued references go to die */
	private final ReferenceQueue<Object> refQueue = new ReferenceQueue<Object>(); 
	
	/**
	 * Acquires the singleton ReferenceService instance
	 * @return the singleton ReferenceService instance
	 */
	public static ReferenceService getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new ReferenceService();
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
				Object removed = refQueue.remove();
				if(removed!=null && (removed instanceof ReferenceRunnable)) {
					ReferenceRunnable rr = (ReferenceRunnable)removed;
					if(rr.getClearedRunnable()!=null) {
						threadPool.submit(rr);
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
	 * Creates a new weak reference
	 * @param referent The referent which when enqueued will trigger the passed task
	 * @param onEnqueueTask the task to run when the referent becomes weakly reachable
	 * @return the reference
	 */
	@SuppressWarnings("unchecked")
	public <T> WeakReference<T> newWeakReference(T referent, Runnable onEnqueueTask) {
		return (WeakReference<T>) new WeakReferenceWrapper(referent, onEnqueueTask);
	}
	
	/**
	 * Creates a new soft reference
	 * @param referent The referent which when enqueued will trigger the passed task
	 * @param onEnqueueTask the task to run when the referent becomes softly reachable
	 * @return the reference
	 */
	@SuppressWarnings("unchecked")
	public <T> SoftReference<T> newSoftReference(T referent, Runnable onEnqueueTask) {
		return (SoftReference<T>) new SoftReferenceWrapper(referent, onEnqueueTask);
	}
	
	
	
	/**
	 * Creates a new ReferenceService
	 */
	private ReferenceService() {
		refQueueThread = new Thread(this, getClass().getSimpleName() + "RefQueueThread");
		refQueueThread.setDaemon(true);
		refQueueThread.start();
	}
	
	public interface ReferenceRunnable extends Runnable {
		public Runnable getClearedRunnable();
	}

	
	
    private class PhantomReferenceWrapper extends PhantomReference<Object> implements ReferenceRunnable {
    	private final Runnable runOnClear;
    	public Runnable getClearedRunnable() {
    		return runOnClear;
    	}
    	public void run() {
    		if(runOnClear!=null) {
    			runOnClear.run();
    		}
    	}
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
    
    private class WeakReferenceWrapper extends WeakReference<Object> implements ReferenceRunnable {
    	private final Runnable runOnClear;
    	public Runnable getClearedRunnable() {
    		return runOnClear;
    	}
    	public void run() {
    		if(runOnClear!=null) {
    			runOnClear.run();
    		}
    	}
    	/**
		 * Creates a new WeakReferenceWrapper
		 * @param referent The weak referent
		 * @param onEnqueueTask a task to fire when the reference is cleared
		 */
		public WeakReferenceWrapper(final Object referent, final Runnable onEnqueueTask) {
			super(referent, refQueue);
			runOnClear = onEnqueueTask;
		}    	
    }
    
    private class SoftReferenceWrapper extends SoftReference<Object> implements ReferenceRunnable {
    	private final Runnable runOnClear;
    	public Runnable getClearedRunnable() {
    		return runOnClear;
    	}
    	public void run() {
    		if(runOnClear!=null) {
    			runOnClear.run();
    		}
    	}
    	/**
		 * Creates a new SoftReferenceWrapper
		 * @param referent The soft referent
		 * @param onEnqueueTask a task to fire when the reference is cleared
		 */
		public SoftReferenceWrapper(final Object referent, final Runnable onEnqueueTask) {
			super(referent, refQueue);
			runOnClear = onEnqueueTask;
		}    	
    }
    
    

}
