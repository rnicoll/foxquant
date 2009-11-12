// $Id: MomentumTest.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.indicator;

import java.sql.Timestamp;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.model.PeriodicData;
import org.lostics.foxquant.util.TestUtils;

public class MomentumTest extends Object {
    @Test
    public void testStdDevThreePeriod()
        throws InsufficientDataException {
        final Date now = new Date();
        final Timestamp[] timeSeries;
        final Momentum momentum = new Momentum(3);

        timeSeries = TestUtils.generateTimeSeries(5, now, ConnectionManager.REAL_TIME_BAR_PERIOD_SECONDS * 1000);

        momentum.handlePeriodicData(new PeriodicData(timeSeries[0], 1));
        momentum.handlePeriodicData(new PeriodicData(timeSeries[1], 3));
        momentum.handlePeriodicData(new PeriodicData(timeSeries[2], 4));
        Assert.assertEquals(3.0, momentum.getMomentum(0), 0);
        momentum.handlePeriodicData(new PeriodicData(timeSeries[3], 5));
        Assert.assertEquals(2.0, momentum.getMomentum(0), 0);
        momentum.handlePeriodicData(new PeriodicData(timeSeries[4], 2));
        Assert.assertEquals(-2.0, momentum.getMomentum(0), 0);
    }
}
