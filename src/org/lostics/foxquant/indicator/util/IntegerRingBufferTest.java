// $Id: IntegerRingBufferTest.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.indicator.util;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

public class IntegerRingBufferTest extends Object {
    @Test
    public void testFirstLap() {
        final Iterator<String> iterator;
        final IntegerRingBuffer ringBuffer = new IntegerRingBuffer(3);

        ringBuffer.add(1);
        Assert.assertEquals(1, ringBuffer.get(0));

        ringBuffer.add(2);
        Assert.assertEquals(2, ringBuffer.get(0));

        ringBuffer.add(3);
        Assert.assertEquals(3, ringBuffer.get(0));

        Assert.assertEquals(1, ringBuffer.get(2));
    }

    @Test
    public void testMean() {
        final Iterator<String> iterator;
        final IntegerRingBuffer ringBuffer = new IntegerRingBuffer(2);

        ringBuffer.add(1);
        Assert.assertEquals(1.0, ringBuffer.getMean(), 0.01);
        ringBuffer.add(2);
        Assert.assertEquals(1.5, ringBuffer.getMean(), 0.01);
        ringBuffer.add(4);
        Assert.assertEquals(3.0, ringBuffer.getMean(), 0.01);

    }

    @Test
    public void testOverwrite() {
        final Iterator<String> iterator;
        final IntegerRingBuffer ringBuffer = new IntegerRingBuffer(3);

        ringBuffer.add(1);
        ringBuffer.add(2);
        ringBuffer.add(3);
        Assert.assertEquals(1, ringBuffer.get(2));

        ringBuffer.add(4);
        Assert.assertEquals(2, ringBuffer.get(2));
        Assert.assertEquals(3, ringBuffer.get(1));
        Assert.assertEquals(4, ringBuffer.get(0));

        ringBuffer.add(5);
        ringBuffer.add(6);
        ringBuffer.add(7);

        Assert.assertEquals(5, ringBuffer.get(2));
        Assert.assertEquals(6, ringBuffer.get(1));
        Assert.assertEquals(7, ringBuffer.get(0));
    }

    @Test
    public void testSum() {
        final Iterator<String> iterator;
        final IntegerRingBuffer ringBuffer = new IntegerRingBuffer(2);

        ringBuffer.add(1);
        Assert.assertEquals(1, ringBuffer.getSum());
        ringBuffer.add(2);
        Assert.assertEquals(3, ringBuffer.getSum());
        ringBuffer.add(3);
        Assert.assertEquals(5, ringBuffer.getSum());

    }
}
