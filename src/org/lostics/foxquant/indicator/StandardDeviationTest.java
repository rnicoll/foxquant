// $Id: StandardDeviationTest.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.indicator;

import java.sql.Timestamp;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.model.PeriodicData;
import org.lostics.foxquant.util.TestUtils;

public class StandardDeviationTest extends Object {
    @Test
    public void testStdDevFivePeriod()
        throws InsufficientDataException {
        final Date now = new Date();
        final Timestamp[] timeSeries;
        final StandardDeviation stdDev = new StandardDeviation(5);

        timeSeries = TestUtils.generateTimeSeries(5, now, ConnectionManager.REAL_TIME_BAR_PERIOD_SECONDS * 1000);

        stdDev.handlePeriodicData(new PeriodicData(timeSeries[0], 1));
        stdDev.handlePeriodicData(new PeriodicData(timeSeries[1], 3));
        stdDev.handlePeriodicData(new PeriodicData(timeSeries[2], 4));
        stdDev.handlePeriodicData(new PeriodicData(timeSeries[3], 5));
        stdDev.handlePeriodicData(new PeriodicData(timeSeries[4], 7));
        Assert.assertEquals(stdDev.getStandardDeviation(0), stdDev.getStandardDeviation(), 0);
        Assert.assertEquals(2.0, stdDev.getStandardDeviation(0), 0);
        Assert.assertEquals(4.0, stdDev.getSMA(), 0);
    }

    @Test(expected= InsufficientDataException.class)
    public void testStdDevInsufficientData()
        throws InsufficientDataException {
        final Date now = new Date();
        final Timestamp[] timeSeries;
        final StandardDeviation stdDev = new StandardDeviation(4);

        timeSeries = TestUtils.generateTimeSeries(3, now, ConnectionManager.REAL_TIME_BAR_PERIOD_SECONDS * 1000);

        stdDev.handlePeriodicData(new PeriodicData(timeSeries[0], 1));
        stdDev.handlePeriodicData(new PeriodicData(timeSeries[1], 3));
        stdDev.handlePeriodicData(new PeriodicData(timeSeries[2], 4));
        stdDev.getStandardDeviation(0);
    }
}
