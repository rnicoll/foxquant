// $Id: TWSContractManager.java 707 2009-11-12 01:30:30Z jrn $
package org.lostics.foxquant.ib;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import com.ib.client.ContractDetails;
import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.TickType;

import org.apache.log4j.Logger;

import org.lostics.foxquant.database.DatabaseUnavailableException;
import org.lostics.foxquant.model.AbstractContractManager;
import org.lostics.foxquant.model.ContractManager;
import org.lostics.foxquant.model.ContractPosition;
import org.lostics.foxquant.model.EntryOrder;
import org.lostics.foxquant.model.ExitOrders;
import org.lostics.foxquant.model.OrderAction;
import org.lostics.foxquant.model.OrderAlreadyInProgressException;
import org.lostics.foxquant.model.OrderStatus;
import org.lostics.foxquant.model.OrderType;
import org.lostics.foxquant.model.PeriodicDataBuffer;
import org.lostics.foxquant.model.PeriodicData;
import org.lostics.foxquant.model.PositionIsFlatException;
import org.lostics.foxquant.model.PositionNotFlatException;
import org.lostics.foxquant.model.StrategyAlreadyExistsException;
import org.lostics.foxquant.model.StrategyFactory;
import org.lostics.foxquant.model.Strategy;
import org.lostics.foxquant.model.TickData;
import org.lostics.foxquant.Configuration;
import org.lostics.foxquant.TwitterGateway;

/**
 * Manages a single contract; handles separating incoming data into a
 * per-contract thread, tracks position and orders on that contract, etc.
 */
public class TWSContractManager extends AbstractContractManager {
    private enum CMState {
        BACKFILL,
        TRADING,
        TARGET_FLAT
    };
    
    /**
     * Enum for the actions the contract manager may want to take once its
     * position in the market is flat.
     */
    private enum OnceFlat {
        /** Indicates the contract manager should resume trading once flat.
         * This is useful where it has to pause to clear an error, or change
         * trade direction.
         */
        TRADE,
        /** Indicates the contract manager should stay flat, and await an
         * external event. This is used where the user has told the trader
         * to flatten a position.
         */
        STAY_FLAT,
        /** Indicates the contract manager should exit once flat, for example
         * as part of a shutdown procedure.
         */
        EXIT
    }

    public static final int QUEUE_SIZE = 100;
    
    public static final int QUEUE_POLL_TIMEOUT = 6000;

    /** Entry/exit orders are valid for five minutes. */
    public static final int ORDER_TTL = 300000;
    
    private static final int DEFAULT_QUANTITY = 100000;

    private static final int INTERVAL_SUBMIT_CHILD_ORDERS = 300;
    private static final int INTERVAL_TRANSMIT_PARENT_ORDER = INTERVAL_SUBMIT_CHILD_ORDERS;

    /**
     * List of old data to be fed to consumers added to this contract
     * manager later. Held seperately from dataPoints as dataPoints is
     * not lossy, while this is (after the end of the cache window, data is
     * removed).
     */
    private final PeriodicDataBuffer backfillCache
        = new PeriodicDataBuffer(ConnectionManager.BACKFILL_HOURS
            * 60 * 60 / ConnectionManager.REAL_TIME_BAR_PERIOD_SECONDS,
            new Date(), ConnectionManager.REAL_TIME_BAR_PERIOD_SECONDS * 1000);

    /**
     * Whether or not the current strategy needs backfilling.
     */
    private CMState state = CMState.BACKFILL;
    
    // tickQueue and orderStatusQueue must be synchronized on notificationObject.
    private final Object notificationObject = new Object();
    private final Deque<OrderStatusDetails> orderStatusQueue = new ArrayDeque<OrderStatusDetails>(TWSContractManager.QUEUE_SIZE);
    private final Deque<TickData> tickQueue = new ArrayDeque<TickData>(TWSContractManager.QUEUE_SIZE);

    /**
     * Time when we last heard from the data feed. Used for the data feed
     * watchdog.
     */
    private long lastDataFeed;
    
    private final ConnectionManager connectionManager;
    
    private boolean stop = false;

    // Synchronize access on the following variables, on the contract manager
    // object.
    private Connection dbConnection;

