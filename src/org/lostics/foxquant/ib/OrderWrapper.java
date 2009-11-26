// $Id: OrderWrapper.java 696 2009-11-10 00:06:49Z  $
package org.lostics.foxquant.ib;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;

import com.ib.client.ContractDetails;
import com.ib.client.Contract;
import com.ib.client.Order;

import org.apache.log4j.Logger;

import org.lostics.foxquant.model.EntryOrder;
import org.lostics.foxquant.model.ExitOrders;
import org.lostics.foxquant.model.OrderAction;
import org.lostics.foxquant.model.OrderStatus;
import org.lostics.foxquant.Configuration;

/**
 * Wraps around an Interactive Broker's order, providing tracking of
 * status, as well as storing order action in a more helpful format.
 * This class is NOT thread safe.
 */
class OrderWrapper extends Object {
    private final ConnectionManager connectionManager;
    private final ContractDetails contractDetails;
    private Date created = null;
    private Order order = null;
    private OrderAction action = null;
    private int price;
    private OrderStatus status = null;
    private OrderType type = null;
    
    public          OrderWrapper(final ConnectionManager setConnectionManager,
        final ContractDetails setContractDetails) {
        this.connectionManager = setConnectionManager;
        this.contractDetails = setContractDetails;
    }
    
    public void clearOrder() {
        this.order = null;
        this.action = null;
        this.created = null;
        this.status = null;
    }
    
    public Order createOrder(final Connection database,
        final OrderAction setAction, final OrderType setType,
        final int quantity, final int setPrice)
        throws OrderIDUnavailableException, SQLException {
        this.order = new Order();
        
        this.action = setAction;
        this.created = new Date();
        this.price = setPrice;
        this.type = setType;
        this.status = OrderStatus.PendingSubmit;
        
        order.m_action = this.action.toString();
        order.m_totalQuantity = quantity;
        order.m_orderType = setType.toString();
        if (this.type == OrderType.STP) {
            order.m_lmtPrice = 0.00000;
            order.m_auxPrice = this.price * getMinimumTick();
            order.m_triggerMethod = ConnectionManager.TRIGGER_METHOD_DOUBLE_BID_ASK;
        } else {
            order.m_lmtPrice = this.price * getMinimumTick();
            order.m_auxPrice = 0.00000;
            order.m_triggerMethod = ConnectionManager.TRIGGER_METHOD_DEFAULT;
        }
        order.m_tif = TimeInForce.DAY.toString();
        order.m_transmit = false;
        order.m_orderId = this.connectionManager.generateOrderID(database,
            this.contractDetails.m_summary, order);
            
        return order;
    }

    public OrderAction getAction() {
        return this.action;
    }
    
    public Date getCreated() {
        return this.created;
    }

    public int getID() {
        return this.order.m_orderId;
    }
    
    private double getMinimumTick() {
        return this.contractDetails.m_minTick;
    }
    
    /**
     * Returns the order this currently wraps. This is not a copy, and as such
     * any changes will be visible to anything else that uses the the same order.
     */
    public Order getOrder() {
        return this.order;
    }

    public OrderStatus getStatus() {
        return this.status;
    }

    public boolean getTransmitFlag() {
        return this.order.m_transmit;
    }
    
    public boolean hasValidOrder() {
        return null != this.order;
    }
    
    /**
     * Returns whether the order status is one that indicates the order has
     * finished being updated (for example "Filled" or "Cancelled").
     *
     * @returns true if the order status is final, false otherwise.
     */
    public boolean isStatusFinal() {
        return OrderStatus.Filled == this.status ||
            OrderStatus.Cancelled == this.status;
    }
    
    public void setStatus(final OrderStatus newStatus) {
        this.status = newStatus;
    }

    /**
     * Updates the price on the order.
     *
     * @returns true if the price changes, false otherwise.
     */
    public boolean setPrice(final int newPrice) {
        if (newPrice == this.price) {
            return false;
        }

        this.price = newPrice;
        if (this.type == OrderType.STP) {
            this.order.m_lmtPrice = 0.00000;
            this.order.m_auxPrice = this.price * this.getMinimumTick();
        } else {
            this.order.m_lmtPrice = this.price * this.getMinimumTick();
            this.order.m_auxPrice = 0.00000;
        }
        
        return true;
    }

    public void setTransmitFlag(final boolean newValue) {
        this.order.m_transmit = newValue;
    }
}
