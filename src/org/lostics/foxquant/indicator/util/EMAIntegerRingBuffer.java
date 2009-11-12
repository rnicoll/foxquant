// $Id: EMAIntegerRingBuffer.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.indicator.util;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Adds exponential moving average to statistics generated from the integer
 * ring buffer class.
 */
public class EMAIntegerRingBuffer extends IntegerRingBuffer {
    private boolean firstValue = true;
    private double ema = 0;
    private double smoothingFactor;

    public      EMAIntegerRingBuffer(final int setSize) {
        super(setSize);
        this.smoothingFactor = 2.0 / (1 + setSize);
    }

    public void add(final int currentValue) {
        super.add(currentValue);

        if (this.firstValue &&
            this.getValueCount() == this.getSize()) {
            this.ema = getMean();
            this.firstValue = false;
        } else {
            this.ema = (currentValue * this.smoothingFactor)
                + (this.ema * (1 - this.smoothingFactor));
        }
    }

    public double getEMA() {
        return this.ema;
    }
}
