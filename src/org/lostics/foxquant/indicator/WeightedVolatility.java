// $Id: WeightedVolatility.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.indicator;

import java.util.LinkedList;
import java.util.List;

import org.lostics.foxquant.model.Indicator;
import org.lostics.foxquant.model.PeriodicData;
import org.lostics.foxquant.model.PriceType;

/**
 * FIXME: Untested
 * Based on http://www.investopedia.com/articles/07/EWMA.asp
 */
public class WeightedVolatility implements Indicator {
    public static final double LAMBDA = 0.94;

    private final int period;
    private final PriceType priceType;

    // Using linked lists because we're inserting at the front
    // XXX: Should be fixed length buffer, maybe ring buffer?
    private final List<Double> prices = new LinkedList<Double>();
    private final List<Double> periodicReturns = new LinkedList<Double>();
    private final List<Double> volatilities = new LinkedList<Double>();

    public      WeightedVolatility(final int setPeriod, final PriceType setPriceType)
        throws IllegalArgumentException {
        if (setPeriod < 1) {
            throw new IllegalArgumentException("Standard deviation period must be at least 1.");
        }

        this.period = setPeriod;
        this.priceType = setPriceType;
    }

    public      WeightedVolatility(final int setPeriod)
        throws IllegalArgumentException {
        this(setPeriod, PriceType.HIGH_LOW_MEAN);
    }

    public Double getWeightedVolatility()
        throws InsufficientDataException {
        return this.getWeightedVolatility(0);
    }

    public Double getWeightedVolatility(final int offset)
        throws InsufficientDataException {
        if (this.prices.size() < this.period) {
            throw new InsufficientDataException(this.getClass().getName(), this.prices.size(), this.period);
        }

        return this.volatilities.get(offset);
    }

    public int getValueCount() {
        return this.prices.size();
    }

    public boolean hasEnoughBars() {
        return this.getValueCount() >= this.period;
    }

    public void handlePeriodicData(final PeriodicData periodicData)
        throws InsufficientDataException {
        final double currentPrice = periodicData.getPrice(this.priceType);
        final double lastPrice;
        double periodicReturn;

        this.prices.add(0, currentPrice);

        if (this.prices.size() > 1) {
            lastPrice = this.prices.get(1);
            periodicReturn = Math.log(currentPrice / lastPrice);

            this.periodicReturns.add(0, periodicReturn);

            if (hasEnoughBars()) {
                double volatility = 0;
                double weight = (1 - LAMBDA);

                for (int returnIdx = 0; returnIdx < this.period; returnIdx++) {
                    periodicReturn = this.periodicReturns.get(returnIdx);
                    weight = weight * LAMBDA;
                    volatility = volatility + (periodicReturn * weight);
                }
                volatility = Math.sqrt(volatility);
                volatilities.add(volatility);
            }
        }
    }
}
