// $Id: CatchingDaggers.java 707 2009-11-12 01:30:30Z jrn $
package org.lostics.foxquant.strategy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Spring;
import javax.swing.SpringLayout;

import org.apache.log4j.Logger;

import org.lostics.foxquant.database.DatabaseUnavailableException;
import org.lostics.foxquant.ib.OrderIDUnavailableException;
import org.lostics.foxquant.indicator.BollingerBands;
import org.lostics.foxquant.indicator.InsufficientDataException;
import org.lostics.foxquant.indicator.SimpleMovingAverage;
import org.lostics.foxquant.model.ContractManager;
import org.lostics.foxquant.model.EntryOrder;
import org.lostics.foxquant.model.ExitOrders;
import org.lostics.foxquant.model.OrderAction;
import org.lostics.foxquant.model.OrderAlreadyInProgressException;
import org.lostics.foxquant.model.OrderStatus;
import org.lostics.foxquant.model.PartialPeriodicData;
import org.lostics.foxquant.model.PeriodicData;
import org.lostics.foxquant.model.PriceType;
import org.lostics.foxquant.model.Strategy;
import org.lostics.foxquant.model.StrategyException;
import org.lostics.foxquant.model.TickData;
import org.lostics.foxquant.Configuration;

/**
 * BB band bounceback strategy with entry confirmation from long term
 * SMA. Works well with 3 minute bars on currencies.
 */
public class CatchingDaggers implements Strategy {
    private static final long ONE_MINUTE = 60000;

    public static final long BAR_PERIOD = ONE_MINUTE; // One minute
    
    /**
     * How long, after a trade exits, before the strategy will trade again.
     * Used to ensure it doesn't re-enter immediately after a stop-loss is
     * hit (although should probably wait until the market hits SMA really).
     */
    public static final long COOLDOWN_PERIOD = ONE_MINUTE * 20;
    
    /**
     * How long the trader expects to be in the market, for each trade,
     * measured in minutes. Used to generate targetProfitPerMinute.
     */
    public static final int EXPECTED_TRADE_DURATION_MINUTES = 60;
    
    /**
     * Ratio of price as maximum distance from the entry point before
     * trades are transmitted from TWS to the market.
     */
    public static final double TRANSMIT_DISTANCE_MULTIPLIER = 0.00040;
    
    /**
     * Ratio of price as maximum distance from the entry point before
     * trades are entered into TWS. Outside this range orders are
     * cancelled.
     */
    public static final double ORDER_DISTANCE_MULTIPLIER = 0.00060;
    
    /**
     * Ratio of price as maximum distance from the entry point before
     * trades entered trades are cancelled.
     */
    public static final double CANCEL_DISTANCE_MULTIPLIER = 0.00070;
    
    /**
     * Ratio of price as minimum expected profit from a trade. Below this
     * there is too high a chance of getting stopped out early.
     */
    public static final double MIN_PROFIT_MULTIPLIER = 0.00080;
    
    /**
     * Ratio of price as maximum expected profit from a trade. Beyond this
     * value it's assumed the market is too far out to be predictable.
     */
    public static final double MAX_PROFIT_MULTIPLIER = 0.003;
    
    /**
     * Ratio of the distance between upper/lower band and the SMA, to expect
     * as profit from a trade.
     */
    public static final double PROFIT_TARGET_MULTIPLIER = 0.75;
 
    /** Close out all positions 6 minutes before the market closes. */
    private static final int MARKET_CLOSE_HARD_STOP = 6 * 60 * 1000;

    /** Don't open any more positions after 45 minutes before market close. */
    private static final int MARKET_CLOSE_SOFT_STOP = 45 * 60 * 1000;
    
    // -----------------------------------------------------------------------
    // Finished with the constants.

    private static final Logger log = Logger.getLogger(CatchingDaggers.class);
    private Configuration configuration;
    private ContractManager contractManager;
    private CatchingDaggersFactory factory;
    
    private Date marketClose;

    /** The number of historical data bars the strategy uses to make decisions. */
    private final int totalHistoricalBars;

    /** The number of historical data bars the strategy has so far. */
    private int historicalBars = 0;
    
