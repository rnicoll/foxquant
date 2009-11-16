// $Id: ContractManagerTest.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.model;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.ib.client.ContractDetails;

import org.junit.Assert;
import org.junit.Test;

import org.lostics.foxquant.backtest.UnitTestContractManager;
import org.lostics.foxquant.model.StrategyAlreadyExistsException;

public class ContractManagerTest extends Object {
    @Test
    public void testMarketCloseDuration()
        throws StrategyAlreadyExistsException, ParseException {
        final ContractManager contractManager = new UnitTestContractManager(
            new ContractDetails());

        final DateFormat dateFormat = new SimpleDateFormat("HH:mm yyyy-MM-dd zzzzzzzzz");
        final Date dayAfterMarketClose = dateFormat.parse("16:00 2008-06-28 GMT-05:00");
        final Date twoDaysAfterMarketClose = dateFormat.parse("16:00 2008-06-29 GMT-05:00");
        final Date fiveMinutesAfterMarketClose = dateFormat.parse("16:05 2008-06-26 GMT-05:00");

        Assert.assertEquals(300000L, contractManager.getMarketCloseDuration(fiveMinutesAfterMarketClose));
        Assert.assertEquals(86400000L, contractManager.getMarketCloseDuration(dayAfterMarketClose));
        Assert.assertEquals(172800000L, contractManager.getMarketCloseDuration(twoDaysAfterMarketClose));
    }

    @Test
    public void testMarketOpenDuration()
        throws StrategyAlreadyExistsException, ParseException {
        final ContractManager contractManager = new UnitTestContractManager(
            new ContractDetails());

        final DateFormat dateFormat = new SimpleDateFormat("HH:mm yyyy-MM-dd zzzzzzzzz");
        final Date minuteAfterMarketOpen = dateFormat.parse("16:16 2008-06-29 GMT-05:00");
        final Date hourAfterMarketOpen = dateFormat.parse("17:15 2008-06-29 GMT-05:00");

        Assert.assertEquals(60000L, contractManager.getMarketOpenDuration(minuteAfterMarketOpen));
        Assert.assertEquals(3600000L, contractManager.getMarketOpenDuration(hourAfterMarketOpen));
    }

    @Test
    public void testMarketCloseTime()
        throws StrategyAlreadyExistsException, ParseException {
        final ContractManager contractManager = new UnitTestContractManager(
            new ContractDetails());

        final DateFormat dateFormat = new SimpleDateFormat("HH:mm yyyy-MM-dd zzzzzzzzz");
        final Date minuteAfterMarketOpen = dateFormat.parse("16:16 2008-06-29 GMT-05:00");
        final Date halfDayAfterMarketOpen = dateFormat.parse("04:15 2008-07-01 GMT-05:00");
        final Date mondayMarketClose = dateFormat.parse("16:00 2008-06-30 GMT-05:00");
        final Date tuesdayMarketClose = dateFormat.parse("16:00 2008-07-01 GMT-05:00");

        Assert.assertEquals(mondayMarketClose, contractManager.getMarketCloseTime(minuteAfterMarketOpen));
        Assert.assertEquals(tuesdayMarketClose, contractManager.getMarketCloseTime(halfDayAfterMarketOpen));
    }

    @Test
    public void testMarketOpen()
        throws StrategyAlreadyExistsException, ParseException {
        final ContractManager contractManager = new UnitTestContractManager(
            new ContractDetails());

        final DateFormat dateFormat = new SimpleDateFormat("HH:mm yyyy-MM-dd zzzzzzzzz");
        final Date justAfterMarketClose = dateFormat.parse("16:01 2008-06-27 GMT-05:00");
        final Date justBeforeMarketClose = dateFormat.parse("15:59 2008-06-27 GMT-05:00");
        final Date justAfterMarketOpen = dateFormat.parse("16:16 2008-06-29 GMT-05:00");
        final Date justBeforeMarketOpen = dateFormat.parse("16:14 2008-06-29 GMT-05:00");
        final Date marketClose = dateFormat.parse("16:00 2008-06-27 GMT-05:00");
        final Date marketOpen = dateFormat.parse("16:15 2008-06-29 GMT-05:00");

        Assert.assertFalse(contractManager.isMarketOpen(justBeforeMarketOpen));
        Assert.assertTrue(contractManager.isMarketOpen(marketOpen));
        Assert.assertTrue(contractManager.isMarketOpen(justAfterMarketOpen));

        Assert.assertTrue(contractManager.isMarketOpen(justBeforeMarketClose));
        Assert.assertFalse(contractManager.isMarketOpen(marketClose));
        Assert.assertFalse(contractManager.isMarketOpen(justAfterMarketClose));
    }
}
