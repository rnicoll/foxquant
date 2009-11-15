package org.lostics.forexquant.util;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

public class LossyRingBufferTest extends Object {
    @Test(expected= ConcurrentModificationException.class)
    public void testConcurrentOverwrite() {
        final Iterator<String> iterator;
        final LossyRingBuffer<String> ringBuffer = new LossyRingBuffer<String>(2);

        iterator = ringBuffer.iterator();
        for (int i = 0; i < 13; i++) {
            ringBuffer.add(Integer.toString(i));
        }
        iterator.next();
    }

    @Test
    public void testFirstLap() {
        final Iterator<String> iterator;
        final LossyRingBuffer<String> ringBuffer = new LossyRingBuffer<String>(2);

        ringBuffer.add("1");
        ringBuffer.add("2");

        iterator = ringBuffer.iterator();
        Assert.assertEquals("1", iterator.next());
        Assert.assertEquals("2", iterator.next());
    }

    @Test
    public void testSimpleOverflow() {
        final Iterator<Integer> iterator;
        final LossyRingBuffer<Integer> ringBuffer = new LossyRingBuffer<Integer>(2);

        for (int i = 0; i < 12; i++) {
            ringBuffer.add(i);
        }
        iterator = ringBuffer.iterator();
        Assert.assertEquals(10, (int)iterator.next());
        Assert.assertEquals(11, (int)iterator.next());
    }
}
