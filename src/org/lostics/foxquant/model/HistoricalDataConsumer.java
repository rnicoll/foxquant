// $Id: HistoricalDataConsumer.java 702 2009-11-11 00:10:10Z jrn $
package org.lostics.foxquant.model;

import org.lostics.foxquant.model.PeriodicData;

/**
 * Interface for objects which take in historic data prices. Used by
 * ConnectionManager to pass data back as it arrives.
 */
public interface HistoricalDataConsumer {
    public void handleHistoricPriceError(final Exception e);

    /**
     * Called by the historical data source to indicate that no data matched
     * the query supplied. This is called instead of handleHistoricPrice().
     * It is not guaranteed to be called, and primarily exists as an aid
     * for debugging.
     */
    public void handleHistoricPriceNoData();

    /**
     * Called by the historical data source to indicate that a set of
     * historical prices have finished being received.
     */
    public void handleHistoricPriceFinished();

    /**
     * Called by the historical data source to pass a new set of historical
     * prices to the data consumer.
     *
     * @throws Exception to indicate any sort of problem in the consumer.
     * Data sources MAY stop sending data to a consumer that has thrown an
     * exception previously.
     */
    public void handleHistoricPrice(final PeriodicData periodicData, final boolean hasGaps);
}
