// $Id: CatchingDaggersFactory.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.strategy;

import org.lostics.foxquant.model.Strategy;

/**
 * Class for tracking strategies requests' for trading permission.
 */
public class TradingRequest<T extends Strategy> extends Object {
    public final String longCurrency;
    public final String shortCurrency;
    public final T strategy;
    
    public          TradingRequest(final T setStrategy,
        final String setLongCurrency, final String setShortCurrency) {
        this.strategy = setStrategy;
        this.longCurrency = setLongCurrency;
        this.shortCurrency = setShortCurrency;
    }
}
