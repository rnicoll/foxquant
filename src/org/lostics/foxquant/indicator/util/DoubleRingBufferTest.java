// $Id: DoubleRingBufferTest.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.indicator.util;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

public class DoubleRingBufferTest extends Object {
    @Test
    public void testFirstLap() {
        final Iterator<String> iterator;
        final DoubleRingBuffer ringBuffer = new DoubleRingBuffer(3);

        ringBuffer.add(1.0);
        Assert.assertEquals(1.0, ringBuffer.get(0), 0.0);

        ringBuffer.add(2.0);
        Assert.assertEquals(2.0, ringBuffer.get(0), 0.0);

        ringBuffer.add(3.0);
        Assert.assertEquals(3.0, ringBuffer.get(0), 0.0);

        Assert.assertEquals(1.0, ringBuffer.get(2), 0.0);
    }

    @Test
    public void testMean() {
        final Iterator<String> iterator;
        final DoubleRingBuffer ringBuffer = new DoubleRingBuffer(2);

        ringBuffer.add(1.0);
        Assert.assertEquals(1.0, ringBuffer.getMean(), 0.0);
        ringBuffer.add(2.0);
        Assert.assertEquals(1.5, ringBuffer.getMean(), 0.0);
        ringBuffer.add(4.0);
        Assert.assertEquals(3.0, ringBuffer.getMean(), 0.0);

    }

    @Test
    public void testOverwrite() {
        final Iterator<String> iterator;
        final DoubleRingBuffer ringBuffer = new DoubleRingBuffer(3);

        ringBuffer.add(1.0);
        ringBuffer.add(2.0);
        ringBuffer.add(3.0);
        Assert.assertEquals(1.0, ringBuffer.get(2), 0.0);

        ringBuffer.add(4.0);
        Assert.assertEquals(2.0, ringBuffer.get(2), 0.0);
        Assert.assertEquals(3.0, ringBuffer.get(1), 0.0);
        Assert.assertEquals(4.0, ringBuffer.get(0), 0.0);

        ringBuffer.add(5.0);
        ringBuffer.add(6.0);
        ringBuffer.add(7.0);

        Assert.assertEquals(5.0, ringBuffer.get(2), 0.0);
        Assert.assertEquals(6.0, ringBuffer.get(1), 0.0);
        Assert.assertEquals(7.0, ringBuffer.get(0), 0.0);
    }

    @Test
    public void testSum() {
        final Iterator<String> iterator;
        final DoubleRingBuffer ringBuffer = new DoubleRingBuffer(2);

        ringBuffer.add(1.0);
        Assert.assertEquals(1.0, ringBuffer.getSum(), 0.0);
        ringBuffer.add(2.0);
        Assert.assertEquals(3.0, ringBuffer.getSum(), 0.0);
        ringBuffer.add(3.0);
        Assert.assertEquals(5.0, ringBuffer.getSum(), 0.0);

    }
}
