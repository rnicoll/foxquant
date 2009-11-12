// $Id: PeriodicDataTest.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.model;

import java.sql.Timestamp;

import org.junit.Assert;
import org.junit.Test;

import org.lostics.foxquant.util.TestUtils;

public class PeriodicDataTest extends Object {
    private static final int OPEN = 2;
    private static final int HIGH = 4;
    private static final int LOW = 1;
    private static final int CLOSE = 3;

    @Test
    public void testPeriodicData() {
        final PeriodicData data;

        data = new PeriodicData(new Timestamp(System.currentTimeMillis()), OPEN, HIGH, LOW, CLOSE);

        Assert.assertEquals(OPEN, data.open, 0);
        Assert.assertEquals(HIGH, data.high, 0);
        Assert.assertEquals(LOW, data.low, 0);
        Assert.assertEquals(CLOSE, data.close, 0);

        Assert.assertEquals(OPEN, data.getPrice(PriceType.OPEN), 0);
        Assert.assertEquals(HIGH, data.getPrice(PriceType.HIGH), 0);
        Assert.assertEquals(LOW, data.getPrice(PriceType.LOW), 0);
        Assert.assertEquals(CLOSE, data.getPrice(PriceType.CLOSE), 0);

        Assert.assertEquals((HIGH + LOW) / 2, data.getPrice(PriceType.HIGH_LOW_MEAN), 0);
    }
}
