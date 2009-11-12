// $Id: MACD.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.indicator;

import org.lostics.foxquant.indicator.util.EMADoubleRingBuffer;
import org.lostics.foxquant.indicator.util.EMAIntegerRingBuffer;
import org.lostics.foxquant.model.Indicator;
import org.lostics.foxquant.model.PeriodicData;
import org.lostics.foxquant.model.PriceType;

public class MACD implements Indicator {
    public static final int DEFAULT_FAST = 12;
    public static final int DEFAULT_SLOW = 26;
    public static final int DEFAULT_SIGNAL = 9;

    private final PriceType priceType;

    private final EMAIntegerRingBuffer fast;
    private final EMAIntegerRingBuffer slow;
    private final EMADoubleRingBuffer signal;

    public      MACD(final int setFast, final int setSlow, final int setSignal,
        final PriceType setPriceType)
        throws IllegalArgumentException {
        if (setFast < 1) {
            throw new IllegalArgumentException("Fast period must be at least 1.");
        }
        if (setSlow < 1) { 
            throw new IllegalArgumentException("Slow period must be at least 1.");
        }
        if (setSignal < 1) { 
            throw new IllegalArgumentException("Signal period must be at least 1.");
        }

        if (setSlow <= setFast) {
            throw new IllegalArgumentException("Slow period must be greater than the fast period.");
        }

        this.priceType = setPriceType;

        this.fast = new EMAIntegerRingBuffer(setFast);
        this.slow = new EMAIntegerRingBuffer(setSlow);
        this.signal = new EMADoubleRingBuffer(setSignal);
    }

    public      MACD()
        throws IllegalArgumentException {
        this(DEFAULT_FAST, DEFAULT_SLOW, DEFAULT_SIGNAL,
            PriceType.CLOSE);
    }

    public double getDifference()
        throws InsufficientDataException {
        if (this.slow.getValueCount() < this.slow.getSize()) {
            throw new InsufficientDataException(this.getClass().getName(), this.slow.getValueCount(), this.slow.getSize());
        }

        return this.signal.get(0);
    }

    public double getFast()
        throws InsufficientDataException {
        if (this.fast.getValueCount() < this.fast.getSize()) {
            throw new InsufficientDataException(this.getClass().getName(), this.fast.getValueCount(), this.fast.getSize());
        }

        return this.fast.getEMA();
    }

    public double getSignal()
        throws InsufficientDataException {
        if (!hasEnoughBars()) {
            throw new InsufficientDataException(this.getClass().getName(), this.signal.getValueCount(), this.signal.getSize());
        }

        return this.signal.getEMA();
    }

    public double getSlow()
        throws InsufficientDataException {
        if (this.slow.getValueCount() < this.slow.getSize()) {
            throw new InsufficientDataException(this.getClass().getName(), this.slow.getValueCount(), this.slow.getSize());
        }

        return this.slow.getEMA();
    }

    public boolean hasEnoughBars() {
        return this.signal.getValueCount() == this.signal.getSize();
    }

    public void handlePeriodicData(final PeriodicData periodicData)
        throws InsufficientDataException {
        final int currentPrice = periodicData.getPrice(this.priceType);

        this.fast.add(currentPrice);
        this.slow.add(currentPrice);
        if (this.slow.getValueCount() == this.slow.getSize()) {
            this.signal.add(this.fast.getEMA() - this.slow.getEMA());
        }
    }
}