    private final BollingerBands bidBB;
    private final BollingerBands askBB;

    private final Timestamp runStart = new Timestamp(System.currentTimeMillis());
    
    /** Used to re-create the per-minute data to go into the Bollinger bands. */
    private PartialPeriodicData askMinuteBar;
    private PartialPeriodicData bidMinuteBar;
    
    private boolean orderPlaced = false;
    
    /** Re-usable entry order object */
    private final EntryOrder entryOrderPool = new EntryOrder();
    
    /** Re-usable exit orders object */
    private final ExitOrders exitOrdersPool = new ExitOrders();
    
    private final TradingRequest longTradeRequest;
    private final TradingRequest shortTradeRequest;
    
    /** The time at which the entry order initially completed (irrespective
     * of quantity).
     */
    private long timeEnteredMarket = 0;
    
    /** The time at which the exit order completed filled. Used to handle cool-down.
     */
    private long timeExitedMarket = 0;
    
    private int actualEntryPrice;
    private int projectedEntryPrice;
    private int actualExitLimitPrice;
    private int projectedExitLimitPrice;
    private int actualExitStopPrice;
    private int projectedExitStopPrice;
    
    /**
     * The value difference the strategy would expect to make as a profit,
     * based on actual entry price. Used to calculate limit orders.
     */
    private int targetProfit;
    
    /**
     * The profit rate this trade would achieve if it hits targetProfit in
     * exactly EXPECTED_TRADE_DURATION_MINUTES minutes. Stored in minutes
     * for precision reasons.
     */
    private int targetProfitPerMinute;

    private Integer mostRecentBid;
    private Integer mostRecentAsk;
    private long mostRecentUpdate = System.currentTimeMillis();
    
    private TFPanel swingPanel = null;

    /**
     * @param setConfiguration configuration to retrieve database connections
     * from. May be null, in which case no logging to database will be done.
     * @param setFactory the factory that created this strategy. Used to
     * manage cross-strategy details such as blocking multiple concurrent
     * entries into the same position on a currency.
     */
    protected CatchingDaggers(final Configuration setConfiguration, final CatchingDaggersFactory setFactory,
        final ContractManager setContractManager,
        final int setHistoricalBars, final double setSpread) {
        this.bidBB = new BollingerBands(setSpread, setHistoricalBars);
        this.askBB = new BollingerBands(setSpread, setHistoricalBars);
        
        this.contractManager = setContractManager;
        this.configuration = setConfiguration;
        this.factory = setFactory;
        this.totalHistoricalBars = setHistoricalBars;
        
        this.marketClose = setContractManager.getMarketCloseTime(new Date());
        
        this.longTradeRequest = new TradingRequest(setFactory,
            this, this.contractManager.getBaseCurrency(), this.contractManager.getPurchaseCurrency());
        this.shortTradeRequest = new TradingRequest(setFactory,
            this, this.contractManager.getPurchaseCurrency(), this.contractManager.getBaseCurrency());
    }
    
    public boolean equals(final Object o) {
        final CatchingDaggers strategyB = (CatchingDaggers)o;
        return this.contractManager.equals(strategyB.contractManager);
    }
    
    public int hashCode() {
        return this.contractManager.hashCode();
    }
    
    public String toString() {
        return "Catching daggers strategy running on "
            + this.contractManager.toString();
    }

    public void backfillMinuteBar(final PeriodicData periodicData)
        throws StrategyException {
        try {
            this.bidBB.handlePeriodicData(periodicData);
            this.askBB.handlePeriodicData(periodicData);
        } catch(InsufficientDataException e) {
            throw new StrategyException(e);
        }
        this.historicalBars++;
    }
    
    /**
     * Calculate the maximum value distance that we'll maintain an order in the market,
     * between the current bid/ask and the entry price. Outwith this range, any
     * order is cancelled. See {@link #getOrderDistance()} for the point when a
     * new order is generated.
     */
    private int getCancelDistance() {
        // Calculate the minimum profit before we'll trade, based on the mid-point price.
        double distance = (this.mostRecentAsk + this.mostRecentBid) * CANCEL_DISTANCE_MULTIPLIER / 2.0;
        
        // Convert this to a valid price point. Note this uses ceiling, not round.
        return (int)Math.ceil(distance);
    }
    
