// $Id: OrderAlreadyInProgressException.java 685 2009-11-08 01:12:26Z  $

package org.lostics.foxquant.model;

import com.ib.client.Contract;
import com.ib.client.Order;

/**
 * Thrown if contract manager cannot place a new order because an order is
 * already working.
 */
public class OrderAlreadyInProgressException extends Exception {
    /**
     * @param contract the contract that an order is already in progress for.
     * @param existingOrder the existing order, if details are known, that is
     * blocking a new order being placed. May be null.
     */
    public   OrderAlreadyInProgressException(final Contract contract,
        final int orderID) {
        super("Cannot place new order for contract "
            + contract.m_localSymbol + " because order #"
            + orderID + " is already in progress.");
    }
}
