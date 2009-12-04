package org.lostics.foxquant.strategy;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.ib.client.ContractDetails;

import org.lostics.foxquant.backtest.UnitTestContractManager;
import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.indicator.InsufficientDataException;
import org.lostics.foxquant.model.ContractManagerTest;
import org.lostics.foxquant.model.EntryOrder;
import org.lostics.foxquant.model.OrderAction;
import org.lostics.foxquant.model.PeriodicData;
import org.lostics.foxquant.model.StrategyAlreadyExistsException;
import org.lostics.foxquant.model.StrategyException;
import org.lostics.foxquant.util.TestUtils;

public class CatchingDaggersTest extends Object {
    @Test(expected=StrategyAlreadyExistsException.class)
    public void testDoubleContract()
        throws InsufficientDataException, StrategyAlreadyExistsException, StrategyException {
        final CatchingDaggersFactory strategyFactory = new CatchingDaggersFactory(3, 0.5);
        final Date now = new Date();
        final Timestamp[] timeSeries = TestUtils.generateTimeSeries(3, now, 60000);
        final List<PeriodicData> testData = new ArrayList<PeriodicData>();

        testData.add(new PeriodicData(timeSeries[0], 10010));
        testData.add(new PeriodicData(timeSeries[1], 10009));
        testData.add(new PeriodicData(timeSeries[2], 10006));

        new UnitTestContractManager(strategyFactory, testData, 60);
        new UnitTestContractManager(strategyFactory, testData, 60);
    }
    
    @Test
    public void testProfitTooSmall()
        throws InsufficientDataException, StrategyAlreadyExistsException, StrategyException {
        final UnitTestContractManager contractManager;
        final CatchingDaggersFactory strategyFactory = new CatchingDaggersFactory(3, 0.5);
        final Date now = new Date();
        final Timestamp[] timeSeries = TestUtils.generateTimeSeries(3, now, 60000);
        final List<PeriodicData> testData = new ArrayList<PeriodicData>();

        testData.add(new PeriodicData(timeSeries[0], 10010));
        testData.add(new PeriodicData(timeSeries[1], 10009));
        testData.add(new PeriodicData(timeSeries[2], 10006));

        contractManager = new UnitTestContractManager(strategyFactory, testData, 60);
        contractManager.run();

        /* A profit of 3 pips (0.03%) should be far too small for the strategy
         * to try trading.
         */
        Assert.assertNull(contractManager.getOrdersFromFlat());
    }
    
    @Test
    public void testTooFarToTransmit()
        throws InsufficientDataException, StrategyAlreadyExistsException, StrategyException {
        final UnitTestContractManager contractManager;
        final CatchingDaggersFactory strategyFactory = new CatchingDaggersFactory(4, 0.5);
        final Date now = new Date();
        final Timestamp[] timeSeries = TestUtils.generateTimeSeries(5, now, 60000);
        final List<PeriodicData> testData = new ArrayList<PeriodicData>();

        // We need 4 data points as history, plus one "current" datapoint
        testData.add(new PeriodicData(timeSeries[0], 10000));
        testData.add(new PeriodicData(timeSeries[1], 9950));
        testData.add(new PeriodicData(timeSeries[2], 10050));
        testData.add(new PeriodicData(timeSeries[3], 10000));
        testData.add(new PeriodicData(timeSeries[4], 10016));

        contractManager = new UnitTestContractManager(strategyFactory, testData, 60);
        contractManager.run();

        /* At 6 pips from the trade entry it should be just enough to trigger an
         * order without being enough to transmit it.
         */
        final EntryOrder entryOrder = contractManager.getOrdersFromFlat();
        Assert.assertNotNull(entryOrder);

        contractManager.close();
        
        Assert.assertEquals(OrderAction.SELL, entryOrder.getOrderAction());
        Assert.assertEquals(10032, entryOrder.getEntryLimitPrice());
        Assert.assertEquals(false, entryOrder.shouldTransmit());
    }
    
