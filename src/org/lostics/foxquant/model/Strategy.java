// $Id: Strategy.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JComponent;

import org.lostics.foxquant.ib.OrderIDUnavailableException;
import org.lostics.foxquant.indicator.InsufficientDataException;
import org.lostics.foxquant.database.DatabaseUnavailableException;

public interface Strategy {
    /**
     * To "kick start" a strategy, the contract manager feeds it recent
     * minute bars. These are not as good as per-tick data, but may be
     * used by a strategy to start more quickly than might otherwise be
     * possible.
     */
    public void backfillMinuteBar(final PeriodicData periodicData)
        throws StrategyException;
        
    public void handleTick(final TickData[] tickData)
        throws StrategyException;
        
    /**
     * @param avgFillPrice the average fill price, as a multiple of minimum tick size.
     */
    public void handleEntryOrderStatus(final OrderAction action,
        final OrderStatus status, int filled, int remaining, int avgFillPrice)
        throws StrategyException;

    /**
     * @param avgFillPrice the average fill price, as a multiple of minimum tick size.
     */
    public void handleExitOrderStatus(final OrderAction action, final boolean isLimitOrder,
        final OrderStatus status, int filled, int remaining, int avgFillPrice)
        throws StrategyException;

    /**
     * @return A price to enter the market at, by limit order. A null return
     * is taken as an instruction to not trade at any value.
     * The strategy runner MUST NOT cache the returned value directly, so
     * that the strategy can re-use the object next time ANY getOrders...()
     * method is called.
     */
    public EntryOrder getOrdersFromFlat()
        throws StrategyException;

    /**
     * @return Updated limit/stop order prices to exit the market at. A null
     * return is taken as indicating an instruction to leave at market price.
     * The strategy runner MUST NOT cache the returned value directly, so
     * that the strategy can re-use the object next time ANY getOrders...()
     * method is called.
     */
    public ExitOrders getOrdersFromLong()
        throws StrategyException;

    /**
     * @return Updated limit/stop order prices to exit the market at. A null
     * return is taken as indicating an instruction to leave at market price.
     * The strategy runner MUST NOT cache the returned value directly, so
     * that the strategy can re-use the object next time ANY getOrders...()
     * method is called.
     */
    public ExitOrders getOrdersFromShort()
        throws StrategyException;
     
    /**
     * Returns the JComponent that can be used to display this strategy's state.
     * This MUST only be called by the Swing thread, and the strategy must therefore
     * handle any locking issues as a result. See {@link #updateSwingComponent()}
     * for details on how the user interface updates should be handled.
     */
    public JComponent getSwingComponent();
    
    /**
     * For strategies that use a cross-strategy manager, this is used to
     * indicate that the strategy has the go-ahread to trade.
     */
    public void notifyTradingRequestApproved();
    
    /**
     * Tells the strategy that now is the right time to update the swing component,
     * if it needs to. This is called by the contract manager after all pending
     * work has been cleared.
     */
    public void updateSwingComponent();
}
