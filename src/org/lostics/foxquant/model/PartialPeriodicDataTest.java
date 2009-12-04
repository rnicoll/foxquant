// $Id$
package org.lostics.foxquant.model;

import java.sql.Timestamp;

import org.junit.Assert;
import org.junit.Test;

public class PartialPeriodicDataTest {
    @Test
    public void testSingleBar() {
        final Timestamp startTime = new Timestamp(System.currentTimeMillis());
        final PartialPeriodicData periodicData = new PartialPeriodicData(startTime, 1000);
        
        periodicData.update(1050);
        Assert.assertEquals(1050, periodicData.close);
        Assert.assertEquals(1050, periodicData.high);
        Assert.assertEquals(1000, periodicData.open);
        Assert.assertEquals(1000, periodicData.low);
    }
}
