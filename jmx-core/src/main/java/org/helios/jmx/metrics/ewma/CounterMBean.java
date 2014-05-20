package org.helios.jmx.metrics.ewma;

/**
 * <p>Title: CounterMBean</p>
 * <p>Description: MBean interface for {@link Counter}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.metrics.ewma.CounterMBean</code></p>
 */
public interface CounterMBean {

	/**
	 * Increments the counter by one 
	 */
	public void incr();

	/**
	 * Increments the error count by one 
	 */
	public void err();

	/**
	 * Increments the counter by the passed value
	 * @param value the value to increment by
	 */
	public void append(double value);

	/**
	 * Increments the counter by the passed value
	 * @param value the value to increment by
	 */
	public void append(long value);

	/**
	 * Increments the counter by the passed value
	 * @param value the value to increment by
	 */
	public void append(int value);

	/**
	 * Resets this counter
	 */
	public void reset();

	/**
	 * Returns the error count
	 * @return the error count
	 */
	public long getErrors();
	
	/**
	 * Returns the current counter value
	 * @return the current counter value
	 */
	public long getValue();

}