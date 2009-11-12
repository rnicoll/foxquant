// $Id: CatchingDaggersFactory.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.strategy;

import java.util.HashMap;
import java.util.Map;

import org.lostics.foxquant.model.ContractManager;
import org.lostics.foxquant.model.StrategyFactory;
import org.lostics.foxquant.model.Strategy;
import org.lostics.foxquant.Configuration;

/**
 * BB band bounceback strategy with entry confirmation from long term
 * SMA. Works well with 3 minute bars on currencies.
 */
public class CatchingDaggersFactory implements StrategyFactory {
    /** The minimum number of bars required before the strategy starts
     * working.
     */
    public static final int DEFAULT_HISTORICAL_BARS = 120;
    
    public static final double DEFAULT_SPREAD = 2;
    
    // Synchronize access to these maps, on themselves.
    private final Map<String, CatchingDaggers> longStrategies = new HashMap<String, CatchingDaggers>();
    private final Map<String, CatchingDaggers> shortStrategies = new HashMap<String, CatchingDaggers>();
    
    private int historicalBars = DEFAULT_HISTORICAL_BARS;
    private double spread = DEFAULT_SPREAD;
    
    public          CatchingDaggersFactory() {
        // Nothing to do
    }
    
    public          CatchingDaggersFactory(final int setHistoricalBars,
        final double setSpread) {
        this();
        this.historicalBars = setHistoricalBars;
        this.spread = setSpread;
    }

    public void disposeStrategy(final Strategy strategy) {
        return;
    }

    public Strategy getStrategy(final Configuration configuration,
        final ContractManager contractManager) {
        return new CatchingDaggers(configuration, contractManager,
            this.historicalBars, this.spread);
    }
    
    protected boolean requestLong(final String symbol, final CatchingDaggers requestor) {
        synchronized (this.longStrategies) {
            final CatchingDaggers strategy = this.longStrategies.get(symbol);
            if (null == strategy) {
                this.longStrategies.put(symbol, requestor);
                return true;
            } else {
                return false;
            }
        }
    }
    
    protected boolean requestShort(final String symbol, final CatchingDaggers requestor) {
        synchronized (this.shortStrategies) {
            final CatchingDaggers strategy = this.shortStrategies.get(symbol);
            if (null == strategy) {
                this.shortStrategies.put(symbol, requestor);
                return true;
            } else {
                return false;
            }
        }
    }
}
