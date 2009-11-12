// $Id: DoubleRingBuffer.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.indicator.util;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A fixed length array of double values, primarily intended for indicators
 * to use as caches. NOT THREAD SAFE.
 */
public class DoubleRingBuffer extends Object {
    private final double[] buffer;

    /** The position at which a value was LAST written. This is different
     * to elsewhere, where the write head is the NEXT position.
     */
    private int writeHead;

    private double sum = 0;

    /**
     * Number of values actually written out, used to get mean correct.
     */
    private int values = 0;

    public      DoubleRingBuffer(final int setSize) {
        this.buffer = new double[setSize];
        Arrays.fill(buffer, 0.0);
        writeHead = buffer.length - 1;
    }

    public void add(final double newElement) {
        final int writeOffset;

        this.writeHead = (this.writeHead + 1) % this.buffer.length;

        if (values < buffer.length) {
            this.sum += newElement;
            values++;
        } else {
            this.sum = this.sum - this.buffer[writeHead]
                + newElement;
        }

        buffer[this.writeHead] = newElement;
    }

    /**
     * Returns the value at a specific offset.
     *
     * @param offset offset from the end of the buffer to return value for. Offsets are 0 to
     * buffer length - 1, where 0 is the newest value.
     */
    public double get(final int offset)
        throws IndexOutOfBoundsException {
        if (offset < 0 ||
            offset >= this.values) {
            throw new IndexOutOfBoundsException("Offset must be greater than zero and less than the number of values stored ("
                + this.values + "), was "
                + offset + ".");
        }

        int readHead = writeHead + this.values - offset;

        return buffer[readHead % this.values];
    }

    public double getMean() {
        return this.sum / (double)this.values;
    }

    public int getSize() {
        return this.buffer.length;
    }

    public double getSum() {
        return this.sum;
    }

    public int getValueCount() {
        return this.values;
    }
}
