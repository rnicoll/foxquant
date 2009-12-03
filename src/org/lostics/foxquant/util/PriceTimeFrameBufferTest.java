// $Id$
package org.lostics.foxquant.util;

import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

public class PriceTimeFrameBufferTest extends Object {
    @Test(expected= IllegalStateException.class)
    public void testEmpty() {
        final long baseTime = System.currentTimeMillis();
        final PriceTimeFrameBuffer buffer = new PriceTimeFrameBuffer(1000, 5);
        
        buffer.getHigh();
    }
    
    @Test
    public void testSingleDatapoint() {
        final long baseTime = System.currentTimeMillis();
        final PriceTimeFrameBuffer buffer = new PriceTimeFrameBuffer(1000, 5);
        
        buffer.add(baseTime, 5);
        Assert.assertEquals(5, buffer.getHigh());
        Assert.assertEquals(5, buffer.getLow());
    }
    
    @Test
    public void testTwoDatapoint() {
        final long baseTime = System.currentTimeMillis();
        final PriceTimeFrameBuffer buffer = new PriceTimeFrameBuffer(1000, 5);
        
        buffer.add(baseTime, 6);
        buffer.add(baseTime + 5, 4);
        Assert.assertEquals(5, buffer.getHigh());
        Assert.assertEquals(5, buffer.getLow());
    }
    
    @Test
    public void testFourDatapoint() {
        final long baseTime = System.currentTimeMillis();
        final PriceTimeFrameBuffer buffer = new PriceTimeFrameBuffer(1000, 5);
        
        buffer.add(baseTime, 6);
        buffer.add(baseTime + 5, 4);
        Assert.assertEquals(5, buffer.getHigh());
        Assert.assertEquals(5, buffer.getLow());
        buffer.add(baseTime + 1005, 6);
        Assert.assertEquals(6, buffer.getHigh());
        Assert.assertEquals(5, buffer.getLow());
        buffer.add(baseTime + 2005, 4);
        Assert.assertEquals(6, buffer.getHigh());
        Assert.assertEquals(4, buffer.getLow());
    }
}
