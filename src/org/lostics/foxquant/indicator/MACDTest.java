// $Id: MACDTest.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.indicator;

import java.sql.Timestamp;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.model.PeriodicData;
import org.lostics.foxquant.util.TestUtils;

public class MACDTest extends Object {
    private int[] TEST_VALUES_FAST = {
        54861,
        54545,
        53202,
        53547,
        54119,
        54207,
        53192,
        53126,
        53526,
        53623,
        54499,
        53641
    };

    private int[] TEST_VALUES_DIFFERENCE = {
        54861,
        54545,
        53202,
        53547,
        54119,
        54207,
        53192,
        53126,
        53526,
        53623,
        54499,
        53641,
        54043,
        53764,
        52863,
        51506,
        51719,
        53004,
        52616,
        54068,
        55296,
        54405,
        55127,
        54128,
        54766,
        54263
    };

    private int[] TEST_VALUES_SIGNAL = {
        54861,
        54545,
        53202,
        53547,
        54119,
        54207,
        53192,
        53126,
        53526,
        53623,
        54499,
        53641,
        54043,
        53764,
        52863,
        51506,
        51719,
        53004,
        52616,
        54068,
        55296,
        54405,
        55127,
        54128,
        54766,
        54263,
        54799,
        56259,
        55299,
        55182,
        56661,
        56347,
        56672,
        56208,
        57084
    };

    @Test
    public void testMACDFast()
        throws InsufficientDataException {
        final Date now = new Date();
        final Timestamp[] timeSeries;
        final MACD macd = new MACD();

        timeSeries = TestUtils.generateTimeSeries(TEST_VALUES_FAST.length + 1,
            now, ConnectionManager.REAL_TIME_BAR_PERIOD_SECONDS * 1000);

        for (int i = 0; i < TEST_VALUES_FAST.length; i++) {
            macd.handlePeriodicData(new PeriodicData(timeSeries[i], TEST_VALUES_FAST[i]));
        }

        Assert.assertEquals(53840.66667, macd.getFast(), 0.00001);

        macd.handlePeriodicData(new PeriodicData(timeSeries[TEST_VALUES_FAST.length], 54043));
        Assert.assertEquals(53871.79487, macd.getFast(), 0.00001);
    }

    @Test
    public void testMACDDifference()
        throws InsufficientDataException {
        final Date now = new Date();
        final Timestamp[] timeSeries;
        final MACD macd = new MACD();

        timeSeries = TestUtils.generateTimeSeries(TEST_VALUES_DIFFERENCE.length,
            now, ConnectionManager.REAL_TIME_BAR_PERIOD_SECONDS * 1000);

        for (int i = 0; i < TEST_VALUES_DIFFERENCE.length; i++) {
            macd.handlePeriodicData(new PeriodicData(timeSeries[i], TEST_VALUES_DIFFERENCE[i]));
        }

        Assert.assertEquals(54077.61979, macd.getFast(), 0.00001);
        Assert.assertEquals(53756.0, macd.getSlow(), 0.0001);
        Assert.assertEquals(321.619793, macd.getDifference(), 0.000001);
    }

    @Test
    public void testMACDSignal()
        throws InsufficientDataException {
        final Date now = new Date();
        final Timestamp[] timeSeries;
        final MACD macd = new MACD();

        timeSeries = TestUtils.generateTimeSeries(TEST_VALUES_SIGNAL.length,
            now, ConnectionManager.REAL_TIME_BAR_PERIOD_SECONDS * 1000);

        for (int i = 0; i < TEST_VALUES_SIGNAL.length; i++) {
            macd.handlePeriodicData(new PeriodicData(timeSeries[i], TEST_VALUES_SIGNAL[i]));
        }

        Assert.assertEquals(55794.54238, macd.getFast(), 0.00001);
        Assert.assertEquals(54960.24383, macd.getSlow(), 0.00001);
        Assert.assertEquals(834.2985451, macd.getDifference(), 0.0000001);
        Assert.assertEquals(619.9525911, macd.getSignal(), 0.0000001);
    }
}
