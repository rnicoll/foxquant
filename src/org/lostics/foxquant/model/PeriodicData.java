// $Id: PeriodicData.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.util.List;
import java.util.ArrayList;

public class PeriodicData {
    public final Timestamp startTime;
    public final int open;
    public final int high;
    public final int low;
    public final int close;

    public      PeriodicData(final Timestamp setStartTime,
        final int setOpen, final int setHigh, final int setLow, final int setClose) {
        this.startTime = setStartTime;

        this.open = setOpen;
        this.high = setHigh;
        this.low = setLow;
        this.close = setClose;
    }

    /**
     * Constructs a periodic data point where the price did not change during
     * the time period. Primarily intended for use in unit testing.
     */
    public      PeriodicData(final Timestamp setStartTime, final int setPrice) {
        this(setStartTime, setPrice, setPrice, setPrice, setPrice);
    }
    
    /** Extract a set of data from a ResultSet.
     * Useful for backtesting with...
     * The resultset _must_ have columns named
     * start_time, open, high, low and close
     * (Although they need not be in that order ... 
     * haha, only kidding, columns in SQL _arn't_ ordered, despit what JDBC wants to think...)
     * Ahem.  Anyway, the use case is to basically call PeriodicData.getFrom(statement.execteQuery());
     * This method _will_ close the ResultSet once it's finished with it.
     */
    public static List<PeriodicData> getFrom(final double minTick, final ResultSet rs) throws SQLException {
        final List<PeriodicData> ret = new ArrayList<PeriodicData>();

        while (rs.next()) {
            final PeriodicData data = new PeriodicData(rs.getTimestamp("START_TIME"), 
                (int)(rs.getDouble("OPEN") / minTick), (int)(rs.getDouble("HIGH") / minTick), 
                (int)(rs.getDouble("LOW") / minTick), (int)(rs.getDouble("CLOSE") / minTick));
            ret.add(data);
        }
        
        rs.close();
        
        return ret;
    }

    /**
     * Get the given price type from this periodic data point.
     *
     * @param priceType type of price (high, low, close, open) to retrieve.
     * @throws IllegalArgumentException if the price type given is not one
     * supported for periodic data.
     */
    public int   getPrice(final PriceType priceType)
        throws IllegalArgumentException {
        final int price;

        switch(priceType) {
        case OPEN:
            price = this.open;
            break;
        case HIGH:
            price = this.high;
            break;
        case LOW:
            price = this.low;
            break;
        case CLOSE:
            price = this.close;
            break;
        case HIGH_LOW_MEAN:
            price = (this.high + this.low) / 2;
            break;
        default:
            throw new IllegalArgumentException("Unrecognised price type \""
                + priceType + "\" passed to PeriodicData.get().");
        }

        return price;
    }
}