    private int getEntryLong()
        throws InsufficientDataException {                                                                   
        return this.bidBB.getLower();
    }                                                                                                        
                                                                                                             
    private int getEntryShort()
        throws InsufficientDataException {                                                                  
        return this.askBB.getUpper();
    }

    /**
     * Returns the price at which the strategy would exit the market with a
     * profit, if it had a long position, ignoring any quick profit taking.
     *
     * @return the minimum price at which the strategy would want to exit the
     * market, to close a pre-existing long position.
     */
    private int getExitLong()
        throws InsufficientDataException {                                                                   
        return this.bidBB.getMidpoint();
    }                                                                                                        
                       
    /**
     * Returns the price at which the strategy would exit the market with a
     * profit, if it had a short position, ignoring any quick profit taking.
     *
     * @return the maximum price at which the strategy would want to exit the
     * market, to close a pre-existing short position.
     */                                                                                      
    private int getExitShort()
        throws InsufficientDataException {                                                                  
        return this.askBB.getMidpoint();
    }
    
    private int getMinimumProfit() {
        // Calculate the minimum profit before we'll trade, based on the mid-point price.
        double minimumProfit = (this.mostRecentAsk + this.mostRecentBid) * MIN_PROFIT_MULTIPLIER / 2.0;
        
        return (int)Math.ceil(minimumProfit);
    }
    
    /**
     * Calculate the maxium value distance before an order is submitted to TWS
     * (but not transmitted), between the current bid/ask and the entry price.
     */
    private int getOrderDistance() {
        // Calculate the minimum profit before we'll trade, based on the mid-point price.
        double distance = (this.mostRecentAsk + this.mostRecentBid) * ORDER_DISTANCE_MULTIPLIER / 2.0;
        
        // Convert this to a valid price point. Note this uses ceiling, not round.
        return (int)Math.ceil(distance);
    }
    
    /**
     * Calculate the maximum value distance before an order is transmitted to
     * the exchange, between the current bid/ask and the entry price.
     */
    private int getTransmitDistance() {
        // Calculate the minimum profit before we transmit trades to the market, based on the mid-point price.
        double distance = (this.mostRecentAsk + this.mostRecentBid) * TRANSMIT_DISTANCE_MULTIPLIER / 2.0;
        
        // Convert this to a valid price point. Note this uses ceiling, not round.
        return (int)Math.ceil(distance);
    }
    
    public void notifyTradingRequestApproved() {
        // FIXME: Do stuff here
    }
    
    private EntryOrder generateLongOrder(final int distance)
        throws InsufficientDataException, StrategyException {
        final int cancelDistance = getCancelDistance();
        final int projectedProfit;
        final boolean transmit;
        
        // Go long
        this.projectedEntryPrice = this.getEntryLong();
        this.actualEntryPrice = Math.min(this.mostRecentAsk, this.projectedEntryPrice);
        
        // We're closer to going long, so we want to get the distance from
        // the SMA down to the entry price.
        this.targetProfit = (int)Math.ceil((this.getExitLong() - this.actualEntryPrice) * PROFIT_TARGET_MULTIPLIER);
        
        this.projectedExitLimitPrice = this.actualEntryPrice + this.targetProfit;
        this.projectedExitStopPrice = this.actualEntryPrice - this.targetProfit;
        
        // Check the spread on the Bollinger Band is wide enough to make this
        // a viable trade.
        projectedProfit = (int)Math.ceil((this.getExitLong() - this.projectedEntryPrice) * PROFIT_TARGET_MULTIPLIER);
        if (projectedProfit < getMinimumProfit()) {
            return null;
        }
        
        if (distance > cancelDistance) {
            // Too far out, cancel any existing order.
            return null;
        }
        
        if (!this.orderPlaced &&
            distance > getOrderDistance()) {
            // No pre-existing order, and we're too far out to create a
            // new order, so delete them.
            return null;
        }
        
        this.longTradeRequest.queueIfInactive();
        transmit = getTransmitDistance() > distance &&
            this.longTradeRequest.isApproved();

        this.entryOrderPool.setLong(this.actualEntryPrice,
            this.projectedExitLimitPrice, this.projectedExitStopPrice,
            transmit);
        this.actualExitLimitPrice = this.projectedExitLimitPrice;
        this.actualExitStopPrice = this.projectedExitStopPrice;
            
        return this.entryOrderPool;
    }
    
