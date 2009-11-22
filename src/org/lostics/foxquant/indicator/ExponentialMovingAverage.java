// $Id: ExponentialMovingAverage.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.indicator;

import org.lostics.foxquant.indicator.util.EMAIntegerRingBuffer;
import org.lostics.foxquant.model.PeriodicData;
import org.lostics.foxquant.model.Indicator;
import org.lostics.foxquant.model.PriceType;

public class ExponentialMovingAverage implements Indicator {
    private final PriceType priceType;

    private final EMAIntegerRingBuffer prices;

    public      ExponentialMovingAverage(final int setPeriod, final PriceType setPriceType)
        throws IllegalArgumentException {
        if (setPeriod < 1) {
            throw new IllegalArgumentException("Standard deviation period must be at least 1.");
        }

        this.prices = new EMAIntegerRingBuffer(setPeriod);
        this.priceType = setPriceType;
    }

    public      ExponentialMovingAverage(final int setPeriod)
        throws IllegalArgumentException {
        this(setPeriod, PriceType.HIGH_LOW_MEAN);
    }

    public Double getExponentialMovingAverage()
        throws InsufficientDataException {
        if (!this.hasEnoughBars()) {
            throw new InsufficientDataException(this.getClass().getName(), this.prices.getValueCount(), this.prices.getSize());
        }

        return this.prices.getEMA();
    }

    public int getValueCount() {
        return this.prices.getValueCount();
    }

    public boolean hasEnoughBars() {
        return this.getValueCount() == this.prices.getSize();
    }

    public void handlePeriodicData(final PeriodicData periodicData)
        throws InsufficientDataException {
        final int currentPrice = periodicData.getPrice(this.priceType);

        this.prices.add(currentPrice);
    }
}
