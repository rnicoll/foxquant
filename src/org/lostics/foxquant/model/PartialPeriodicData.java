// $Id: PartialPeriodicData.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.ArrayList;

import org.lostics.foxquant.model.PeriodicData;

public class PartialPeriodicData {
    public Timestamp startTime;
    public int open;
    public int high;
    public int low;
    public int close;

    public      PartialPeriodicData(final Timestamp setStartTime,
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
    public      PartialPeriodicData(final Timestamp setStartTime, final int setPrice) {
        this(setStartTime, setPrice, setPrice, setPrice, setPrice);
    }
    
    /**
     * Returns a clone of this periodic data, for passing to other methods (so the original
     * can be changed without side-effects if other code caches the values passed to them).
     */
    public PeriodicData getCopy() {
        return new PeriodicData(this.startTime, this.open, this.high, this.low, this.close);
    }

    /**
     * Resets this price bar for use in a new time period (for example at the
     * start of the next minute).
     */
    public void startNewBar(final java.sql.Timestamp setStartTime, final int setOpen) {
        this.startTime = setStartTime;
        this.close = setOpen;
        this.open = setOpen;
        this.high = setOpen;
        this.low = setOpen;
    }
    
    /**
     * Adds a new tick price to this price bar; updates high/low prices where
     * relevant, and sets the close price to this price.
     */
    public void update(final int newPrice) {
        if (this.high < newPrice) {
            this.high = newPrice;
        } else if (this.low > newPrice) {
            this.low = newPrice;
        }
        this.close = newPrice;
    }
}