    private OrderWrapper entryOrderWrapper;
    private OrderWrapper limitProfitOrderWrapper;
    private OrderWrapper stopLossOrderWrapper;
    
    /** Quantity filled from market entry order. */
    private int entryOrderFilled = 0;
    
    /** Average price paid when entering the market. */
    private double entryPrice;

    /** Our actual position in the market. Positive means long, negative short. */
    private int position = 0;
    
    private final Strategy strategy;

    /**
     * This is not intended to be called directly; use ConnectionManager.getTWSContractManager()
     * instead to ensure datafeeds are set up correctly.
     */
    protected TWSContractManager(final ConnectionManager setConnectionManager,
        final ContractDetails setContractDetails, final StrategyFactory strategyFactory)
        throws DatabaseUnavailableException, SQLException, StrategyAlreadyExistsException {
        super(setContractDetails);
        
        this.connectionManager = setConnectionManager;

        this.dbConnection = this.connectionManager.getConfiguration().getDBConnection();
        this.strategy = strategyFactory.getStrategy(this.connectionManager.getConfiguration(), this);

        this.setName(this.contract.m_localSymbol + " Manager");

        this.lastDataFeed = System.currentTimeMillis();

        this.entryOrderWrapper = new OrderWrapper(setConnectionManager, setContractDetails);
        this.limitProfitOrderWrapper = new OrderWrapper(setConnectionManager, setContractDetails);
        this.stopLossOrderWrapper = new OrderWrapper(setConnectionManager, setContractDetails);
    }

    /**
     * Cancels the entry order if it hasn't triggered yet, and the exit order
     * otherwise. MUST only be called from within the manager thread.
     */
    private void cancelOrders() {
        if (!this.entryOrderWrapper.hasValidOrder()) {
            // No orders in play, nothing to do.
            return;
        }

        if (this.entryOrderWrapper.isStatusFinal()) {
            this.connectionManager.cancelOrder(this.limitProfitOrderWrapper.getID());
            this.connectionManager.cancelOrder(this.stopLossOrderWrapper.getID());
            this.limitProfitOrderWrapper.setStatus(OrderStatus.PendingCancel);
            this.stopLossOrderWrapper.setStatus(OrderStatus.PendingCancel);
        } else {
            // XXX: Need to check entry order was submitted.
            // XXX: Need to check exit are automatically cancelled, and be able to recover
            // if not.
            this.connectionManager.cancelOrder(this.entryOrderWrapper.getID());
            this.entryOrderWrapper.setStatus(OrderStatus.PendingCancel);
        }
    }
    
    /**
     * Clears order records. Does not cancel orders, and MUST only be called once
     * flat in the market, and only from the contract manager thead.
     */
    private void clearOrders() {
        this.entryOrderWrapper.clearOrder();
        this.limitProfitOrderWrapper.clearOrder();
        this.stopLossOrderWrapper.clearOrder();
    }

    public void close()
        throws SQLException {
        this.stop = true;
        this.interrupt();
        this.dbConnection.close();
    }
    
    public Strategy getStrategy() {
        return this.strategy;
    }