    private EntryOrder generateShortOrder(final int distance)
        throws InsufficientDataException, StrategyException {
        final int cancelDistance = getCancelDistance();
        final int projectedProfit;
        final boolean transmit;
        
        // Go short
        this.projectedEntryPrice = this.getEntryShort();
        this.actualEntryPrice = Math.max(this.mostRecentBid, this.projectedEntryPrice);
        
        // We're closer to going short, so we want to get the distance from
        // the entry price down to the SMA.
        this.targetProfit = (int)Math.ceil((this.getExitLong() - this.actualEntryPrice) * PROFIT_TARGET_MULTIPLIER);
        
        this.projectedExitLimitPrice = this.actualEntryPrice - this.targetProfit;
        this.projectedExitStopPrice = this.actualEntryPrice + this.targetProfit;
        
        // Check the spread on the Bollinger Band is wide enough to make this
        // a viable trade.
        projectedProfit = (int)Math.ceil((this.projectedEntryPrice - this.getExitShort()) * PROFIT_TARGET_MULTIPLIER);
        if (projectedProfit < getMinimumProfit()) {
            return null;
        }
        
        if (distance > cancelDistance) {
            // Too far out, cancel any existing order.
            return null;
        }
        
        if (!this.orderPlaced &&
            distance > getOrderDistance()) {
            // No pre-existing order, and we're too far out to create a
            // new order, so delete them.
            return null;
        }
        
        this.shortTradeRequest.queueIfInactive();
        transmit = getTransmitDistance() > distance &&
            this.shortTradeRequest.isApproved();

        this.entryOrderPool.setShort(this.actualEntryPrice,
            this.projectedExitLimitPrice, this.projectedExitStopPrice,
            transmit);
        this.actualExitLimitPrice = this.projectedExitLimitPrice;
        this.actualExitStopPrice = this.projectedExitStopPrice;
            
        return this.entryOrderPool;
    }

    public EntryOrder getOrdersFromFlat()
        throws StrategyException {
        if (this.historicalBars < this.totalHistoricalBars) {
            return null;
        }
        
        final long timeToClose;
        final long now = System.currentTimeMillis();
        if (this.marketClose.getTime() < now) {
            final Date nowDate = new Date(now);
            this.marketClose = this.contractManager.getMarketCloseTime(nowDate);
        }
        timeToClose = this.marketClose.getTime() - now;

        if (timeToClose <= MARKET_CLOSE_SOFT_STOP) {
            log.debug("Too close to market close at "
                + marketClose + ", remaining flat in market.");
            return null;
        }
        
        final long timeSinceTraded = now - this.timeExitedMarket;
        
        if (timeSinceTraded < COOLDOWN_PERIOD) {
            log.debug("Most recent trade completed too recently, still in cooldown period.");
            return null;
        }
        
        if (this.mostRecentAsk == null ||
            this.mostRecentBid == null) {
            log.debug("No most recent bid/ask to generate entry prices from.");
            // XXX: Should have a countdown timer before we're willing to re-enter the market
            return null;
        }
        
        try {
            final EntryOrder entryOrder;
            final int distanceFromShortEntry = this.getEntryShort() - this.mostRecentBid;
            final int distanceFromLongEntry = this.mostRecentAsk - this.getEntryLong();
            
            if (distanceFromLongEntry < distanceFromShortEntry) {
                entryOrder = generateLongOrder(distanceFromLongEntry);
                if (null == entryOrder) {
                    this.longTradeRequest.cancelIfQueued();
                }
            } else {
                entryOrder = generateShortOrder(distanceFromShortEntry);
                if (null == entryOrder) {
                    this.shortTradeRequest.cancelIfQueued();
                }
            }
            
            this.orderPlaced = (entryOrder != null);

            return entryOrder;
        } catch(InsufficientDataException e) {
            throw new StrategyException(e);
        }
    }

