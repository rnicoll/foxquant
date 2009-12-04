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
    
    @Test
    public void testTwoBar() {
        final Timestamp startTime = new Timestamp(System.currentTimeMillis());
        final Timestamp startTime2ndBar = new Timestamp(startTime.getTime() + 60000);
        final PartialPeriodicData periodicData = new PartialPeriodicData(startTime, 1000);
        
        periodicData.update(1050);
        Assert.assertEquals(1050, periodicData.close);
        Assert.assertEquals(1050, periodicData.high);
        Assert.assertEquals(1000, periodicData.open);
        Assert.assertEquals(1000, periodicData.low);
        periodicData.startNewBar(startTime2ndBar, 1060);
        Assert.assertEquals(1060, periodicData.close);
        Assert.assertEquals(1060, periodicData.high);
        Assert.assertEquals(1060, periodicData.open);
        Assert.assertEquals(1060, periodicData.low);
        periodicData.update(1050);
        Assert.assertEquals(1050, periodicData.close);
        Assert.assertEquals(1060, periodicData.high);
        Assert.assertEquals(1060, periodicData.open);
        Assert.assertEquals(1050, periodicData.low);
        
        final PeriodicData bar = periodicData.getCopy();
        Assert.assertEquals(1050, bar.close);
        Assert.assertEquals(1060, bar.high);
        Assert.assertEquals(1060, bar.open);
        Assert.assertEquals(1050, bar.low);
        Assert.assertEquals(startTime2ndBar, bar.startTime);
    }
}