    /**
     * Places the order to enter the market, and generates its child orders (but doesn't
     * place them). Not thread safe, should only be called from the
     * manager thread.
     */
    private void placeInitialEntryOrder(final Connection database, final EntryOrder orderDetails)
        throws PositionNotFlatException,
            OrderIDUnavailableException, SQLException, UnexpectedOrderTypeException {
        // No outstanding entry order, so create a new set of orders.
        final Order entryOrder;

        switch (orderDetails.getOrderType()) {
            case LMT:
                entryOrder = this.entryOrderWrapper.createLimitOrder(database,
                    orderDetails.getOrderAction(),
                    DEFAULT_QUANTITY, orderDetails.getEntryLimitPrice());
                break;
            case STP:
                entryOrder = this.entryOrderWrapper.createStopOrder(database,
                    orderDetails.getOrderAction(),
                    DEFAULT_QUANTITY, orderDetails.getEntryStopPrice());
                break;
            case STPLMT:
                entryOrder = this.entryOrderWrapper.createStopLimitOrder(database,
                    orderDetails.getOrderAction(),
                    DEFAULT_QUANTITY, orderDetails.getEntryLimitPrice(), orderDetails.getEntryStopPrice());
                break;
            default:
                throw new UnexpectedOrderTypeException(orderDetails.getOrderType());
        }

        this.entryOrderFilled = 0;
        this.entryPrice = 0;
            
        this.connectionManager.placeOrder(database,
            this, this.contract, entryOrder);
    }
    /**
     * Places the order to enter the market, and generates its child orders (but doesn't
     * place them). Not thread safe, should only be called from the
     * manager thread.
     */
    private void placeInitialExitOrders(final Connection database, final EntryOrder orderDetails)
        throws PositionNotFlatException,
            OrderIDUnavailableException, SQLException {
        final Order entryOrder = this.entryOrderWrapper.getOrder();
        final Order limitProfitOrder = this.limitProfitOrderWrapper.createLimitOrder(database,
            (orderDetails.getOrderAction() == OrderAction.BUY
                ? OrderAction.SELL
                : OrderAction.BUY),
            DEFAULT_QUANTITY, orderDetails.getExitLimitPrice());

        limitProfitOrder.m_parentId = entryOrder.m_orderId;
        limitProfitOrder.m_ocaGroup = generateOCAGroup();
        limitProfitOrder.m_ocaType = 1; // Cancel all other orders
        
        final Order stopLossOrder = this.stopLossOrderWrapper.createStopOrder(database,
            (orderDetails.getOrderAction() == OrderAction.BUY
                ? OrderAction.SELL
                : OrderAction.BUY),
            DEFAULT_QUANTITY, orderDetails.getExitStopPrice());
            
        stopLossOrder.m_parentId = entryOrder.m_orderId;
        stopLossOrder.m_ocaGroup = limitProfitOrder.m_ocaGroup;
        stopLossOrder.m_ocaType = 1; // Cancel all other orders

        this.connectionManager.placeOrder(database,
            this, this.contract, limitProfitOrder);
        this.connectionManager.placeOrder(database,
            this, this.contract, stopLossOrder);
    }

    /**
     * Places market entry orders. Not thread safe, should only be called from the
     * manager thread.
     */
    private void placeEntryOrders(final Connection database,
        final EntryOrder orderDetails)
        throws PositionNotFlatException, OrderIDUnavailableException, SQLException,
            UnexpectedOrderTypeException {
        if (this.position != 0) {
            throw new PositionNotFlatException("Could not place entry order because already in the market.");
        }
        
        if (!this.entryOrderWrapper.hasValidOrder()) {
            placeInitialEntryOrder(database, orderDetails);
            return;
        }
        
        if (this.entryOrderWrapper.getAction() != orderDetails.getOrderAction() ||
            this.entryOrderWrapper.getType() != orderDetails.getOrderType()) {
            // Order direction has changed. Cancel all orders, then return once that's done
            this.state = CMState.TARGET_FLAT;
            log.info("Cancelling entry order in an attempt to flatten to the market before switching trade side.");
            this.connectionManager.cancelOrder(this.entryOrderWrapper.getID());
            
            return;
        }
        
        if (!this.limitProfitOrderWrapper.hasValidOrder()) {
            final long interval = System.currentTimeMillis() - this.entryOrderWrapper.getCreated().getTime();
            
            if (interval >= INTERVAL_SUBMIT_CHILD_ORDERS) {
                placeInitialExitOrders(database, orderDetails);
            }
            
            return;
        }
        
        // If orders haven't been transmitted yet, do that now
        if (!this.entryOrderWrapper.getTransmitFlag()) {
            final long interval;

            if (!orderDetails.shouldTransmit()) {
                // Strategy isn't confident enough to transmit yet, so we have nothing more to do
                return;
            }

            interval = System.currentTimeMillis() - this.limitProfitOrderWrapper.getCreated().getTime();
            if (interval >= INTERVAL_TRANSMIT_PARENT_ORDER) {
                this.entryOrderWrapper.setTransmitFlag(true);
                
                // XXX: Limit/stop loss transmit should be set from the order status change
                this.limitProfitOrderWrapper.setTransmitFlag(true);
                this.stopLossOrderWrapper.setTransmitFlag(true);
                this.entryOrderWrapper.setPrices(orderDetails.getEntryLimitPrice(), orderDetails.getEntryStopPrice());
                this.connectionManager.placeOrder(database,
                    this, this.contract, this.entryOrderWrapper.getOrder());
            }

            return;
        }

        if (this.entryOrderWrapper.setPrices(orderDetails.getEntryLimitPrice(), orderDetails.getEntryStopPrice())) {
            this.connectionManager.placeOrder(database,
                this, this.contract, this.entryOrderWrapper.getOrder());
        }
        if (this.limitProfitOrderWrapper.setLimitPrice(orderDetails.getExitLimitPrice())) {
            this.connectionManager.placeOrder(database,
                this, this.contract, this.limitProfitOrderWrapper.getOrder());
        }
        if (this.stopLossOrderWrapper.setStopPrice(orderDetails.getExitStopPrice())) {
            this.connectionManager.placeOrder(database,
                this, this.contract, this.stopLossOrderWrapper.getOrder());
        }

        return;
    }

