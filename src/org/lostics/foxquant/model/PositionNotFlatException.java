// $Id: PositionNotFlatException.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.model;

/**
 * Represents an error caused by a position in the market not being flat, for
 * example an attempt to change the strategy a contract manager is running
 * while not flat.
 */
public class PositionNotFlatException extends Exception {
    public          PositionNotFlatException(final String message) {
        super(message);
    }
}
