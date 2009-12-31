// $Id: BacktestContractManager.java 691 2009-11-09 19:44:32Z  $
package org.lostics.foxquant.backtest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;

import com.ib.client.ContractDetails;
import com.ib.client.Contract;
import com.ib.client.Order;

import org.lostics.foxquant.database.DatabaseUnavailableException;
import org.lostics.foxquant.model.AbstractContractManager;
import org.lostics.foxquant.model.ContractManager;
import org.lostics.foxquant.model.ContractPosition;
import org.lostics.foxquant.model.OrderType;
import org.lostics.foxquant.model.PeriodicData;
import org.lostics.foxquant.model.PriceType;
import org.lostics.foxquant.model.StrategyAlreadyExistsException;
import org.lostics.foxquant.model.Strategy;
import org.lostics.foxquant.report.Roundturn;
import org.lostics.foxquant.BacktestFill;
import org.lostics.foxquant.Configuration;

import org.apache.log4j.Logger;

/**
 * Data source for testing. Takes in a list of periodic data, and feeds it back
 * out when run.
 */
public class BacktestContractManager extends AbstractContractManager implements Runnable {
    private static final int REPORTING_INTERVAL = 60 * 24 * 5; // One week of 5 working days.

    private static final Logger log = Logger.getLogger(BacktestContractManager.class);

    private Queue<Order> orders = new ConcurrentLinkedQueue<Order>();
    private AtomicInteger actualPosition = new AtomicInteger(0);
    private AtomicReference<ContractPosition> status = new AtomicReference<ContractPosition>(ContractPosition.FLAT);
    private AtomicInteger targetPosition = new AtomicInteger(0);

    private List<Roundturn> roundturns;

    private Timestamp marketEntry;
    private double entryPrice;

    private PeriodicData lastBidPrice;

    /** The minimum time interval between periodic data entries, in seconds. */
    private final long periodMillis;

    private final ResultSet bidPrices;

    /**
     * Constructs a new list data source.
     *
     * @param setConfiguration the configuration to generate a database
     * connection from.
     * @param setContract the contract to backtest on.
     */
    public          BacktestContractManager(final ContractDetails setContractDetails, 
        final ResultSet prices, final long periodMillis)
        throws StrategyAlreadyExistsException {
        super(setContractDetails);
        
        this.periodMillis = periodMillis;
    
        this.bidPrices = prices;

        this.marketEntry = null;
        this.entryPrice = 0.0;

        this.lastBidPrice = null;

        this.roundturns = new ArrayList<Roundturn>();
    }

    public void close() {
    }

    /**
     * Constructs a new order then sends it to the connection manager for
     * transmission to TWS.
     */
    private Order executeOrder(final String action, final int quantity,
        final OrderType orderType, final double price, final double auxPrice,
        final String timeInForce) {
        final Order order = new Order();

        order.m_action = action;
        order.m_totalQuantity = quantity;
        order.m_orderType = orderType.toString();
        order.m_lmtPrice = price;
        order.m_auxPrice = auxPrice;
        order.m_tif = timeInForce;
        // order.m_transmit = false;

        this.orders.offer(order);

        return order;
    }

    public Order enterLong(final Date baseTime, final int quantity) {
        final Order order;

        if (!this.status.compareAndSet(ContractPosition.FLAT, ContractPosition.LONG)) {
            return null;
        }

        this.targetPosition.addAndGet(quantity);
        order = executeOrder(ContractManager.ORDER_ACTION_BUY, quantity, OrderType.MKT, 0, 0, "IOC");

        this.marketEntry = new Timestamp(baseTime.getTime());
        this.entryPrice = this.lastBidPrice.getPrice(PriceType.CLOSE);

        return order;
    }

    public Order enterShort(final Date baseTime, final int quantity) {
        final Order order;

        if (!this.status.compareAndSet(ContractPosition.FLAT, ContractPosition.SHORT)) {
            return null;
        }

        this.targetPosition.addAndGet(quantity);
        // XXX: Should be ORDER_ACTION_SELL_SHORT for shares
        order = executeOrder(ContractManager.ORDER_ACTION_SELL, quantity, OrderType.MKT, 0, 0, "IOC");

        this.marketEntry = new Timestamp(baseTime.getTime());
        this.entryPrice = this.lastBidPrice.getPrice(PriceType.CLOSE);

        return order;
    }