    /**
     * Places market entry orders.  Not thread safe, should only be called from the
     * manager thread.
     */
    private void placeExitOrders(final ExitOrders orderDetails)
        throws DatabaseUnavailableException, PositionIsFlatException, OrderIDUnavailableException, SQLException {  
        if (this.position == 0) {
            throw new PositionIsFlatException("Could not place exit orders because already flat to the market.");
        }

        if (this.limitProfitOrderWrapper.setLimitPrice(orderDetails.getExitLimitPrice())) {
            this.connectionManager.placeOrder(this.getDBConnection(),
                this, this.contract, this.limitProfitOrderWrapper.getOrder());
        }
        if (this.stopLossOrderWrapper.setStopPrice(orderDetails.getExitStopPrice())) {
            this.connectionManager.placeOrder(this.getDBConnection(),
                this, this.contract, this.stopLossOrderWrapper.getOrder());
        }

        return;
    }

    private String generateOCAGroup() {
        final long millis = System.currentTimeMillis();

        return this.contract.m_localSymbol
            + millis;
    }

    /**
     * Retrieves a database connection. This is cached in the class for speed,
     * and MUST not be closed until the contract manager is disposed of.
     */
    private Connection getDBConnection()
        throws DatabaseUnavailableException {
        try {
            final PreparedStatement statement = this.dbConnection.prepareStatement("SELECT 1 FROM DUAL");

            statement.executeQuery();
            statement.close();
        } catch(SQLException e) {
            log.warn("Recovering database connection: ", e);
            this.dbConnection = this.connectionManager.getConfiguration().getDBConnection();
        }

        return this.dbConnection;
    }

    public long getBarPeriod() {
        return ConnectionManager.REAL_TIME_BAR_PERIOD_SECONDS * 1000;
    }

    public Date getFirstGapInBackfillCache() {
        return this.backfillCache.getFirstGap();
    }

    public String getLocalSymbol() {
        return this.contract.m_localSymbol;
    }

    public void handleHistoricPrice(final PeriodicData periodicData, final boolean hasGaps) {
        assert null != periodicData;

        this.backfillCache.handlePeriodicData(periodicData);
    }

    public void handleHistoricPriceFinished() {
        // Do nothing.
    }

