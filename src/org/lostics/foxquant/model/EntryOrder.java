// $Id: EntryOrder.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.model;

/**
 * Stores the limit price the stategy wishes to enter the market at, and stop
 * prices that the strategy wishes to exit the market at if the entry order
 * succeeds.
 *
 * All prices are stored as a multiple of the minimum tick for the contract in
 * question.
 */
public class EntryOrder extends Object {
    private boolean transmit;
    private int entryLimitPrice;
    private int exitLimitPrice;
    private int exitStopPrice;
    private OrderAction orderAction;
    
    public EntryOrder() {
    }
    
    public void copy(final EntryOrder copyFrom) {
        this.transmit = copyFrom.transmit;
        this.entryLimitPrice = copyFrom.entryLimitPrice;
        this.exitLimitPrice = copyFrom.exitLimitPrice;
        this.exitStopPrice = copyFrom.exitStopPrice;
        this.orderAction = copyFrom.orderAction;
    }
    
    public int getEntryLimitPrice() {
        return this.entryLimitPrice;
    }
    
    public int getExitLimitPrice() {
        return this.exitLimitPrice;
    }
    
    public int getExitStopPrice() {
        return this.exitStopPrice;
    }
    
    public OrderAction getOrderAction() {
        return this.orderAction;
    }
    
    /**
     * Returns how far (in value terms) the entry price is from the given
     * price. This will return a negative value if the given price is below
     * the entry price for a long order, or above it for a short order.
     */
    public int getEntryDistance(final int fromPrice) {
        if (orderAction == OrderAction.BUY) {
            return fromPrice - this.entryLimitPrice;
        } else {
            return this.entryLimitPrice - fromPrice;
        }
    }
    
    /**
     * Returns the value difference between the entry price and the exit limit
     * price.
     */
    public int getProfitTarget() {
        if (orderAction == OrderAction.BUY) {
            return this.exitLimitPrice - this.entryLimitPrice;
        } else {
            return this.entryLimitPrice - this.exitLimitPrice;
        }
    }
    
    /**
     * Generates an order to go long.
     *
     * @param setEntryLimitPrice the maximum price to pay for the asset.
     * @param setExitLimitPrice the minimum price at which to take the profit.
     * @param setExitStopPrice the maximum price to remain in the market before
     * exiting at a loss. Used to control maximum risk.
     * @param setTransmit whether to enter this order into the market immediately.
     * This is provides guidance to the contract manager, but does not guarantee
     * that the order will not be transmitted before it's true, or will be once
     * is it set true. True to transmit, false not to.
     */
    public void setLong(final int setEntryLimitPrice,
        final int setExitLimitPrice, final int setExitStopPrice,
        final boolean setTransmit) {
        this.orderAction = OrderAction.BUY;
        this.entryLimitPrice = setEntryLimitPrice;
        this.exitLimitPrice = setExitLimitPrice;
        this.exitStopPrice = setExitStopPrice;
        
        this.transmit = setTransmit;
    }
    
    /**
     * Generates an order to go long. Stop-loss price is calculated as being
     * the same distance from the entry price, as the limit price is. For
     * example, if the entry price was 20 and the profit limit price 25, the
     * stop loss price would be 20 - (25 - 20) = 15.
     *
     * @param setEntryLimitPrice the maximum price to pay for the asset.
     * @param setExitLimitPrice the minimum price at which to take the profit.
     * The stop-loss price is generated using this price.
     * @param setTransmit whether to enter this order into the market immediately.
     * This is provides guidance to the contract manager, but does not guarantee
     * that the order will not be transmitted before it's true, or will be once
     * is it set true. True to transmit, false not to.
     */
    public void setLong(final int setEntryLimitPrice,
        final int setExitLimitPrice,
        final boolean setTransmit) {
        this.orderAction = OrderAction.BUY;
        this.entryLimitPrice = setEntryLimitPrice;
        this.exitLimitPrice = setExitLimitPrice;
        
        this.exitStopPrice = setEntryLimitPrice - getProfitTarget();
        
        this.transmit = setTransmit;
    }
    
    public void setShort(final int setEntryLimitPrice,
        final int setExitLimitPrice, final int setExitStopPrice,
        final boolean setTransmit) {
        this.orderAction = OrderAction.SELL;
        this.entryLimitPrice = setEntryLimitPrice;
        this.exitLimitPrice = setExitLimitPrice;
        this.exitStopPrice = setExitStopPrice;
        
        this.transmit = setTransmit;
    }
    
    public void setShort(final int setEntryLimitPrice,
        final int setExitLimitPrice, final boolean setTransmit) {
        this.orderAction = OrderAction.SELL;
        this.entryLimitPrice = setEntryLimitPrice;
        this.exitLimitPrice = setExitLimitPrice;

        this.exitStopPrice = setEntryLimitPrice + getProfitTarget();
        
        this.transmit = setTransmit;
    }
    
    public boolean shouldTransmit() {
        return this.transmit;
    }
}
