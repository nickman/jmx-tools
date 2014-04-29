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
package org.helios.jmx.concurrency;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.management.ObjectName;

import org.helios.jmx.util.helpers.ConfigurationHelper;
import org.helios.jmx.util.helpers.JMXHelper;

/**
 * <p>Title: JMXManagedThreadPool</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.concurrency.JMXManagedThreadPool</code></p>
 */

public class JMXManagedThreadPool extends ThreadPoolExecutor implements ThreadFactory, RejectedExecutionHandler, UncaughtExceptionHandler, JMXManagedThreadPoolMBean {
	/** The JMX ObjectName for this pool's MBean */
	protected final ObjectName objectName;
	/** The pool name */
	protected final String poolName;
	/** The task work queue */
	protected final ArrayBlockingQueue<Runnable> workQueue;
	/** The instance logger */
	protected final Logger log;
	/** The count of uncaught exceptions */
	protected final AtomicLong uncaughtExceptionCount = new AtomicLong(0L);
	/** The count of rejected executions where the task queue was full and a new task could not be accepted */
	protected final AtomicLong rejectedExecutionCount = new AtomicLong(0L);
	/** The thread group that threads created for this pool are created in */
	protected final ThreadGroup threadGroup;
	/** The thread factory thread serial number factory */
	protected final AtomicInteger threadSerial = new AtomicInteger(0);
	/** Threadlocal to hold the start time of a given task */
	protected final ThreadLocal<long[]> taskStartTime = new ThreadLocal<long[]>() {
		@Override
		protected long[] initialValue() {
			return new long[1];
		}
	};

	


	/**
	 * Creates a new JMXManagedThreadPool, reading all the configuration values from ECS Config
	 * @param objectName The JMX ObjectName for this pool's MBean 
	 * @param poolName The pool name
	 */
	public JMXManagedThreadPool(ObjectName objectName, String poolName) {
		this(
			objectName, 
			poolName,
			
			ConfigurationHelper.getIntSystemThenEnvProperty(poolName.toLowerCase() + CONFIG_CORE_POOL_SIZE, DEFAULT_CORE_POOL_SIZE), 
			ConfigurationHelper.getIntSystemThenEnvProperty(poolName.toLowerCase() + CONFIG_MAX_POOL_SIZE, DEFAULT_MAX_POOL_SIZE), 
			ConfigurationHelper.getIntSystemThenEnvProperty(poolName.toLowerCase() + CONFIG_MAX_QUEUE_SIZE, DEFAULT_MAX_QUEUE_SIZE), 
			ConfigurationHelper.getLongSystemThenEnvProperty(poolName.toLowerCase() + CONFIG_KEEP_ALIVE, DEFAULT_KEEP_ALIVE), 
			ConfigurationHelper.getIntSystemThenEnvProperty(poolName.toLowerCase() + CONFIG_WINDOW_SIZE, DEFAULT_WINDOW_SIZE),
			ConfigurationHelper.getIntSystemThenEnvProperty(poolName.toLowerCase() + CONFIG_WINDOW_PERCENTILE, DEFAULT_WINDOW_PERCENTILE)
		);
		int prestart = ConfigurationHelper.getIntSystemThenEnvProperty(CONFIG_CORE_PRESTART, DEFAULT_CORE_PRESTART);
		for(int i = 0; i < prestart; i++) {
			prestartCoreThread();
		}
	}
	
	public static void main(String[] args) {
		for(Field f: JMXManagedThreadPoolMBean.class.getDeclaredFields()) {
			try {
				if(f.getName().startsWith("CONFIG_")) {
					String key = "crystal-scheduling" + f.get(null);
					Field v = JMXManagedThreadPoolMBean.class.getDeclaredField(f.getName().replace("CONFIG", "DEFAULT"));
					System.out.println("[,*].onexconfig." + key + "=" + v.get(null));
				}
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
			}
		}
		//System.out.println("[,*].onexconfig." + )
	}

