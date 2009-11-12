// $Id: PeriodicDataBufferTest.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.model;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import org.lostics.foxquant.util.TestUtils;

public class PeriodicDataBufferTest extends Object {
    @Test
    public void testFirstLap() {
        final Iterator<PeriodicData> iterator;
        final PeriodicDataBuffer buffer;
        final Date now = new Date();
        final Timestamp[] timeSeries;

        buffer = new PeriodicDataBuffer(2, now, 60000);
        timeSeries = TestUtils.generateTimeSeries(2, now, 60000);

        buffer.handlePeriodicData(new PeriodicData(timeSeries[0], 1));
        buffer.handlePeriodicData(new PeriodicData(timeSeries[1], 2));

        iterator = buffer.iterator();
        Assert.assertEquals(1.0, iterator.next().high, 0);
        Assert.assertEquals(now, iterator.next().startTime);
    }

    @Test
    public void testNonContinuousFill() {
        final Iterator<PeriodicData> iterator;
        final PeriodicDataBuffer buffer;
        final Date now = new Date();
        final Timestamp[] timeSeries;

        buffer = new PeriodicDataBuffer(3, now, 60000);
        timeSeries = TestUtils.generateTimeSeries(3, now, 60000);

        buffer.handlePeriodicData(new PeriodicData(timeSeries[2], 2));
        buffer.handlePeriodicData(new PeriodicData(timeSeries[0], 5));

        iterator = buffer.iterator();
        Assert.assertEquals(5.0, iterator.next().high, 0);
        Assert.assertEquals(now, iterator.next().startTime);
        Assert.assertFalse(iterator.hasNext());

        Assert.assertEquals(timeSeries[1], buffer.getFirstGap());
    }
}