    public ExitOrders getOrdersFromLong()
        throws StrategyException {
        // Surely we can cache these?
        final Date now = new Date();
        final Date marketClose = this.contractManager.getMarketCloseTime(now);
        final long timeToClose = marketClose.getTime() - now.getTime();

        if (timeToClose <= MARKET_CLOSE_HARD_STOP) {
            return null;
        }
        
        final int minutesInTrade = (int)Math.round((System.currentTimeMillis() - this.timeEnteredMarket) / 60000.0);
        int fastProfit = this.targetProfitPerMinute * minutesInTrade * 2;
        final int fastProfitLimit;
        
        fastProfit = Math.max(fastProfit, getMinimumProfit());
        this.exitOrdersPool.setLong(this.actualEntryPrice + Math.min(this.targetProfit, fastProfit),
            this.actualEntryPrice - this.targetProfit);
        
        return this.exitOrdersPool;
    }

    public ExitOrders getOrdersFromShort()
        throws StrategyException {
        // Surely we can cache these?
        final Date now = new Date();
        final Date marketClose = this.contractManager.getMarketCloseTime(now);
        final long timeToClose = marketClose.getTime() - now.getTime();

        if (timeToClose <= MARKET_CLOSE_HARD_STOP) {
            return null;
        }
        
        final int minutesInTrade = (int)Math.round((System.currentTimeMillis() - this.timeEnteredMarket) / 60000.0);
        int fastProfit = this.targetProfitPerMinute * minutesInTrade * 2;
        final int fastProfitLimit;
        
        fastProfit = Math.max(fastProfit, getMinimumProfit());
        this.exitOrdersPool.setLong(this.actualEntryPrice + Math.min(this.targetProfit, fastProfit),
            this.actualEntryPrice - this.targetProfit);
        
        return this.exitOrdersPool;
    }
    
    /* MUST only be called from the Swing event dispatcher thread. Panel
     * construction is done by this method rather than when the strategy
     * is initialised to ensure it's done in the correct thread.
     */
    public TFPanel getSwingComponent() {
        if (null == this.swingPanel) {
            TFPanel panel = new TFPanel();
            panel.createAndShowUI();
            this.swingPanel = panel;
        }
        
        return this.swingPanel;
    }

    public void handleEntryOrderStatus(final OrderAction action, final OrderStatus status, final int filled,
        final int remaining, final int avgFillPrice)
        throws StrategyException {
        if (status == OrderStatus.Submitted ||
            status == OrderStatus.Filled) {            
            if (filled == 0) {
                // Can happen on submitted states.
                return;
            }
            
            try {
                this.handleEntryOrderFilled(action, filled, avgFillPrice);
            } catch(InsufficientDataException e) {
                throw new StrategyException(e);
            }
        }
    }

    public void handleExitOrderStatus(final OrderAction action, final boolean isLimitOrder,
        final OrderStatus status, final int filled, final int remaining, final int avgFillPrice)
        throws StrategyException {
        this.timeExitedMarket = System.currentTimeMillis();
        
        if (0 == remaining) {
            this.longTradeRequest.cancelIfQueued();
            this.shortTradeRequest.cancelIfQueued();
        }
        
        return;
    }
    
    private void handleEntryOrderFilled(final OrderAction action, final int filled,
        final int avgFillPrice)
        throws InsufficientDataException {
        final int actualTradeDistance;
        final int projectedTradeDistance;
        
        this.timeEnteredMarket = System.currentTimeMillis();
        this.actualEntryPrice = avgFillPrice;
        
        if (action == OrderAction.BUY) {            
            projectedTradeDistance = this.getExitLong() - this.projectedEntryPrice;
            actualTradeDistance = this.getExitLong() - this.actualEntryPrice;
        } else {            
            projectedTradeDistance = this.projectedEntryPrice - this.getExitShort();
            actualTradeDistance = this.actualEntryPrice - this.getExitShort();
        }
        
        // Profit target is a multiple of trade distance
        this.targetProfit = (int)Math.round(actualTradeDistance * PROFIT_TARGET_MULTIPLIER);
        this.targetProfitPerMinute = (int)Math.ceil(this.targetProfit / EXPECTED_TRADE_DURATION_MINUTES);
    }

