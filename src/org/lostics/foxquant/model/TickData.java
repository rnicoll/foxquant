// $Id: TickData.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.util.List;
import java.util.ArrayList;

/**
 * A snapshot of bid/ask price and size at a single moment in time. All prices
 * are stored as a multiple of minimum tick size for the contract.
 */
public class TickData {
    public long timeMillis;
    public Integer bidPrice;
    public Integer askPrice;
    public Integer bidSize;
    public Integer askSize;

    public      TickData(final long setTimeMillis,
        final Integer setBidPrice, final Integer setAskPrice,
        final Integer setBidSize, final Integer setAskSize) {
        this.timeMillis = setTimeMillis;

        this.bidPrice = setBidPrice;
        this.askPrice = setAskPrice;
        this.bidSize = setBidSize;
        this.askSize = setAskSize;
    }

    public      TickData(
        final Integer setBidPrice, final Integer setAskPrice,
        final Integer setBidSize, final Integer setAskSize) {
        this(System.currentTimeMillis(), setBidPrice, setAskPrice, setBidSize, setAskSize);
    }

    public      TickData() {
        this(null, null, null, null);
    }

    /**
     * Returns a clone of this periodic data, for passing to other methods (so the original
     * can be changed without side-effects if other code caches the values passed to them).
     */
    public TickData getCopy() {
        return new TickData(this.timeMillis,
            this.bidPrice, this.askPrice,
            this.bidSize, this.askSize);
    }
    
    @Override
    public String toString() {
        return "Bid: "
            + bidPrice + ", ask: "
            + askPrice;
    }
}
