// $Id: SimpleMovingAverage.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.indicator;

import java.util.Date;

import org.lostics.foxquant.indicator.util.IntegerRingBuffer;
import org.lostics.foxquant.model.Indicator;
import org.lostics.foxquant.model.PeriodicData;
import org.lostics.foxquant.model.PriceType;

public class SimpleMovingAverage implements Indicator {
    private final PriceType priceType;
    private final IntegerRingBuffer prices;

    public      SimpleMovingAverage(final int setPeriod, final PriceType setPriceType)
        throws IllegalArgumentException {
        if (setPeriod < 1) {
            throw new IllegalArgumentException("SMA period must be at least 1.");
        }

        this.prices = new IntegerRingBuffer(setPeriod);
        this.priceType = setPriceType;
    }

    public      SimpleMovingAverage(final int setPeriod)
        throws IllegalArgumentException {
        this(setPeriod, PriceType.HIGH_LOW_MEAN);
    }

    public      SimpleMovingAverage() {
        this(3);
    }

    public Double getSMA()
        throws InsufficientDataException {
        if (!this.hasEnoughBars()) {
            throw new InsufficientDataException(this.getClass().getName(),
                this.prices.getValueCount(), this.prices.getSize());
        }

        return this.prices.getMean();
    }

    public boolean hasEnoughBars() {
        return this.prices.getValueCount() == this.prices.getSize();
    }

    public void handlePeriodicData(final PeriodicData periodicData) {
        this.prices.add(periodicData.getPrice(this.priceType));
    }
}
