// $Id: CatchingDaggersFactory.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.strategy;

import org.lostics.foxquant.model.Strategy;

/**
 * Class for tracking strategies requests' for trading permission.
 */
public class TradingRequest<T extends Strategy> extends Object {
    public final String longSymbol;
    public final String shortSymbol;
    public final T strategy;
    
    public          TradingRequest(final T setStrategy,
        final String setLongSymbol, final String setShortSymbol) {
        this.strategy = setStrategy;
        this.longSymbol = setLongSymbol;
        this.shortSymbol = setShortSymbol;
    }
}
