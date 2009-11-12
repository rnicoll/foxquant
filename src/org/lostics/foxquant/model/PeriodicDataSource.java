// $Id: PeriodicDataSource.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.model;

/**
 * Interface for classes that supply prices in the form of periodic high, low,
 * open and close prices.
 */
public interface PeriodicDataSource {
	/**
	 * Add a new periodic data consumer to those that this class broadcasts
 	 * incoming data to. MUST be thread safe to call. Would normally be
     * expected to be call from the consumer, so that the consumer is aware
     * it's being added to this source.
	 *
	 * @param consumer the new consumer to add. The result of adding the same
	 * consumer twice is undefined.
 	 */
    public void addRealtimeBarConsumer(final Strategy consumer);

	/**
	 * Get the time, in milliseconds, that each periodic data bar represents.
	 * Each bar MUST be over the same time period. Would normally be
     * expected to be call from the consumer, so that the consumer is aware
     * it's being removed from this source.
	 */
    public long getBarPeriod();

    public void removeRealtimeBarConsumer(final Strategy consumer);
}
