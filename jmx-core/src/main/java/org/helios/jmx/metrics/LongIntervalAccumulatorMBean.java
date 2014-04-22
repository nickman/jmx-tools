package org.helios.jmx.metrics;

/**
 * <p>Title: LongIntervalAccumulatorMBean</p>
 * <p>Description: MBean interface for the long-view of an accumulator instance</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.metrics.LongIntervalAccumulatorMBean</code></p>
 */
public interface LongIntervalAccumulatorMBean extends IntervalAccumulatorMBean {

	/**
	 * Processes a new data point into this aggregator
	 * @param value The value to process
	 * @return this aggregator
	 */
	public IntervalAccumulator append(long value);


	/**
	 * Returns the long mean value
	 * @return the long mean value
	 */
	public long getLongMean();

	/**
	 * Returns the long minimum value
	 * @return the long minimum value
	 */
	public long getLongMin();

	/**
	 * Returns the long maximum value
	 * @return the long maximum value
	 */
	public long getLongMax();

}