    private void handleOrderStatus(final OrderStatusDetails details) {
        final OrderStatus status = details.getOrderStatus();
        
        if (!this.entryOrderWrapper.hasValidOrder()) {
            // XXX: Need to track old orders, and ignore duplicate status reports for them.
            log.error("Received order status for order #"
                + details.getOrderID() + " while flat to the market.");
            return;
        }
    
        // Process the order status
        if (details.getOrderID() == this.entryOrderWrapper.getID()) {
            final Order order = this.entryOrderWrapper.getOrder();
            
            this.entryOrderFilled = details.getFilled();
            this.entryPrice = details.getAvgFillPrice();
            this.entryOrderWrapper.setStatus(status);
            
            if (status == OrderStatus.Submitted) {
                this.limitProfitOrderWrapper.setTransmitFlag(true);
                this.stopLossOrderWrapper.setTransmitFlag(true);
            } else if (status == OrderStatus.Filled) {
                final TwitterGateway twitter = this.connectionManager.getTwitterGateway();
                
                if (null != twitter) {
                    if (order.m_action.equals(OrderAction.BUY.toString())) {
                        twitter.updateStatus("Bought "
                            + this.DEFAULT_QUANTITY + " "
                            + this.contract.m_localSymbol + " at "
                            + details.getAvgFillPrice() + ".");
                    } else {
                        twitter.updateStatus("Sold "
                            + this.DEFAULT_QUANTITY + " "
                            + this.contract.m_localSymbol + " at "
                            + details.getAvgFillPrice() + ".");
                    }
                }
            } else if (status == OrderStatus.Cancelled) {
                if (this.state != CMState.TARGET_FLAT) {
                    log.error("Opening order cancelled. Flattening position then restarting trading.");
                    this.state = CMState.TARGET_FLAT;
                }
            }
            
            if (order.m_action.equals(OrderAction.BUY.toString())) {
                this.position = this.entryOrderFilled;
            } else {
                this.position = -this.entryOrderFilled;
            }
            
            final OrderAction action = this.entryOrderWrapper.getAction();
            
            try {
                final double avgFillPrice = details.getAvgFillPrice();
            
                this.strategy.handleEntryOrderStatus(action, status, details.getFilled(),
                    details.getRemaining(), (int)(avgFillPrice / getMinimumTick()));
            } catch(Exception e) {
                log.fatal("Strategy failed to handle entry order status change.", e);
                
                this.state = CMState.TARGET_FLAT;
                // XXX: Should indicate no further trades once flat
            }
        } else {
            final boolean isLimitOrder;
            final Order order;
            
            // Order status is from one of the two exit orders.
            
            if (this.limitProfitOrderWrapper.hasValidOrder() &&
                details.getOrderID() == this.limitProfitOrderWrapper.getID()) {
                order = this.limitProfitOrderWrapper.getOrder();
                this.limitProfitOrderWrapper.setStatus(status);
                isLimitOrder = true;
            } else if (this.stopLossOrderWrapper.hasValidOrder() &&
                details.getOrderID() == this.stopLossOrderWrapper.getID()) {
                order = this.stopLossOrderWrapper.getOrder();
                this.stopLossOrderWrapper.setStatus(status);
                isLimitOrder = false;
            } else {
                log.warn("Received order status for unknown order #"
                    + details.getOrderID());
                return;
            }
            
            if (null != this.strategy) {
                final OrderAction action = this.entryOrderWrapper.getAction();

                try {
                    final double avgFillPrice = details.getAvgFillPrice();
                    
                    this.strategy.handleExitOrderStatus(action, isLimitOrder, status, details.getFilled(),
                        details.getRemaining(), (int)(avgFillPrice / getMinimumTick()));
                } catch(Exception e) {
                    log.fatal("Strategy failed to handle exit order status change.", e);
                    
                    this.state = CMState.TARGET_FLAT;
                    // XXX: Should indicate no further trades once flat
                }
            }
            
            if (order.m_action.equals(OrderAction.BUY.toString())) {
                this.position = details.getFilled() - this.entryOrderFilled;
            } else {
                this.position = this.entryOrderFilled - details.getFilled();
            }
            if (status == OrderStatus.Filled) {
                final TwitterGateway twitter = this.connectionManager.getTwitterGateway();
                
                if (null != twitter) {
                    if (order.m_action.equals(OrderAction.BUY.toString())) {
                        double profit = this.entryPrice - details.getAvgFillPrice();
                        
                        profit = Math.round(profit * DEFAULT_QUANTITY * 100.0) / 100.0;
                        
                        twitter.updateStatus("Bought "
                            + this.DEFAULT_QUANTITY + " "
                            + this.contract.m_localSymbol + " at "
                            + details.getAvgFillPrice() + ". Sold at "
                            + this.entryPrice + ", p/l "
                            + profit
                            + this.contract.m_currency + ".");
                    } else {
                        double profit = details.getAvgFillPrice() - this.entryPrice;
                        
                        profit = Math.round(profit * DEFAULT_QUANTITY * 100.0) / 100.0;
                        
                        twitter.updateStatus("Sold "
                            + this.DEFAULT_QUANTITY + " "
                            + this.contract.m_localSymbol + " at "
                            + details.getAvgFillPrice() + ". Bought at "
                            + this.entryPrice + ", p/l "
                            + profit
                            + this.contract.m_currency + ".");
                    }
                }
                
                // XXX: Should double check all orders have filled/cancelled
            }
        }
        
        // Check for all orders being cancelled or filled, and position being 0.
        final OrderStatus entryOrderStatus = this.entryOrderWrapper.getStatus();
        final boolean entryOrderFinalised = entryOrderStatus == OrderStatus.Filled ||
            // We're a little more forgiving about checking the entry order has been cancelled,
            // as TWS doesn't appear to report its cancellation!
            entryOrderStatus == OrderStatus.PendingCancel ||
            entryOrderStatus == OrderStatus.Cancelled;
        final boolean limitProfitOrderFinalised = this.limitProfitOrderWrapper.isStatusFinal();
        final boolean stopLossOrderFinalised = this.stopLossOrderWrapper.isStatusFinal();
        
        if (entryOrderFinalised &&
            limitProfitOrderFinalised &&
            stopLossOrderFinalised) {
            // Well, we're not expecting any more updates. If our position
            // in the market is 0, good to reset.
            if (this.position == 0) {
                clearOrders();        
                if (this.state == CMState.TARGET_FLAT) {
                    try {
                        this.strategy.handlePositionFlat();
                    } catch(Exception e) {
                        log.fatal("Strategy failed to handle notification of position being flat.", e);
                        
                        this.state = CMState.TARGET_FLAT;
                        // XXX: Should indicate no further trades once flat
                    }
                    
                    // XXX: Need to handle situations where it's not trading
                    // after going flat.
                    this.state = CMState.TRADING;
                }
            } else {
                // Need intervention to fix
                this.state = CMState.TARGET_FLAT;
            }
        }
        
        // Check in case we cancelled before the orders were sent to the market:
        if (entryOrderStatus == OrderStatus.ApiCancelled &&
            this.limitProfitOrderWrapper.getStatus() == OrderStatus.PendingSubmit &&
            this.stopLossOrderWrapper.getStatus() == OrderStatus.PendingSubmit) {
            clearOrders();
            if (this.state == CMState.TARGET_FLAT) {
                try {
                    this.strategy.handlePositionFlat();
                } catch(Exception e) {
                    log.fatal("Strategy failed to handle notification of position being flat.", e);
                    
                    this.state = CMState.TARGET_FLAT;
                    // XXX: Should indicate no further trades once flat
                }
                
                // XXX: Need to handle situations where it's not trading
                // after going flat.
                this.state = CMState.TRADING;
            }
        }
    }