	/**
	 * Creates a new JMXManagedThreadPool
	 * @param objectName The JMX ObjectName for this pool's MBean 
	 * @param poolName The pool name
	 * @param corePoolSize  the number of threads to keep in the pool, even if they are idle.
	 * @param maximumPoolSize the maximum number of threads to allow in the pool.
	 * @param queueSize The maximum number of pending tasks to queue
	 * @param keepAliveTimeMs when the number of threads is greater than the core, this is the maximum time in ms. that excess idle threads will wait for new tasks before terminating.
	 * @param metricWindowSize The maximum size of the metrics sliding window
	 * @param metricDefaultPercentile The default percentile reported in the metrics management  
	 */
	public JMXManagedThreadPool(ObjectName objectName, String poolName, int corePoolSize, int maximumPoolSize, int queueSize, long keepAliveTimeMs, int metricWindowSize, int metricDefaultPercentile) {
		super(corePoolSize, maximumPoolSize, keepAliveTimeMs, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(queueSize, false));
		this.threadGroup = new ThreadGroup(poolName + "ThreadGroup");
		setThreadFactory(this);
		setRejectedExecutionHandler(this);
		log = LogManager.getLogManager().getLogger(getClass().getName() + "." + poolName);
		this.objectName = objectName;
		this.poolName = poolName;
		workQueue = (ArrayBlockingQueue<Runnable>)getQueue();
		try {			
			JMXHelper.getHeliosMBeanServer().registerMBean(this, objectName);
		} catch (Exception ex) {
			log.log(Level.WARNING, "Failed to register JMX management interface. Will continue without.", ex);
		}
		log.info("Created JMX Managed Thread Pool [" + poolName + "]");
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ThreadPoolExecutor#beforeExecute(java.lang.Thread, java.lang.Runnable)
	 */
	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		taskStartTime.get()[0] = System.currentTimeMillis();
		super.beforeExecute(t, r);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ThreadPoolExecutor#afterExecute(java.lang.Runnable, java.lang.Throwable)
	 */
	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		if(t==null) {
			long elapsed = System.currentTimeMillis() - taskStartTime.get()[0]; 
		}
		super.afterExecute(r, t);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
	 */
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		uncaughtExceptionCount.incrementAndGet();
		log.log(Level.WARNING, "Thread pool handled uncaught exception on thread [" + t + "]", e);
		
	}


	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.RejectedExecutionHandler#rejectedExecution(java.lang.Runnable, java.util.concurrent.ThreadPoolExecutor)
	 */
	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
		rejectedExecutionCount.incrementAndGet();
		log.log(Level.SEVERE, "Submitted execution task [" + r + "] was rejected due to a full task queue", new Throwable());		
	}


	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(threadGroup, r, poolName + "Thread#" + threadSerial.incrementAndGet());
		t.setDaemon(true);
		return t;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.rindle.util.jmx.concurrency.JMXManagedThreadPoolMBean#getObjectName()
	 */
	@Override
	public ObjectName getObjectName() {
		return objectName;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.rindle.util.jmx.concurrency.JMXManagedThreadPoolMBean#getPoolName()
	 */
	@Override
	public String getPoolName() {
		return poolName;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.rindle.util.jmx.concurrency.JMXManagedThreadPoolMBean#getQueueDepth()
	 */
	@Override
	public int getQueueDepth() {
		return workQueue.size();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.rindle.util.jmx.concurrency.JMXManagedThreadPoolMBean#getQueueCapacity()
	 */
	@Override
	public int getQueueCapacity() {
		return workQueue.remainingCapacity();
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.rindle.util.jmx.concurrency.JMXManagedThreadPoolMBean#getUncaughtExceptionCount()
	 */
	@Override
	public long getUncaughtExceptionCount() {
		return uncaughtExceptionCount.get();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.rindle.util.jmx.concurrency.JMXManagedThreadPoolMBean#getRejectedExecutionCount()
	 */
	@Override
	public long getRejectedExecutionCount() {
		return rejectedExecutionCount.get();
	}

//	/**
//	 * {@inheritDoc}
//	 * @see org.helios.rindle.util.jmx.concurrency.JMXManagedThreadPoolMBean#getMetrics()
//	 */
//	@Override
//	public Map<String, Long> getMetrics() {
//		Map<String, Long> map = new TreeMap<String, Long>();
//		for(Map.Entry<MetricType, Long> ex: metrics.getMetrics().entrySet()) {
//			map.put(ex.getKey().name(), ex.getValue());
//		}
//    	return map;
//	}
	
//	/**
//	 * {@inheritDoc}
//	 * @see org.helios.rindle.util.jmx.concurrency.JMXManagedThreadPoolMBean#getMetricsTable()
//	 */
//	@Override
//	public String getMetricsTable() {
//		StringBuilder b = new StringBuilder(METRIC_TABLE_HEADER);
//		b.append("<tr>");
//		b.append("<td>").append(poolName).append("</td>");
//		for(Map.Entry<String, Long> ex: getMetrics().entrySet()) {
//			b.append("<td>").append(ex.getValue()).append("</td>");
//		}
//		b.append("</tr>");    					
//		return b.append("</table>").toString();
//	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.rindle.util.jmx.concurrency.JMXManagedThreadPoolMBean#getExecutingTaskCount()
	 */
	@Override
	public long getExecutingTaskCount() {
		return getTaskCount()-getCompletedTaskCount();
	}
	
	public void execute(Runnable r) {
		super.execute(r);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.AbstractExecutorService#submit(java.lang.Runnable)
	 */
	@Override
	public Future<?> submit(Runnable task) {
		return super.submit(task);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.AbstractExecutorService#submit(java.lang.Runnable, java.lang.Object)
	 */
	@Override
	public <T> Future<T> submit(Runnable task, T result) {		
		return super.submit(task, result);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.AbstractExecutorService#submit(java.util.concurrent.Callable)
	 */
	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return super.submit(task);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.rindle.util.jmx.concurrency.JMXManagedThreadPoolMBean#getKeepAliveTime()
	 */
	@Override
	public long getKeepAliveTime() {
		return getKeepAliveTime(TimeUnit.MILLISECONDS);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.rindle.util.jmx.concurrency.JMXManagedThreadPoolMBean#setKeepAliveTime(long)
	 */
	@Override
	public void setKeepAliveTime(long keepAliveTimeMs) {
		setKeepAliveTime(keepAliveTimeMs, TimeUnit.MILLISECONDS);
	}
	
//	/**
//	 * {@inheritDoc}
//	 * @see org.helios.rindle.util.jmx.concurrency.JMXManagedThreadPoolMBean#reset()
//	 */
//	@Override
//	public void reset() {
//		t
//	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.rindle.util.jmx.concurrency.JMXManagedThreadPoolMBean#waitForCompletion(java.util.Collection, long)
	 */
	@Override
	public boolean waitForCompletion(Collection<Future<?>> futures, long timeout) {
		if(futures.isEmpty()) return true;
		final long expiryTime = System.currentTimeMillis() + timeout;
		final boolean[] bust = new boolean[]{false};
		while(System.currentTimeMillis() <= expiryTime) {
			if(bust[0]) return false;
			for(Iterator<Future<?>> fiter = futures.iterator(); fiter.hasNext();) {
				Future<?> f = fiter.next();
				if(f.isDone() || f.isCancelled()) {
					fiter.remove();
				} else {
					try {
						f.get(200, TimeUnit.MILLISECONDS);
						fiter.remove();
					} catch (CancellationException e) {
						log.log(Level.WARNING, "Task Was Cancelled", e);
						fiter.remove();						
					} catch (InterruptedException e) {
						log.log(Level.WARNING, "Thread interrupted while waiting for task check to complete", e);
						bust[0] = true;
					} catch (ExecutionException e) {
						log.log(Level.WARNING, "Task Failed", e);
						fiter.remove();
					} catch (TimeoutException e) {
						/* No Op */
					} catch (Exception e) {
						log.log(Level.WARNING, "Task Failure. Cancelling check.", e);
						fiter.remove();
					}
				}
			}
			if(futures.isEmpty()) return true;
		}
		for(Future<?> f: futures) { f.cancel(true); }
		log.log(Level.WARNING, "Task completion timed out with [" + futures.size() + "] tasks incomplete");
		futures.clear();
		return false;
	}
	

}