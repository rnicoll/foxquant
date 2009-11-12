// $Id$
package org.lostics.foxquant.model;

import java.util.Date;

import com.ib.client.ContractDetails;

import org.lostics.foxquant.model.PeriodicData;

/**
 * Interface for objects which produce historic data prices.
 */
public interface HistoricalDataSource {
    public void requestHistoricalData(final HistoricalDataConsumer backfillHandler,
        final ContractDetails contractDetails, final Date startDate, final Date endDate,
        final HistoricBarSize barSize)
        throws IllegalArgumentException, InterruptedException;
}
