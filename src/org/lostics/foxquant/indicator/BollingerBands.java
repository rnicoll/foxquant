// $Id: BollingerBands.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.indicator;

import java.util.Date;

import org.lostics.foxquant.indicator.util.DoubleRingBuffer;
import org.lostics.foxquant.model.Indicator;
import org.lostics.foxquant.model.PeriodicData;
import org.lostics.foxquant.model.PriceType;

public class BollingerBands implements Indicator {
    private final int period;
    private final PriceType priceType;
    private final double spread;

    private final DoubleRingBuffer means;
    private final DoubleRingBuffer upper;
    private final DoubleRingBuffer lower;

    private final StandardDeviation standardDeviation;

    /**
     * @param setUpperSpread how many standard deviations above the SMA to place the
     * upper band.
     * @param setLowerSpread how many standard deviations below the SMA to place the
     * lower band.
     * @param setPeriod the sample period for prices.
     * @param setLookback how many data points to store historical values for.
     * @param setPriceType the type of price (high, low, mid, etc.) to use for price sample.
     */
    public      BollingerBands(final double setSpread, final int setPeriod,
        final int setLookback, final PriceType setPriceType)
        throws IllegalArgumentException {
        if (setSpread < 0) {
            throw new IllegalArgumentException("Bollinger band spread must be positive.");
        }
        if (setPeriod < 1) {
            throw new IllegalArgumentException("Bollinger band period must be at least 1.");
        }

        this.spread = setSpread;
        this.period = setPeriod;
        this.priceType = setPriceType;

        this.means = new DoubleRingBuffer(setLookback);
        this.lower = new DoubleRingBuffer(setLookback);
        this.upper = new DoubleRingBuffer(setLookback);

        this.standardDeviation = new StandardDeviation(this.period);
    }

    public      BollingerBands(final double setSpread, final int setPeriod) {
        this(setSpread, setPeriod, setPeriod, PriceType.HIGH_LOW_MEAN);
    }

    public      BollingerBands() {
        this(2.0, 14);
    }

    public Integer getLower()
        throws InsufficientDataException {
        return this.getLower(0);
    }

    public Integer getLower(final int offset)
        throws InsufficientDataException {
        final Double value = this.lower.get(offset);
        
        // Our output doesn't have as much precision as we use for calculation,
        // so we convert to an Integer
        return (null == value 
            ? null
            : new Integer((int)Math.round(value)));
    }

    /**
     * Retrieves the SMA as a valid price point (whereas getSMA() returns as
     * precision as it can.
     */
    public int getMidpoint()
        throws InsufficientDataException {
        return (int)Math.round(this.getSMA(0));
    }

    public double getSMA()
        throws InsufficientDataException {
        return this.getSMA(0);
    }

    public double getSMA(final int offset)
        throws IndexOutOfBoundsException, InsufficientDataException {
        if (offset >= this.means.getSize()) {
            throw new IndexOutOfBoundsException("Offset "
                + offset + " is beyond the lookback period "
                + this.means.getSize() + ".");
        }
        if (offset >= this.means.getValueCount()) {
            throw new IndexOutOfBoundsException("Insufficient data to have generated SMA for "
                + offset + " bars ago, have "
                + this.means.getValueCount() + " mean values.");
        }

        return this.means.get(offset);
    }

    public Double getStandardDeviation()
        throws InsufficientDataException {
        return this.standardDeviation.getStandardDeviation(0);
    }

    public Double getStandardDeviation(final int offset)
        throws InsufficientDataException {
        return this.standardDeviation.getStandardDeviation(offset);
    }

    public Integer getUpper()
        throws InsufficientDataException {
        return this.getUpper(0);
    }

    public Integer getUpper(final int offset)
        throws InsufficientDataException {
        final Double value = this.upper.get(offset);
        
        // Our output doesn't have as much precision as we use for calculation,
        // so we convert to an Integer
        return (null == value 
            ? null
            : new Integer((int)Math.round(value)));
    }

    public boolean hasEnoughBars() {
        return this.standardDeviation.hasEnoughBars();
    }

    public void handlePeriodicData(final PeriodicData periodicData)
        throws InsufficientDataException {
        this.standardDeviation.handlePeriodicData(periodicData);

        if (this.hasEnoughBars()) {
            final double distance;
            final double mean;
            final double stdDev;

            mean = this.standardDeviation.getSMA();
            stdDev = this.standardDeviation.getStandardDeviation();

            distance = stdDev * this.spread;

            this.means.add(mean);
            this.lower.add(mean - distance);
            this.upper.add(mean + distance);
        }
    }
}
