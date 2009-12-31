package org.lostics.foxquant.ib;

import org.lostics.foxquant.model.OrderType;

/**
 * Exception indicating that the code has encountered an order type it cannot
 * handle. Normally this would mean part of the code has been modified without
 * checking for side-effects.
 */
public class UnexpectedOrderTypeException extends Exception {
    protected   UnexpectedOrderTypeException(final OrderType type) {
        super("Encountered unexpected order type \""
            + type + "\".");
    }
}
