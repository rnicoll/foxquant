// $Id: ExitOrders.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.model;

/**
 * Stores the limit and stop prices that the strategy wishes to exit the
 * market at.
 *
 * All prices are stored as a multiple of the minimum tick for the contract in
 * question.
 */
public class ExitOrders extends Object {
    private int limitPrice = 0;
    private int stopPrice = 0;
    
    /**
     * Initialise a blank exit orders pair. See setLong() and setShort() for
     * setting the prices.
     */
    public      ExitOrders() {
    }
    
    public void copy(final ExitOrders copyFrom) {
        this.limitPrice = copyFrom.limitPrice;
        this.stopPrice = copyFrom.stopPrice;
    }
    
    public int getExitLimitPrice() {
        return this.limitPrice;
    }
    
    public int getExitStopPrice() {
        return this.stopPrice;
    }
    
    public void setLong(final int setLimitPrice, final int setStopPrice)
        throws IllegalArgumentException {
        if (setLimitPrice < setStopPrice) {
            throw new IllegalArgumentException("Limit price for exiting a long position must be greater than the stop price.");
        }
        this.limitPrice = setLimitPrice;
        this.stopPrice = setStopPrice;
    }
    
    public void setShort(final int setLimitPrice, final int setStopPrice)
        throws IllegalArgumentException {
        if (setLimitPrice > setStopPrice) {
            throw new IllegalArgumentException("Limit price for exiting a short position must be less than the stop price.");
        }
        this.limitPrice = setLimitPrice;
        this.stopPrice = setStopPrice;
    }
}