    @Test
    public void testJustInsideTransmitDistance()
        throws InsufficientDataException, StrategyAlreadyExistsException, StrategyException {
        final UnitTestContractManager contractManager;
        final CatchingDaggersFactory strategyFactory = new CatchingDaggersFactory(4, 0.5);
        final Date now = new Date();
        final Timestamp[] timeSeries = TestUtils.generateTimeSeries(5, now, 60000);
        final List<PeriodicData> testData = new ArrayList<PeriodicData>();

        // We need 4 data points as history, plus one "current" datapoint
        testData.add(new PeriodicData(timeSeries[0], 10000));
        testData.add(new PeriodicData(timeSeries[1], 9950));
        testData.add(new PeriodicData(timeSeries[2], 10050));
        testData.add(new PeriodicData(timeSeries[3], 10000));
        testData.add(new PeriodicData(timeSeries[4], 10020));

        contractManager = new UnitTestContractManager(strategyFactory, testData, 60);
        contractManager.run();

        /* At 4 pips from the trade entry it should be just enough to set the
         * order as ready to transmit.
         */
        final EntryOrder entryOrder = contractManager.getOrdersFromFlat();
        Assert.assertNotNull(entryOrder);

        contractManager.close();
        
        Assert.assertEquals(OrderAction.SELL, entryOrder.getOrderAction());
        Assert.assertEquals(10040, entryOrder.getEntryLimitPrice());
        Assert.assertEquals(true, entryOrder.shouldTransmit());
    }
    
    @Test
    public void testThreePeriodFlat()
        throws InsufficientDataException, StrategyAlreadyExistsException, StrategyException {
        final UnitTestContractManager contractManager;
        final CatchingDaggersFactory strategyFactory = new CatchingDaggersFactory(3, 0.5);
        final Date now = new Date();
        final Timestamp[] timeSeries = TestUtils.generateTimeSeries(3, now, 60000);
        final List<PeriodicData> testData = new ArrayList<PeriodicData>();

        testData.add(new PeriodicData(timeSeries[0], 110));
        testData.add(new PeriodicData(timeSeries[1], 90));
        testData.add(new PeriodicData(timeSeries[2], 100));

        contractManager = new UnitTestContractManager(strategyFactory, testData, 60);
        contractManager.run();

        /* As the current price should be the exact mid-price of the data,
         * the strategy should see it as too far from an entry price, and
         * remain flat in the market.
         */
        Assert.assertNull(contractManager.getOrdersFromFlat());

        contractManager.close();
    }
    
    @Test
    public void testThreePeriodLong()
        throws InsufficientDataException, StrategyAlreadyExistsException, StrategyException {
        final UnitTestContractManager contractManager;
        final CatchingDaggersFactory strategyFactory = new CatchingDaggersFactory(3, 0.5);
        final Date now = new Date();
        final Timestamp[] timeSeries = TestUtils.generateTimeSeries(3, now, 60000);
        final List<PeriodicData> testData = new ArrayList<PeriodicData>();

        testData.add(new PeriodicData(timeSeries[0], 10050));
        testData.add(new PeriodicData(timeSeries[1], 10000));
        testData.add(new PeriodicData(timeSeries[2], 9950));

        contractManager = new UnitTestContractManager(strategyFactory, testData, 60);
        contractManager.run();

        /* The price drops fast in the third data point, so we should have an
         * order to go long.
         */
        final EntryOrder entryOrder = contractManager.getOrdersFromFlat();
        Assert.assertNotNull(entryOrder);

        contractManager.close();

        Assert.assertEquals(OrderAction.BUY, entryOrder.getOrderAction());
        // 50 below the bid price because the price appears to the strategy to have dropped 50 points in the last second
        Assert.assertEquals(9900, entryOrder.getEntryLimitPrice());
        // XXX: Need to manually check this number
        Assert.assertEquals(9966, entryOrder.getExitLimitPrice());
        Assert.assertEquals(9834, entryOrder.getExitStopPrice());
    }
    