    public boolean handleTick(final TickData tickData)
        throws Exception {
        final boolean success;
        
        this.lastDataFeed = System.currentTimeMillis();
        synchronized(this.notificationObject) {
            success = this.tickQueue.offer(tickData);
            this.notificationObject.notifyAll();
        }
        
        return success;
    }

    public boolean isContractForexNZD() {
        return this.contract.m_secType.equals(ConnectionManager.CONTRACT_SECURITY_TYPE_CASH) &&
            (this.contract.m_symbol.equals("NZD") ||
            this.contract.m_currency.equals("NZD"));
    }

    public boolean isFlat() {
        return this.position == 0;
    }

    public boolean isLong() {
        return this.position > 0;
    }

    public boolean isShort() {
        return this.position < 0;
    }

    private void placeOrders() {
        if (this.isFlat()) {
            final EntryOrder orderDetails;

            try {
                orderDetails = this.strategy.getOrdersFromFlat();
            } catch(Exception e) {
                // XXX: Need to check orders cancel okay, and allow for re-activation of the strategy
                log.fatal("Caught exception while generating orders from strategy. Cancelling all outstanding orders.",
                    e);
                this.state = CMState.TARGET_FLAT;
                cancelOrders();
                // XXX: Exit once flat.
                return;
            }
            
            if (null != orderDetails) {
                final Connection database;
                try {
                    database = this.getDBConnection();
                } catch(DatabaseUnavailableException e) {
                    log.fatal("Cannot generate entry orders because I cannot contact the database server. Cancelling all outstanding orders.",
                        e);
                    this.state = CMState.TARGET_FLAT;
                    // XXX: Exit once flat.
                    return;
                }
                
                try {
                    this.placeEntryOrders(database, orderDetails);
                } catch(Exception e) {
                    // XXX: Need to ensure we're in a stable state of some kind at this point
                    log.fatal("Error while placing market entry order.", e);
                    this.state = CMState.TARGET_FLAT;
                    cancelOrders();
                    // XXX: Exit once flat.
                    return;
                }
            } else {
                // Strategy returning null entry orders means no valid trade price.
                // Cancel any outstanding orders and then ensure we are flat to the market.
                cancelOrders();
                if (this.entryOrderWrapper.hasValidOrder()) {
                    // We had a valid order in the market, so ensure we go flat successfully
                    this.state = CMState.TARGET_FLAT;
                }
            }
        } else {
            final ExitOrders orderDetails;
            
            try {
                if (isLong()) {
                    orderDetails = this.strategy.getOrdersFromLong();
                } else {
                    orderDetails = this.strategy.getOrdersFromShort();
                }
            } catch(Exception e) {
                // XXX: Need to check orders cancel okay, and allow for re-activation of the strategy
                log.error("Caught exception while generating orders from strategy. Cancelling all outstanding orders and removing strategy.",
                    e);
                this.state = CMState.TARGET_FLAT;
                cancelOrders();
                return;
            }
            
            if (null != orderDetails) {
                try {
                    this.placeExitOrders(orderDetails);
                } catch(Exception e) {
                    log.fatal("Error while placing market exit orders.");
                    this.state = CMState.TARGET_FLAT;
                    cancelOrders();
                    // XXX: Exit once flat.
                    return;
                }
            } else {
                // FIXME: Leave market at market price
            }
        }
    }

