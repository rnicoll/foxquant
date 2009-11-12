// $Id: Momentum.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.indicator;

import java.util.LinkedList;
import java.util.List;

import org.lostics.foxquant.indicator.util.IntegerRingBuffer;
import org.lostics.foxquant.model.Indicator;
import org.lostics.foxquant.model.PeriodicData;
import org.lostics.foxquant.model.PriceType;

public class Momentum implements Indicator {
    private final int period;
    private final PriceType priceType;

    private final IntegerRingBuffer prices;

    public      Momentum(final int setPeriod, final PriceType setPriceType)
        throws IllegalArgumentException {
        if (setPeriod < 1) {
            throw new IllegalArgumentException("Standard deviation period must be at least 1.");
        }

        this.period = setPeriod;
        this.priceType = setPriceType;
        this.prices = new IntegerRingBuffer(setPeriod);
    }

    public      Momentum(final int setPeriod)
        throws IllegalArgumentException {
        this(setPeriod, PriceType.CLOSE);
    }

    public Integer getMomentum()
        throws InsufficientDataException {
        return this.getMomentum(0);
    }

    public Integer getMomentum(final int offset)
        throws InsufficientDataException {
        if (!hasEnoughBars()) {
            throw new InsufficientDataException(this.getClass().getName(), this.prices.getValueCount(), this.period);
        }

        return this.prices.get(offset) - this.prices.get(offset + period - 1);
    }

    public boolean hasEnoughBars() {
        return this.prices.getValueCount() >= this.period;
    }

    public void handlePeriodicData(final PeriodicData periodicData)
        throws InsufficientDataException {
        this.prices.add(periodicData.getPrice(this.priceType)); 
    }
}
