// $Id: BackfillHandler.java 706 2009-11-11 10:41:13Z jrn $
package org.lostics.foxquant.ib;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TimeZone;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.EClientSocket;
import com.ib.client.EWrapper;
import com.ib.client.Execution;
import com.ib.client.Order;
import com.ib.client.OrderState;

import org.apache.log4j.Logger;

import org.lostics.foxquant.database.DatabaseUnavailableException;
import org.lostics.foxquant.model.ContractManagerConsumer;
import org.lostics.foxquant.model.ErrorListener;
import org.lostics.foxquant.model.HistoricBarSize;
import org.lostics.foxquant.model.HistoricalDataConsumer;
import org.lostics.foxquant.model.HistoricalDataSource;
import org.lostics.foxquant.model.PeriodicData;
import org.lostics.foxquant.model.Strategy;
import org.lostics.foxquant.util.ConcurrentQueueMap;
import org.lostics.foxquant.Configuration;

/**
 * Manages backfill of data into a contract manager. Loads any available
 * data from the database, finds the earliest point from which it is
 * missing data and fills in from there. Once all the data is loaded,
 * spools it into the contract manager and passes control over to it.
 */
class BackfillHandler extends Thread implements HistoricalDataConsumer {
    private final HistoricBarSize BAR_SIZE = HistoricBarSize.ONE_MINUTE;

    private final Timestamp startTime;
    private final ConnectionManager connectionManager;

    private static final Logger log = Logger.getLogger(BackfillHandler.class);

    private final TWSContractManager contractManager;
    private final int tickerID;

    protected   BackfillHandler(final ConnectionManager setConnectionManager,
        final int setTickerID, final TWSContractManager setContractManager) {
        final Calendar calendar = Calendar.getInstance();
        final Timestamp endTime = new Timestamp(System.currentTimeMillis()
            + BAR_SIZE.getBarLengthMillis());
        this.setName("Backfill "
            + setContractManager.getContract().m_localSymbol);

        this.connectionManager = setConnectionManager;
        this.contractManager = setContractManager;
        this.tickerID = setTickerID;

        calendar.setTime(endTime);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.HOUR_OF_DAY, -ConnectionManager.BACKFILL_HOURS);
        this.startTime = new Timestamp(calendar.getTime().getTime());
    }

    private void backfillFromDB()
        throws DatabaseUnavailableException, SQLException {
        final Connection dbConnection = getDBConnection();

        try {
            final ResultSet rs;
            final PreparedStatement statement
                = dbConnection.prepareStatement("SELECT BAR_START, OPEN, HIGH, LOW, CLOSE "
                + "FROM MINUTE_BAR "
                + "WHERE CONTRACT_ID=? "
                + "AND BAR_TYPE=? "
                + "AND BAR_START>=?");

            statement.setInt(1, this.contractManager.getContract().m_conId);
            statement.setString(2, ConnectionManager.TICKER_TYPE_MIDPOINT);
            statement.setTimestamp(3, this.startTime);
            rs = statement.executeQuery();
            while (rs.next()) {
                final PeriodicData dataBar = new PeriodicData(rs.getTimestamp("BAR_START"),
                    (int)Math.round(rs.getDouble("OPEN") / this.contractManager.getMinimumTick()), (int)Math.round(rs.getDouble("HIGH") / this.contractManager.getMinimumTick()),
                    (int)Math.round(rs.getDouble("LOW") / this.contractManager.getMinimumTick()), (int)Math.round(rs.getDouble("CLOSE") / this.contractManager.getMinimumTick()));

                // XXX: Need to actually check if it has gaps
                this.contractManager.handleHistoricPrice(dataBar, true);
            }
            rs.close();
        } finally {
            dbConnection.close();
        }
    }

    private Connection getDBConnection()
        throws DatabaseUnavailableException, SQLException {
        return this.connectionManager.getConfiguration().getDBConnection();
    }

    public Contract getContract() {
        return this.contractManager.getContract();
    }

    public ContractDetails getContractDetails() {
        return this.contractManager.getContractDetails();
    }

    public void handleHistoricPrice(final PeriodicData periodicData, final boolean hasGaps) {
        this.contractManager.handleHistoricPrice(periodicData, hasGaps);
    }

    public void handleHistoricPriceError(final Exception e) {
        log.error("Historical data source returned error: "
            + e);
    }

    public void handleHistoricPriceNoData() {
        log.info("No data while retrieving historic prices.");
    }

    public void handleHistoricPriceFinished() {
        // Once we're not doing pacing, this means we can fire off the
        // contract manager ready notification. For now though, we just pass
        // along incase the contract manager wants to know.
        this.contractManager.handleHistoricPriceFinished();

        this.contractManager.start();
        this.connectionManager.fireContractManagerReady(this.contractManager);
    }

    public void run() {
        Timestamp dataEndTime = new Timestamp(System.currentTimeMillis());
        Date dataStartTime = null;

        // First up, load what we can from disk
        try {
            backfillFromDB();
        } catch(DatabaseUnavailableException e) {
            this.connectionManager.sendError("Error while loading backfill data from database: "
                + e);
        } catch(SQLException e) {
            this.connectionManager.sendError("Error while loading backfill data from database: "
                + e);
        }

        dataStartTime = this.contractManager.getFirstGapInBackfillCache();

        if (null != dataStartTime) {
            // Check that the backfill period is not entirely a closed period for the market.
            if (!this.contractManager.isMarketOpen(dataStartTime)) {
                final long dataBackfillDuration = dataEndTime.getTime() - dataStartTime.getTime();
                final long marketClosedDuration = this.contractManager.getMarketOpenDuration(dataEndTime);

                if (marketClosedDuration >= dataBackfillDuration) {
                    // Has been closed for the entire backfill period, abort.
                    this.contractManager.start();
                    this.connectionManager.fireContractManagerReady(this.contractManager);
                    return;
                }
            }

            try {
                this.connectionManager.getIQFeedGateway().requestHistoricalData(this,
                    this.getContractDetails(), dataStartTime,
                    dataEndTime, BAR_SIZE);
            } catch(InterruptedException e) {
                this.connectionManager.sendError("Interrupted while loading backfill data from IQFeed: "
                    + e);
            }
        }
    }
}
