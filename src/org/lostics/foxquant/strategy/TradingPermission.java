// $Id$
package org.lostics.foxquant.strategy;

/**
 * Enum for the different states a strategy's trading permissions can be in,
 * for each currency in its currency pair.
 */
public enum TradingPermission {
    LONG_APPROVED,
    LONG_REQUESTED,
    NEUTRAL,
    SHORT_REQUESTED,
    SHORT_APPROVED
}
