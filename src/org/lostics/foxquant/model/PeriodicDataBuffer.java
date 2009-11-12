// $Id: PeriodicDataBuffer.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.model;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Periodic data consumer that records the incoming data into a buffer for
 * retrieval later.
 * XXX: Has a lot in common with the lossy ring buffer, might be better to
 * make this a subclass?
 */
public final class PeriodicDataBuffer extends Object implements Iterable<PeriodicData> {
    private final static int NO_MANS_LAND = 10;

    // Synchronize access to the buffer and writeHead on buffer.
    private final PeriodicData[] buffer;

    /** The index from the startTime of the next data point to be written */
    private int writeHead = 0;

    private final long stepPeriodMillis;
    private final long startTime;

    public          PeriodicDataBuffer(final int setDataPoints, final Date setEndTime, final long setStepPeriodMillis) {
        assert setStepPeriodMillis > 0;
        assert setDataPoints > 0;

        this.startTime = setEndTime.getTime() - (setStepPeriodMillis * setDataPoints);
        this.stepPeriodMillis = setStepPeriodMillis;

        this.buffer = new PeriodicData[setDataPoints + NO_MANS_LAND];
        Arrays.fill(this.buffer, null);
    }

    /**
     * Returns the first time in the buffer where there is a gap in the data.
     */
    public Timestamp getFirstGap() {
        Timestamp gap = null;

        synchronized(this.buffer) {
            int readHead = this.writeHead
                - this.buffer.length + NO_MANS_LAND;

            if (readHead < 0) {
                readHead = 0;
            }

            for (; readHead < this.writeHead; readHead++) {
                if (null == this.buffer[readHead % this.buffer.length]) {
                    gap = new Timestamp(this.startTime
                        + readHead * this.stepPeriodMillis);
                    break;
                }
            }

            if (null == gap &&
                this.writeHead < (buffer.length - NO_MANS_LAND)) {
                // Haven't completed initial fill yet.
                // XXX: Need a test case for this.
                gap = new Timestamp(this.startTime
                    + this.writeHead * this.stepPeriodMillis);
            }
        }

        return gap;
    }

    /**
     * Writes the incoming periodic data into the buffer. If before the period
     * the buffer currently covers, silently drops the data. If after, the
     * buffer is moved forwards to cover the new data point.
     */
    public void handlePeriodicData(final PeriodicData periodicData) {
        final long millisSinceStart = periodicData.startTime.getTime() - this.startTime;
        int targetIdx;

        synchronized(this.buffer) {
            targetIdx = (int)(millisSinceStart / this.stepPeriodMillis);
            if (targetIdx < 0 ||
                targetIdx < (this.writeHead - this.buffer.length)) {
                // Before the start of the current buffer, disregard.
                return;
            }

            if (targetIdx >= this.writeHead) {
                if (targetIdx > this.writeHead) {
                    // Ensure any data points we're skipping are cleared
                    for (int skipIdx = this.writeHead; skipIdx < targetIdx; skipIdx++) {
                        this.buffer[skipIdx % this.buffer.length] = null;
                    }
                }
                this.writeHead = targetIdx + 1;
            }
            this.buffer[targetIdx % this.buffer.length] = periodicData;
        }
    }

    public Iterator<PeriodicData> iterator() {
        return new InternalIterator();
    }

    private class InternalIterator extends Object implements Iterator<PeriodicData> {
        private int headPosition;

        private     InternalIterator() {
            this.headPosition = PeriodicDataBuffer.this.writeHead
                - PeriodicDataBuffer.this.buffer.length + NO_MANS_LAND;
            if (this.headPosition < 0) {
                this.headPosition = 0;
            }
        }

        public boolean hasNext() {
            final int readWriteDifference = PeriodicDataBuffer.this.writeHead - this.headPosition;

            return readWriteDifference > 0;
        }

        public PeriodicData next()
            throws NoSuchElementException {
            final PeriodicData nextVal;

            // First a simple test for more data.
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            synchronized(PeriodicDataBuffer.this.buffer) {
                final int writeHead = PeriodicDataBuffer.this.writeHead - PeriodicDataBuffer.this.buffer.length;

                // If we're behind the buffer start, catchup
                if (this.headPosition < (PeriodicDataBuffer.this.writeHead - PeriodicDataBuffer.this.buffer.length)) {
                    this.headPosition = PeriodicDataBuffer.this.writeHead - PeriodicDataBuffer.this.buffer.length;
                }

                // Skip missing entries
                while (PeriodicDataBuffer.this.buffer[this.headPosition % PeriodicDataBuffer.this.buffer.length] == null) {
                    this.headPosition++;
                }

                nextVal = PeriodicDataBuffer.this.buffer[this.headPosition % PeriodicDataBuffer.this.buffer.length];
                this.headPosition++;
            }

            return nextVal;
        }

        public void remove()
            throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }
    }
}
