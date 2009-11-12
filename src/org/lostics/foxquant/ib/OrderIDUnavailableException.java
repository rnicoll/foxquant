// $Id: OrderIDUnavailableException.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.ib;

/**
 * Represents a failure to retrieve an order ID from the database.
 */
public class OrderIDUnavailableException extends Exception {
    public   OrderIDUnavailableException() {
        super("Unable to retrieve next order ID in sequence from database.");
    }
}
