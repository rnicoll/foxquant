// $Id: TestUtils.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.util;

import java.sql.Timestamp;
import java.util.Date;

public final class TestUtils extends Object {
    private         TestUtils() {
    }

    public static Timestamp[] generateTimeSeries(final int seriesLength, final Date lastBarStartTime, final long periodMillis) {
        final Timestamp[] timeSeries = new Timestamp[seriesLength];
        final long seriesPeriodMillis = (seriesLength - 1) * periodMillis;
        long time = lastBarStartTime.getTime() - seriesPeriodMillis;

        for (int seriesIdx = 0; seriesIdx < seriesLength; seriesIdx++) {
            timeSeries[seriesIdx] = new Timestamp(time);
            time += periodMillis;
        }

        return timeSeries;
    }
}