    public void run() {
        while (!this.stop) {
            final OrderStatusDetails[] pendingOrderStatuses;
            final TickData[] pendingTicks;
            
            synchronized (this.notificationObject) {            
                if (this.tickQueue.size() == 0 &&
                    this.orderStatusQueue.size() == 0) {
                    if (null != this.strategy) {
                        this.strategy.updateSwingComponent();
                    }
                    
                    try {
                        // Wait for notification of new data to process
                        this.notificationObject.wait();
                    } catch(InterruptedException e) {
                        if (this.stop) {
                            return;
                        }
                    }
                }
                
                pendingTicks = this.tickQueue.toArray(new TickData[this.tickQueue.size()]);
                this.tickQueue.clear();
                
                pendingOrderStatuses = this.orderStatusQueue.toArray(new OrderStatusDetails[this.orderStatusQueue.size()]);
                this.orderStatusQueue.clear();
            }
        
            // Backfill from the data thread so that strategies do not get
            // backfill and realtime data at the same time.
            if (this.state == CMState.BACKFILL) {
                try {
                    for (PeriodicData backdata: this.backfillCache) {
                        this.strategy.backfillMinuteBar(backdata);
                    }
                    this.state = CMState.TRADING;
                } catch(Exception e) {
                    log.error("Received error from data consumer "
                        + this.strategy.getClass().getName() + " during backfill: "
                        + e);
                    this.connectionManager.emergencyStop();
                }
            }
            
            try {
                this.strategy.handleTick(pendingTicks);
            } catch(Exception e) {
                // XXX: Should abort
                log.error("Caught exception while passing perioidic data to strategy: ",
                    e);
            }
            
            for (int orderStatusIdx = 0; orderStatusIdx < pendingOrderStatuses.length; orderStatusIdx++) {
                final OrderStatusDetails details = pendingOrderStatuses[orderStatusIdx];
                handleOrderStatus(details);
            }
            
            if (this.state == CMState.TRADING) {
                if (!this.stop) {
                    placeOrders();
                }
            } else if (this.state == CMState.TARGET_FLAT) {
                // XXX: Need to try ensuring the orders are all being cancelled
                // and positions unwound.
                
                // Check for any outstanding uncancelled orders, and cancel
                // If orders remain uncancelled, check open orders in case of a mis-match
                // Push a market price exit order through to clear any outstanding position
                
                // XXX: Need to have a timeout if it's actively trying to go flat, rather
                // than being purely event driven.
            }
        }

        return;
    }

    protected boolean orderStatusChanged(final OrderStatusDetails details) {
        final boolean success;
        synchronized (this.notificationObject) {
            success = this.orderStatusQueue.offer(details);
            this.notificationObject.notifyAll();
        }

        return success;
    }
}