    public void handleTick(final TickData[] tickData)
        throws StrategyException {
        for (int tickIdx = 0; tickIdx < tickData.length; tickIdx++) {
            this.mostRecentUpdate = tickData[tickIdx].timeMillis;
            this.mostRecentBid = tickData[tickIdx].bidPrice;
            this.mostRecentAsk = tickData[tickIdx].askPrice;
        
            if (this.mostRecentBid == null ||
                this.mostRecentAsk == null) {
                // Don't attempt any further processing
                continue;
            }
        
            // Check if we've actually started filling in the minute bars yet.
            if (null == this.bidMinuteBar) {
                // We want to be at least close to the start of a minute
                // before we start recording data for the minute bar. In
                // this case, we check we're within 5 seconds of the minute
                // starting.
                final long secondsPastMinute = this.mostRecentUpdate % BAR_PERIOD;
                if (secondsPastMinute > 5000) {
                    return;
                }
            
                final Timestamp barStart = new Timestamp(this.mostRecentUpdate - secondsPastMinute);
                this.bidMinuteBar = new PartialPeriodicData(barStart, this.mostRecentBid);
                this.askMinuteBar = new PartialPeriodicData(barStart, this.mostRecentAsk);
            
                continue;
            }
        
            this.bidMinuteBar.update(this.mostRecentBid);
            this.askMinuteBar.update(this.mostRecentAsk);

            while ((this.mostRecentUpdate - this.bidMinuteBar.startTime.getTime()) > BAR_PERIOD) {
                final Timestamp barStart = new Timestamp(this.bidMinuteBar.startTime.getTime() + BAR_PERIOD);
        
                try {
                    this.bidBB.handlePeriodicData(this.bidMinuteBar.getCopy());
                    this.askBB.handlePeriodicData(this.askMinuteBar.getCopy());
                } catch(InsufficientDataException e) {
                    throw new StrategyException(e);
                }
                this.historicalBars++;

                this.bidMinuteBar.startNewBar(this.mostRecentBid);
                this.bidMinuteBar.startTime = new java.sql.Timestamp(this.bidMinuteBar.startTime.getTime() + BAR_PERIOD);
                this.askMinuteBar.startNewBar(this.mostRecentAsk);
                this.askMinuteBar.startTime = this.bidMinuteBar.startTime;
            
                if (this.historicalBars > this.totalHistoricalBars &&
                    null != this.configuration) {
                    try {
                        logStateInDB(this.bidMinuteBar.startTime);
                    } catch(DatabaseUnavailableException e) {
                        throw new StrategyException(e);
                    } catch(InsufficientDataException e) {
                        throw new StrategyException(e);
                    } catch(SQLException e) {
                        throw new StrategyException(e);
                    }
                }
            }
        }
    }

    private void logStateInDB(final Timestamp timestamp)
        throws DatabaseUnavailableException, InsufficientDataException, SQLException {
        final Connection dbConnection = this.configuration.getDBConnection();

        try {
            final PreparedStatement statement
                = dbConnection.prepareStatement("INSERT INTO TAR_AND_FEATHER "
                    + "(RUN_START, BAR_START, CONTRACT_ID, ENTER_LONG, "
                    + "ENTER_SHORT, EXIT_LONG, EXIT_SHORT, SMA_HIGH, "
                    + "STD_DEV_HIGH, SMA_LOW, STD_DEV_LOW) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            try {
                statement.setTimestamp(1, this.runStart);
                statement.setTimestamp(2, timestamp);
                statement.setInt(3, this.contractManager.getContractID());
                statement.setDouble(4, this.getEntryLong());
                statement.setDouble(5, this.getEntryShort());
                statement.setDouble(6, this.getExitLong());
                statement.setDouble(7, this.getExitShort());
                statement.setDouble(8, this.askBB.getSMA(1));
                statement.setDouble(9, this.askBB.getStandardDeviation(1));
                statement.setDouble(10, this.bidBB.getSMA(1));
                statement.setDouble(11, this.bidBB.getStandardDeviation(1));
                statement.executeUpdate();
            } finally {
                statement.close();
            }
        } finally {
            dbConnection.close();
        }
    }
    
