package org.lostics.forexquant.util;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A fixed length list of data which overwrites old data once the end of
 * the queue is reached. Uses iterators to go through the data on the basis
 * that they can indicate if the next item to be head has been overwritten.
 *
 * Writes are thread safe, as are calls to iterator(), but the iterators
 * themselves must be used only within one thread.
 */
public class LossyRingBuffer<T> extends Object implements Iterable<T> {
    private final Object[] buffer;

    /** Size of the buffer without no-man's-land */
    private final int initialSize;

    /** The item the write head is currently at, from the list of all items
     * added, not just the ones currently in the buffer.
     */
    private int writeHead = 0;

    /** Size of the safety barrier of unreadable entries behind the read head
     * when the read head is created, to ensure the write head can't catch up
     * to it too fast.
     */
    private final int noMansLand;

    public      LossyRingBuffer(final int setSize) {
        this.initialSize = setSize;
        this.noMansLand = (int)Math.max(10.0, (int)setSize * 0.1);

        this.buffer = new Object[setSize + this.noMansLand];
        Arrays.fill(buffer, null);
    }

    public void add(final T newElement) {
        synchronized(this) {
            final int writeOffset = this.writeHead % this.buffer.length;
            buffer[writeOffset] = newElement;
            this.writeHead++;
        }
    }

    public Iterator<T> iterator() {
        final Iterator<T> temp;

        // Synchronize with the add() method so it holds still while we setup
        // the iterator.
        synchronized(this) {
            temp = new InternalIterator();
        }

        return temp;
    }

    private class InternalIterator extends Object implements Iterator<T> {
        private int headPosition;

        private     InternalIterator() {
            this.headPosition = LossyRingBuffer.this.writeHead
                - LossyRingBuffer.this.initialSize;
            if (this.headPosition < 0) {
                this.headPosition = 0;
            }
        }

        public boolean hasNext()
            throws ConcurrentModificationException {
            final int readWriteDifference = LossyRingBuffer.this.writeHead - this.headPosition;

            if (readWriteDifference >= LossyRingBuffer.this.buffer.length) {
                throw new ConcurrentModificationException("Lossy ring buffer has lost the next data point for this iterator.");
            }

            return readWriteDifference > 0;
        }

        public T next()
            throws ConcurrentModificationException, NoSuchElementException {
            final T nextVal = (T)LossyRingBuffer.this.buffer[this.headPosition % LossyRingBuffer.this.buffer.length];

            // We get the value, then check it is valid, to ensure there is no
            // condition. If we checked first, it could become invalid before
            // we could retrieve it. This way, the worst that can happen is we
            // get a false concurrent modification exception.
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            this.headPosition++;

            return nextVal;
        }

        public void remove()
            throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }
    }
}