    public Order exitLong(final Date baseTime) {
        final Order order;
        final int quantity;

        if (!this.status.compareAndSet(ContractPosition.LONG, ContractPosition.FLAT)) {
            return null;
        }

        quantity = this.targetPosition.getAndSet(0);
        order = executeOrder(ContractManager.ORDER_ACTION_SELL, quantity, OrderType.MKT, 0, 0, "IOC");

        this.roundturns.add(new Roundturn(this.contract.m_conId, this.contract.m_localSymbol, 
            ContractManager.ORDER_ACTION_BUY, // Action for the _entry_
            this.marketEntry, this.entryPrice,
            new Timestamp(baseTime.getTime()), this.lastBidPrice.getPrice(PriceType.CLOSE)) );

        this.marketEntry = null;
        this.entryPrice = 0.0;

        return order;
    }

    public Order exitShort(final Date baseTime) {
        final Order order;
        final int quantity;

        if (!this.status.compareAndSet(ContractPosition.SHORT, ContractPosition.FLAT)) {
            return null;
        }

        quantity = this.targetPosition.getAndSet(0);
        order = executeOrder(ContractManager.ORDER_ACTION_BUY, quantity, OrderType.MKT, 0, 0, "IOC");

        this.roundturns.add(new Roundturn(this.contract.m_conId, this.contract.m_localSymbol, 
            ContractManager.ORDER_ACTION_SELL, // Action for the _entry_
            this.marketEntry, this.entryPrice,
            new Timestamp(baseTime.getTime()), this.lastBidPrice.getPrice(PriceType.CLOSE)) );

        this.marketEntry = null;
        this.entryPrice = 0.0;

        return order;
    }

    public long getBarPeriod() {
        return this.periodMillis;
    }

    public List<Order> getOrders() {
        return new ArrayList<Order>(this.orders);
    }

    public List<Roundturn> getRoundturns() { 
        return this.roundturns;
    }
    
    public Strategy getStrategy() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    public boolean isContractForexNZD() {
        // XXX: We probably want to make this an option to the constructor.
        return false;
    }

    public boolean isFlat() {
        return this.status.get() == ContractPosition.FLAT;
    }

    public boolean isLong() {
        return this.status.get() == ContractPosition.LONG;
    }

    public boolean isShort() {
        return this.status.get() == ContractPosition.SHORT;
    }

    /**
     * Returns the first order from the order queue, without removing it. Returns null if there are no
     * orders.
     */
    public Order peekOrder() {
        return this.orders.peek();
    }

    /**
     * Removes the first order from the order queue, and returns it. Returns null if there are no
     * orders.
     */
    public Order pollOrder() {
        return this.orders.poll();
    }

    public void run() {
        final long start = System.currentTimeMillis();
        int i = 0;

        try {
            while(this.bidPrices.next()) {
                i++;
                final PeriodicData bid = new PeriodicData( this.bidPrices.getTimestamp("START_TIME"), 
                    (int)(this.bidPrices.getDouble("OPEN") / getMinimumTick()), (int)(this.bidPrices.getDouble("HIGH") / getMinimumTick()), 
                    (int)(this.bidPrices.getDouble("LOW") / getMinimumTick()), (int)(this.bidPrices.getDouble("CLOSE") / getMinimumTick()));
                try {
                    this.lastBidPrice = bid;
                    // FIXME: sendToConsumers(bid);
                } catch(Exception e) {
                    System.err.println("Strategy threw exception during backtest.");
                    e.printStackTrace();
                }
                if (0 == (i % REPORTING_INTERVAL)) { 
                    log.info("Processed " + (i / REPORTING_INTERVAL) + " weeks worth.");
                }
            }
            this.bidPrices.close();
        } catch(SQLException ex) {
            log.error("Database problem while running backtest: ", ex);
            // And fall through and exit
        }

        log.debug("Processed " + i + " records in " + (System.currentTimeMillis() - start) + " ms");
    }
}
