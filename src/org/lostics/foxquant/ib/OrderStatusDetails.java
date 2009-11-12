// $Id: OrderStatusDetails.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.ib;

import org.lostics.foxquant.model.OrderStatus;

public class OrderStatusDetails extends Object {
    private final int orderID;
    private final OrderStatus orderStatus;
    private final int filled;
    private final int remaining;
    private final double avgFillPrice;
    private final double lastFillPrice;
    private final String whyHeld;
    
    public OrderStatusDetails(final int setOrderID,
        final OrderStatus setOrderStatus,
        final int setFilled, final int setRemaining,
        final double setAvgFillPrice, final double setLastFillPrice,
        final String setWhyHeld) {
        this.orderID = setOrderID;
        this.orderStatus = setOrderStatus;
        this.filled = setFilled;
        this.remaining = setRemaining;
        this.avgFillPrice = setAvgFillPrice;
        this.lastFillPrice = setLastFillPrice;
        this.whyHeld = setWhyHeld;
    }
    
    public double getAvgFillPrice() {
        return this.avgFillPrice;
    }
    
    public int getFilled() {
        return this.filled;
    }
    
    public double getLastFillPrice() {
        return this.lastFillPrice;
    }
    
    public int getOrderID() {
        return this.orderID;
    }
    
    public OrderStatus getOrderStatus() {
        return this.orderStatus;
    }
    
    public int getRemaining() {
        return this.remaining;
    }
}