    public void updateSwingComponent() {
        if (null != this.swingPanel) {
            this.swingPanel.repaint();
        }
    }
    
    public class TFPanel extends JPanel {
        private final int ROWS = 3;
        private final int COLUMNS = 6;
    
        // Not thread safe, must only be called from the Swing event dispatcher
        private DateFormat updateFormat = new SimpleDateFormat("HH:mm:ss");
        
        private final JLabel askPriceLabel = new JLabel("N/A");
        private final JLabel bidPriceLabel = new JLabel("N/A");
        private final JLabel mostRecentUpdateLabel = new JLabel();
        
        private final JLabel historyBarsLabel = new JLabel("0");
        private final JLabel marketCloseLabel = new JLabel("-");
        private final JLabel longShortLabel = new JLabel("");
        
        private final JLabel actualEntryLabel = new JLabel("N/A");
        private final JLabel actualProfitLabel = new JLabel("N/A");
        private final JLabel actualLossLabel = new JLabel("N/A");
        
        private         TFPanel() {
            super();
        }
        
        private void createAndShowUI() {
            final Border border = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
            final SpringLayout layout = new SpringLayout();
            final Date now = new java.util.Date();
            // Longest label for any field, although not actually used...
            final JLabel widestComponent;
            
            this.mostRecentUpdateLabel.setText(this.updateFormat.format(now));
            
            this.bidPriceLabel.setBorder(border);
            this.askPriceLabel.setBorder(border);
            this.mostRecentUpdateLabel.setBorder(border);
            
            this.historyBarsLabel.setBorder(border);
            this.marketCloseLabel.setBorder(border);
            this.longShortLabel.setBorder(border);
            
            this.actualEntryLabel.setBorder(border);
            this.actualProfitLabel.setBorder(border);
            this.actualLossLabel.setBorder(border);
            
            this.setLayout(layout);
            this.add(new JLabel("Bid price"));
            this.add(this.bidPriceLabel);
            this.add(new JLabel("Ask price"));
            this.add(this.askPriceLabel);
            this.add(new JLabel("Last update"));
            this.add(this.mostRecentUpdateLabel);
            
            this.add(new JLabel("History Bars"));
            this.add(this.historyBarsLabel);
            widestComponent = new JLabel("Market Close");
            this.add(widestComponent);
            this.add(this.marketCloseLabel);
            this.add(new JLabel("Long/Short"));
            this.add(this.longShortLabel);
            
            this.add(new JLabel("Entry"));
            this.add(this.actualEntryLabel);
            this.add(new JLabel("Profit"));
            this.add(this.actualProfitLabel);
            this.add(new JLabel("Loss"));
            this.add(this.actualLossLabel);

            final int totalComponents = ROWS * COLUMNS;
            final Spring xInterColumnSpring = Spring.constant(5, 20, 1000);
            final Spring xIntraColumnSpring = Spring.constant(5);
            final Spring yInterColumnSpring = Spring.constant(5);
            final Spring maxWidthSpring = Spring.width(widestComponent);
            final Spring maxHeightSpring = Spring.height(widestComponent);

            // Set all the components to the same size
            for (int i = 0; i < totalComponents; i++) {
                SpringLayout.Constraints constraints = layout.getConstraints(this.getComponent(i));

                constraints.setWidth(maxWidthSpring);
                constraints.setHeight(maxHeightSpring);
            }

            // Align the components into a grid.
            
            // We'll need the bottom-right constraint later, for setting the
            // size of the panel.
            SpringLayout.Constraints bottomRightConstraint = null;
            
            for (int rowIdx = 0; rowIdx < ROWS; rowIdx++) {
                final SpringLayout.Constraints previousEndRowConstraint = bottomRightConstraint;
                
                for (int colIdx = 0; colIdx < COLUMNS; colIdx++) {
                    final SpringLayout.Constraints constraints = layout.getConstraints(this.getComponent((COLUMNS * rowIdx) + colIdx));
                    
                    if (colIdx == 0) {
                        constraints.setX(xInterColumnSpring);
                    } else {
                        if ((colIdx % 2) == 0) {
                            constraints.setX(Spring.sum(bottomRightConstraint.getConstraint(SpringLayout.EAST),
                                xInterColumnSpring));
                        } else {
                            constraints.setX(Spring.sum(bottomRightConstraint.getConstraint(SpringLayout.EAST),
                                xIntraColumnSpring));
                        }
                    }

                    if (rowIdx == 0) {
                        constraints.setY(yInterColumnSpring);
                    } else {
                        constraints.setY(Spring.sum(previousEndRowConstraint.getConstraint(SpringLayout.SOUTH),
                            yInterColumnSpring));
                    }
                    bottomRightConstraint = constraints;
                }
            }

            // Give the panel a size so it actually renders!
            SpringLayout.Constraints panelConstraints = layout.getConstraints(this);
            panelConstraints.setConstraint(SpringLayout.SOUTH,
                Spring.sum(yInterColumnSpring, bottomRightConstraint.getConstraint(SpringLayout.SOUTH))
            );
            panelConstraints.setConstraint(SpringLayout.EAST,
                Spring.sum(xInterColumnSpring, bottomRightConstraint.getConstraint(SpringLayout.EAST))
            );
        }
        
