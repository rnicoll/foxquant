// $Id: StrategyException.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.model;

/**
 * Represents an exceptional event occurring within an strategy.
 */
public class StrategyException extends Exception {
    public          StrategyException(final String message) {
        super(message);
    }
    
    public          StrategyException(final Throwable cause) {
        super(cause);
    }
    
    public          StrategyException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
