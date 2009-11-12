// $Id: PositionIsFlatException.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.model;

/**
 * Represents an error caused by a position in the market being flat, for
 * example an attempt to place exit orders while not in a position.
 */
public class PositionIsFlatException extends Exception {
    public          PositionIsFlatException(final String message) {
        super(message);
    }
}