    @Test
    public void testFourPeriodLong()
        throws InsufficientDataException, StrategyAlreadyExistsException, StrategyException {
        final UnitTestContractManager contractManager;
        final CatchingDaggersFactory strategyFactory = new CatchingDaggersFactory(3, 0.5);
        final Date now = new Date();
        final Timestamp[] timeSeries = TestUtils.generateTimeSeries(4, now, 60000);
        final List<PeriodicData> testData = new ArrayList<PeriodicData>();

        testData.add(new PeriodicData(timeSeries[0], 10050));
        testData.add(new PeriodicData(timeSeries[1], 10000));
        testData.add(new PeriodicData(timeSeries[2], 9950));
        testData.add(new PeriodicData(timeSeries[3], 9950));

        contractManager = new UnitTestContractManager(strategyFactory, testData, 60);
        contractManager.run();

        /* The price drops fast in the third data point, so we should have an
         * order to go long.
         */
        final EntryOrder entryOrder = contractManager.getOrdersFromFlat();
        Assert.assertNotNull(entryOrder);

        contractManager.close();

        Assert.assertEquals(OrderAction.BUY, entryOrder.getOrderAction());
        Assert.assertEquals(9950, entryOrder.getEntryLimitPrice());
        // XXX: Need to manually check this number
        Assert.assertEquals(9962, entryOrder.getExitLimitPrice());
        Assert.assertEquals(9938, entryOrder.getExitStopPrice());
    }
    
    /**
     * Tests two different strategies trying to go long on the same currency,
     * at the same time.
     */
    @Test
    public void testDualThreePeriodLong()
        throws InsufficientDataException, StrategyAlreadyExistsException, StrategyException {
        final ContractDetails contractDetailsGBPUSD = UnitTestContractManager.generateTestContractDetails("GBP", "USD");
        final ContractDetails contractDetailsEURUSD = UnitTestContractManager.generateTestContractDetails("EUR", "USD");
        final UnitTestContractManager contractManagerGBPUSD;
        final UnitTestContractManager contractManagerEURUSD;
        final CatchingDaggersFactory strategyFactory = new CatchingDaggersFactory(3, 0.5);
        final Date now = new Date();
        final Timestamp[] timeSeries = TestUtils.generateTimeSeries(3, now, 60000);
        final List<PeriodicData> testData = new ArrayList<PeriodicData>();

        testData.add(new PeriodicData(timeSeries[0], 10030));
        testData.add(new PeriodicData(timeSeries[1], 10015));
        testData.add(new PeriodicData(timeSeries[2], 9990));

        contractManagerGBPUSD = new UnitTestContractManager(contractDetailsGBPUSD, strategyFactory, testData, 60);
        contractManagerEURUSD = new UnitTestContractManager(contractDetailsEURUSD, strategyFactory, testData, 60);
        contractManagerGBPUSD.run();

        /* The price drops fast in the third data point, so we should have an
         * order to go long.
         */
        EntryOrder entryOrder = contractManagerGBPUSD.getOrdersFromFlat();
        Assert.assertNotNull(entryOrder);
        Assert.assertEquals(OrderAction.BUY, entryOrder.getOrderAction());
        Assert.assertTrue(entryOrder.shouldTransmit());

        contractManagerEURUSD.run();
        entryOrder = contractManagerEURUSD.getOrdersFromFlat();
        Assert.assertFalse(entryOrder.shouldTransmit());

        contractManagerGBPUSD.close();
        contractManagerEURUSD.close();

    }
    
    @Test
    public void testThreePeriodShort()
        throws InsufficientDataException, StrategyAlreadyExistsException, StrategyException {
        final UnitTestContractManager contractManager;
        final CatchingDaggersFactory strategyFactory = new CatchingDaggersFactory(3, 0.5);
        final Date now = new Date();
        final Timestamp[] timeSeries = TestUtils.generateTimeSeries(3, now, 60000);
        final List<PeriodicData> testData = new ArrayList<PeriodicData>();

        testData.add(new PeriodicData(timeSeries[0], 10090));
        testData.add(new PeriodicData(timeSeries[1], 10070));
        testData.add(new PeriodicData(timeSeries[2], 10120));

        contractManager = new UnitTestContractManager(strategyFactory, testData, 60);
        contractManager.run();

        /* The price rises fast in the third data point, so we should have an
         * order to go short.
         */
        final EntryOrder entryOrder = contractManager.getOrdersFromFlat();
        Assert.assertNotNull(entryOrder);

        contractManager.close();

        Assert.assertEquals(OrderAction.SELL, entryOrder.getOrderAction());
        Assert.assertEquals(10170, entryOrder.getEntryLimitPrice());
        Assert.assertEquals(10119, entryOrder.getExitLimitPrice());
        Assert.assertEquals(10221, entryOrder.getExitStopPrice());
    }
}
