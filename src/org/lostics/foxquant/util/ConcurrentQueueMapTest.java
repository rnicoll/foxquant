// $Id: ConcurrentQueueMapTest.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.util;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

public class ConcurrentQueueMapTest extends Object {
    @Test
    public void testSingleQueueSingleEntry() {
        final ConcurrentQueueMap<String, Integer>
            map = new ConcurrentQueueMap<String, Integer>();

        map.offer("Test", Integer.valueOf(1));
        Assert.assertEquals(Integer.valueOf(1), map.peek("Test"));
        Assert.assertEquals(Integer.valueOf(1), map.poll("Test"));
        Assert.assertEquals(null, map.poll("Test"));

        Assert.assertEquals(null, map.peek("Test 2"));
        Assert.assertEquals(null, map.poll("Test 2"));
    }
}
