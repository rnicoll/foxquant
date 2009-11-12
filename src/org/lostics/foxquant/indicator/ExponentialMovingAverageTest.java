// $Id: ExponentialMovingAverageTest.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.indicator;

import java.sql.Timestamp;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.model.PeriodicData;
import org.lostics.foxquant.util.TestUtils;

public class ExponentialMovingAverageTest extends Object {
    private int[] TEST_VALUES = {4150,
        4123,
        4088,
        4094,
        4103,
        4123,
        4100,
        4116,
        4074,
        4134
    };

    @Test
    public void testExponentialMovingAverageTenPeriod()
        throws InsufficientDataException {
        final Date now = new Date();
        final Timestamp[] timeSeries;
        final ExponentialMovingAverage ema = new ExponentialMovingAverage(10);

        timeSeries = TestUtils.generateTimeSeries(12, now, ConnectionManager.REAL_TIME_BAR_PERIOD_SECONDS * 1000);

        for (int i = 0; i < TEST_VALUES.length; i++) {
            ema.handlePeriodicData(new PeriodicData(timeSeries[i], TEST_VALUES[i]));
        }

        Assert.assertEquals(4110.5, ema.getExponentialMovingAverage(), 0.1);
        ema.handlePeriodicData(new PeriodicData(timeSeries[10], 4134));
        Assert.assertEquals(4114.772727, ema.getExponentialMovingAverage(), 0.0001);
        ema.handlePeriodicData(new PeriodicData(timeSeries[11], 4159));
        Assert.assertEquals(4122.81405, ema.getExponentialMovingAverage(), 0.0001);
    }
}
