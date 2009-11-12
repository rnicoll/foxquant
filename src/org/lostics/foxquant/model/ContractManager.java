// $Id: ContractManager.java 690 2009-11-09 10:41:12Z  $
package org.lostics.foxquant.model;

import java.sql.SQLException;
import java.util.Date;

import com.ib.client.ContractDetails;
import com.ib.client.Contract;
import com.ib.client.Order;

import org.lostics.foxquant.ib.OrderIDUnavailableException;
import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.database.DatabaseUnavailableException;

/**
 * Manages a single contract; handles separating incoming data into a
 * per-contract thread, tracks position and orders on that contract, etc.
 */
public interface ContractManager {
    /* 12 hours at 5 seconds intervals. */
    public static final int BACKFILL_CACHE_SIZE    = ConnectionManager.BACKFILL_HOURS
        * 60 * 60 / ConnectionManager.REAL_TIME_BAR_PERIOD_SECONDS;

    public static final String ORDER_ACTION_BUY = "BUY";
    public static final String ORDER_ACTION_SELL = "SELL";
    public static final String ORDER_ACTION_SELL_SHORT = "SSHORT";

    public void addPositionListener(final ContractPositionListener listener);

    public void close()
        throws SQLException;
    
    /**
     * Converts a quantity of ticks into a price string suitable for display
     * in a user interface. This method must be thread safe without external
     * synchronization.
     */
    public String formatTicksAsPrice(int ticks);

    public Contract getContract();
    
    public ContractDetails getContractDetails();

    public int getContractID();

    /**
     * Returns the date the market next closes at. Does not necessarily check
     * the market is open. Has no matching getMarketOpenTime() method because
     * these are tricky, fragile calculations and there is no explicit need
     * for one.
     *
     * @param now the timestamp to use as the current time. Result of calling
     * this when the market is closed, is undefined.
     */
    public Date getMarketCloseTime(final Date now);

    /**
     * Returns how long since the market closed. Results of calling
     * this when the market is opened are undefined.
     *
     * @param the number of milliseconds since the market close
     */
    public long getMarketCloseDuration(final Date now);

    /**
     * Returns how long since the market opened. Results of calling
     * this when the market is closed are undefined.
     *
     * @param the number of milliseconds since the market opened.
     */
    public long getMarketOpenDuration(final Date now);
    
    /**
     * Returns the minimum difference between prices for the contract being managed.
     */
    public double getMinimumTick();
    
    public Strategy getStrategy();

    /**
     * Returns whether the wrapped contract is a Forex contract with the New
     * Zealand dollar (NZD) on either side. This is required because someone
     * thought it would be a good idea for the NZD to have its own open/close
     * times.
     */
    public boolean isContractForexNZD();

    public boolean isFlat();

    public boolean isMarketOpen(final Date now);

    public boolean isLong();

    public boolean isShort();

    public void removePositionListener(final ContractPositionListener listener);
}
