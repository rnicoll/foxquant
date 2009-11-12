// $Id: BollingerBandsTest.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.indicator;

import java.sql.Timestamp;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.model.Indicator;
import org.lostics.foxquant.model.PeriodicData;
import org.lostics.foxquant.util.TestUtils;

public class BollingerBandsTest extends Object {
    /**
     * Basic functionality test for Bollinger bands, using a series of 5 datapoints. Generates a
     * SMA of 4, and a standard deviation of 2.
     */
    @Test
    public void testBollingerBandsFivePeriod()
        throws InsufficientDataException {
        final Date now = new Date();
        final Timestamp[] timeSeries;
        final BollingerBands bBand = new BollingerBands(1.0, 5);

        timeSeries = TestUtils.generateTimeSeries(5, now, ConnectionManager.REAL_TIME_BAR_PERIOD_SECONDS * 1000);

        bBand.handlePeriodicData(new PeriodicData(timeSeries[0], 1));
        bBand.handlePeriodicData(new PeriodicData(timeSeries[1], 3));
        bBand.handlePeriodicData(new PeriodicData(timeSeries[2], 4));
        bBand.handlePeriodicData(new PeriodicData(timeSeries[3], 5));
        bBand.handlePeriodicData(new PeriodicData(timeSeries[4], 7));

        Assert.assertEquals((bBand.getUpper() + bBand.getLower()) / 2, bBand.getSMA(), 0);

        Assert.assertEquals(4.0, bBand.getSMA(), 0);
        Assert.assertEquals(2.0, bBand.getLower(), 0);
        Assert.assertEquals(6.0, bBand.getUpper(), 0);
    }

    protected static void setupOverflow(final Indicator testBand)
        throws Exception {
        final Date now = new Date();
        final Timestamp[] timeSeries;

        timeSeries = TestUtils.generateTimeSeries(8, now, ConnectionManager.REAL_TIME_BAR_PERIOD_SECONDS * 1000);

        testBand.handlePeriodicData(new PeriodicData(timeSeries[0], 5000));
        testBand.handlePeriodicData(new PeriodicData(timeSeries[1], 5000));
        testBand.handlePeriodicData(new PeriodicData(timeSeries[2], 5000));

        testBand.handlePeriodicData(new PeriodicData(timeSeries[3], 1));
        testBand.handlePeriodicData(new PeriodicData(timeSeries[4], 3));
        testBand.handlePeriodicData(new PeriodicData(timeSeries[5], 4));
        testBand.handlePeriodicData(new PeriodicData(timeSeries[6], 5));
        testBand.handlePeriodicData(new PeriodicData(timeSeries[7], 7));
    }

    /**
     * Tests that the Bollinger band indicator correctly discards data after the end of the
     * period it covers.
     */
    @Test
    public void testBollingerBandsOverflow()
        throws InsufficientDataException, Exception {
        final BollingerBands bBand = new BollingerBands(0.5, 5);

        setupOverflow(bBand);

        Assert.assertEquals(4.0, bBand.getSMA(), 0);
        Assert.assertEquals(3.0, bBand.getLower(), 0);
        Assert.assertEquals(5.0, bBand.getUpper(), 0);
    }
}