        private String formatMarketClose(final Date marketClose) {
            long timeRemaining = marketClose.getTime() - System.currentTimeMillis();
            
            if (timeRemaining <= 0) {
                return "Passed";
            }
            
            final int hours = (int)(timeRemaining / (60 * 60 * 1000));
            
            if (hours > 24) {
                // Shouldn't happen, as markets close daily
                return "Over a day";
            }
            
            timeRemaining = timeRemaining % (60 * 60 * 1000);
            final int minutes = (int)(timeRemaining / (60 * 1000));
            
            timeRemaining = timeRemaining % (60 * 1000);
            final int seconds = (int)(timeRemaining / 1000);
            
            return Integer.toString(hours) + ":"
                + minutes + ":"
                + seconds;
        }
        
        // XXX: This was a lousy idea that mangles the entire error handling ability of
        // the code. Need to move this OUT of paint().
        @Override 
        public void paint(java.awt.Graphics g) {
            final ContractManager contractManager = CatchingDaggers.this.contractManager;
            final String askPrice;
            final String bidPrice;
            final Date updateTime = new Date(CatchingDaggers.this.mostRecentUpdate);
            
            if (CatchingDaggers.this.mostRecentBid != null) {
                bidPrice = contractManager.formatTicksAsPrice(CatchingDaggers.this.mostRecentBid);
            } else {
                bidPrice = null;
            }
            if (CatchingDaggers.this.mostRecentAsk != null) {
                askPrice = contractManager.formatTicksAsPrice(CatchingDaggers.this.mostRecentAsk);
            } else {
                askPrice = null;
            }
            
            this.bidPriceLabel.setText(null == bidPrice
                ? "N/A"
                : bidPrice.toString());
            this.askPriceLabel.setText(null == askPrice
                ? "N/A"
                : askPrice.toString());
            this.mostRecentUpdateLabel.setText(this.updateFormat.format(updateTime));
            
            this.historyBarsLabel.setText(Integer.toString(CatchingDaggers.this.historicalBars));
            this.marketCloseLabel.setText(formatMarketClose(CatchingDaggers.this.marketClose));
            if (CatchingDaggers.this.entryOrderPool.isLong()) {
                this.longShortLabel.setText("Long");
            } else {
                this.longShortLabel.setText("Short");
            }

            this.actualEntryLabel.setText(contractManager.formatTicksAsPrice(CatchingDaggers.this.projectedEntryPrice));
            this.actualProfitLabel.setText(contractManager.formatTicksAsPrice(CatchingDaggers.this.projectedExitLimitPrice));
            this.actualLossLabel.setText(contractManager.formatTicksAsPrice(CatchingDaggers.this.projectedExitStopPrice));

            super.paint(g);
        }
    }
}
