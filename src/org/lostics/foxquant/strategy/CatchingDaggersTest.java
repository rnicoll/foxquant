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
import org.lostics.foxquant.model.EntryOrder;
import org.lostics.foxquant.model.OrderAction;
import org.lostics.foxquant.model.PeriodicData;
import org.lostics.foxquant.model.StrategyException;
import org.lostics.foxquant.util.TestUtils;

public class CatchingDaggersTest extends Object {
    @Test
    public void testProfitTooSmall()
        throws InsufficientDataException, StrategyException {
        final UnitTestContractManager contractManager;
        final CatchingDaggersFactory strategyFactory = new CatchingDaggersFactory(3, 0.5);
        final Date now = new Date();
        final Timestamp[] timeSeries = TestUtils.generateTimeSeries(3, now, 60000);
        final List<PeriodicData> testData = new ArrayList<PeriodicData>();

        testData.add(new PeriodicData(timeSeries[0], 10010));
        testData.add(new PeriodicData(timeSeries[1], 10009));
        testData.add(new PeriodicData(timeSeries[2], 10006));

        contractManager = new UnitTestContractManager(new ContractDetails(), strategyFactory, testData, 60);
        contractManager.run();

        /* A profit of 3 pips (0.03%) should be far too small for the strategy
         * to try trading.
         */
        Assert.assertNull(contractManager.getOrdersFromFlat());
    }
    
    @Test
    public void testTooFarToTransmit()
        throws InsufficientDataException, StrategyException {
        final UnitTestContractManager contractManager;
        final CatchingDaggersFactory strategyFactory = new CatchingDaggersFactory(4, 0.5);
        final Date now = new Date();
        final Timestamp[] timeSeries = TestUtils.generateTimeSeries(5, now, 60000);
        final List<PeriodicData> testData = new ArrayList<PeriodicData>();

        // We need 4 data points as history, plus one "current" datapoint
        testData.add(new PeriodicData(timeSeries[0], 10000));
        testData.add(new PeriodicData(timeSeries[1], 9500));
        testData.add(new PeriodicData(timeSeries[2], 10500));
        testData.add(new PeriodicData(timeSeries[3], 10000));
        testData.add(new PeriodicData(timeSeries[4], 10238));

        contractManager = new UnitTestContractManager(new ContractDetails(), strategyFactory, testData, 60);
        contractManager.run();

        /* At 6 pips from the trade entry it should be just enough to trigger an
         * order without being enough to transmit it.
         */
        final EntryOrder entryOrder = contractManager.getOrdersFromFlat();
        Assert.assertNotNull(entryOrder);

        contractManager.close();
        
        Assert.assertEquals(OrderAction.SELL, entryOrder.getOrderAction());
        Assert.assertEquals(10244, entryOrder.getEntryLimitPrice());
        Assert.assertEquals(false, entryOrder.shouldTransmit());
    }
    
    @Test
    public void testJustInsideTransmitDistance()
        throws InsufficientDataException, StrategyException {
        final UnitTestContractManager contractManager;
        final CatchingDaggersFactory strategyFactory = new CatchingDaggersFactory(4, 0.5);
        final Date now = new Date();
        final Timestamp[] timeSeries = TestUtils.generateTimeSeries(5, now, 60000);
        final List<PeriodicData> testData = new ArrayList<PeriodicData>();

        // We need 4 data points as history, plus one "current" datapoint
        testData.add(new PeriodicData(timeSeries[0], 10000));
        testData.add(new PeriodicData(timeSeries[1], 9500));
        testData.add(new PeriodicData(timeSeries[2], 10500));
        testData.add(new PeriodicData(timeSeries[3], 10000));
        testData.add(new PeriodicData(timeSeries[4], 10240));

        contractManager = new UnitTestContractManager(new ContractDetails(), strategyFactory, testData, 60);
        contractManager.run();

        /* At 4 pips from the trade entry it should be just enough to set the
         * order as ready to transmit.
         */
        final EntryOrder entryOrder = contractManager.getOrdersFromFlat();
        Assert.assertNotNull(entryOrder);

        contractManager.close();
        
        Assert.assertEquals(OrderAction.SELL, entryOrder.getOrderAction());
        Assert.assertEquals(10244, entryOrder.getEntryLimitPrice());
        Assert.assertEquals(true, entryOrder.shouldTransmit());
    }
    
    @Test
    public void testThreePeriodFlat()
        throws InsufficientDataException, StrategyException {
        final UnitTestContractManager contractManager;
        final CatchingDaggersFactory strategyFactory = new CatchingDaggersFactory(3, 0.5);
        final Date now = new Date();
        final Timestamp[] timeSeries = TestUtils.generateTimeSeries(3, now, 60000);
        final List<PeriodicData> testData = new ArrayList<PeriodicData>();

        testData.add(new PeriodicData(timeSeries[0], 110));
        testData.add(new PeriodicData(timeSeries[1], 90));
        testData.add(new PeriodicData(timeSeries[2], 100));

        contractManager = new UnitTestContractManager(new ContractDetails(), strategyFactory, testData, 60);
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
        throws InsufficientDataException, StrategyException {
        final UnitTestContractManager contractManager;
        final CatchingDaggersFactory strategyFactory = new CatchingDaggersFactory(3, 0.5);
        final Date now = new Date();
        final Timestamp[] timeSeries = TestUtils.generateTimeSeries(3, now, 60000);
        final List<PeriodicData> testData = new ArrayList<PeriodicData>();

        testData.add(new PeriodicData(timeSeries[0], 12000));
        testData.add(new PeriodicData(timeSeries[1], 11000));
        testData.add(new PeriodicData(timeSeries[2], 9000));

        contractManager = new UnitTestContractManager(new ContractDetails(), strategyFactory, testData, 60);
        contractManager.run();

        /* The price drops fast in the third data point, so we should have an
         * order to go long.
         */
        final EntryOrder entryOrder = contractManager.getOrdersFromFlat();
        Assert.assertNotNull(entryOrder);

        contractManager.close();

        Assert.assertEquals(OrderAction.BUY, entryOrder.getOrderAction());
        Assert.assertEquals(9000, entryOrder.getEntryLimitPrice());
        Assert.assertEquals(9006, entryOrder.getExitLimitPrice());
    }
    
    @Test
    public void testThreePeriodShort()
        throws InsufficientDataException, StrategyException {
        final UnitTestContractManager contractManager;
        final CatchingDaggersFactory strategyFactory = new CatchingDaggersFactory(3, 0.5);
        final Date now = new Date();
        final Timestamp[] timeSeries = TestUtils.generateTimeSeries(3, now, 60000);
        final List<PeriodicData> testData = new ArrayList<PeriodicData>();

        testData.add(new PeriodicData(timeSeries[0], 9000));
        testData.add(new PeriodicData(timeSeries[1], 7000));
        testData.add(new PeriodicData(timeSeries[2], 10000));

        contractManager = new UnitTestContractManager(new ContractDetails(), strategyFactory, testData, 60);
        contractManager.run();

        /* The price rises fast in the third data point, so we should have an
         * order to go short.
         */
        final EntryOrder entryOrder = contractManager.getOrdersFromFlat();
        Assert.assertNotNull(entryOrder);

        contractManager.close();

        Assert.assertEquals(OrderAction.SELL, entryOrder.getOrderAction());
        Assert.assertEquals(10000, entryOrder.getEntryLimitPrice());
        Assert.assertEquals(9994, entryOrder.getExitLimitPrice());
    }
}