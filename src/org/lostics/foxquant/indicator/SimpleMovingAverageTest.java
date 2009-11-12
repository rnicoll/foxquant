// $Id: SimpleMovingAverageTest.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.indicator;

import java.sql.Timestamp;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.model.PeriodicData;
import org.lostics.foxquant.util.TestUtils;

public class SimpleMovingAverageTest extends Object {
    @Test
    public void testSMAThreePeriod()
        throws InsufficientDataException {
        final Date now = new Date();
        final Timestamp[] timeSeries;
        final SimpleMovingAverage sma = new SimpleMovingAverage(3);

        timeSeries = TestUtils.generateTimeSeries(3, now, ConnectionManager.REAL_TIME_BAR_PERIOD_SECONDS * 1000);

        sma.handlePeriodicData(new PeriodicData(timeSeries[0], 1));
        sma.handlePeriodicData(new PeriodicData(timeSeries[1], 3));
        sma.handlePeriodicData(new PeriodicData(timeSeries[2], 5));
        Assert.assertEquals(3.0, sma.getSMA(), 0);
    }

    @Test(expected= InsufficientDataException.class)
    public void testSMAInsufficientData()
        throws InsufficientDataException {
        final Date now = new Date();
        final Timestamp[] timeSeries;
        final SimpleMovingAverage sma = new SimpleMovingAverage(4);

        timeSeries = TestUtils.generateTimeSeries(3, now, ConnectionManager.REAL_TIME_BAR_PERIOD_SECONDS * 1000);

        sma.handlePeriodicData(new PeriodicData(timeSeries[0], 1));
        sma.handlePeriodicData(new PeriodicData(timeSeries[1], 3));
        sma.handlePeriodicData(new PeriodicData(timeSeries[2], 5));
        sma.getSMA();
    }
}
