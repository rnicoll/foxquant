// $Id: StandardDeviation.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.indicator;

import java.util.Date;

import org.lostics.foxquant.indicator.util.DoubleRingBuffer;
import org.lostics.foxquant.indicator.util.IntegerRingBuffer;
import org.lostics.foxquant.model.PeriodicData;
import org.lostics.foxquant.model.Indicator;
import org.lostics.foxquant.model.PriceType;

public class StandardDeviation implements Indicator {
    private final int period;
    private final PriceType priceType;

    private final SimpleMovingAverage sma;

    /** A copy of the recent prices needed to calculate standard deviation from. */
    private final IntegerRingBuffer prices;
    
    /** A cache of standard deviation values. */
    private final DoubleRingBuffer stdDev;

    public      StandardDeviation(final int setPeriod, final PriceType setPriceType)
        throws IllegalArgumentException {
        if (setPeriod < 1) {
            throw new IllegalArgumentException("Standard deviation period must be at least 1.");
        }

        this.period = setPeriod;
        this.priceType = setPriceType;
        this.prices = new IntegerRingBuffer(setPeriod);
        this.sma = new SimpleMovingAverage(period);
        this.stdDev = new DoubleRingBuffer(setPeriod);
    }

    public      StandardDeviation(final int setPeriod)
        throws IllegalArgumentException {
        this(setPeriod, PriceType.HIGH_LOW_MEAN);
    }

    public Double getSMA()
        throws InsufficientDataException {
        return this.sma.getSMA();
    }

    public Double getStandardDeviation()
        throws InsufficientDataException {
        return this.getStandardDeviation(0);
    }

    public Double getStandardDeviation(final int offset)
        throws InsufficientDataException {
        if (offset >= this.stdDev.getValueCount()) {
            throw new InsufficientDataException(this.getClass().getName(), this.prices.getValueCount(), this.period);
        }

        return this.stdDev.get(offset);
    }
    
    public int getValueCount() {
        return this.prices.getValueCount();
    }

    public boolean hasEnoughBars() {
        return this.prices.getValueCount() >= this.period;
    }

    public void handlePeriodicData(final PeriodicData periodicData)
        throws InsufficientDataException {
        this.prices.add(periodicData.getPrice(this.priceType));
        this.sma.handlePeriodicData(periodicData);

        if (hasEnoughBars()) {
            final double mean = this.sma.getSMA();
            final double stdDev;
            final double distance;
            double variation = 0;

            for (int priceIdx = 0; priceIdx < this.period; priceIdx++) {
                double temp = this.prices.get(priceIdx) - mean;

                variation += (temp * temp);
            }
            stdDev = Math.sqrt(variation / this.period);

            this.stdDev.add(stdDev);
        }
    }
}
