// $Id: Indicator.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.model;

import java.util.Date;

public interface Indicator {
    /**
     * Retrieves the number of data points that this indicator has to generate
     * values from. In real terms, this means the number of data points inputted,
     * capped by the size of the lookback period.
     */
    public int getValueCount();
    
    /**
     * Called to pass new data to the indicator.
     *
     * @param periodicData the periodic data.
     * @throws Exception to indicate any sort of problem in the indicator.
     * Data sources MAY stop sending data to a indicator that has thrown an
     * exception previously.
     */
    public void handlePeriodicData(final PeriodicData periodicData)
        throws Exception;

    /**
     * Returns whether this indicator has been given enough bars to start
     * calculations with.
     */
    public boolean hasEnoughBars();
}
