// $Id: CatchingDaggersFactory.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.strategy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.lostics.foxquant.model.ContractManager;
import org.lostics.foxquant.model.StrategyAlreadyExistsException;
import org.lostics.foxquant.model.StrategyException;
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
    private final Map<String, LinkedHashSet<CatchingDaggers>> longStrategies = new HashMap<String, LinkedHashSet<CatchingDaggers>>();
    private final Map<String, LinkedHashSet<CatchingDaggers>> shortStrategies = new HashMap<String, LinkedHashSet<CatchingDaggers>>();
    
    private int historicalBars = DEFAULT_HISTORICAL_BARS;
    private double spread = DEFAULT_SPREAD;
    
    private Set<Strategy> runningStrategies = new HashSet<Strategy>();
    
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
        synchronized (this.runningStrategies) {
            this.runningStrategies.remove(strategy);
        }
        // XXX: Tell the strategy to clean up?
    }

    public Strategy getStrategy(final Configuration configuration,
        final ContractManager contractManager)
        throws StrategyAlreadyExistsException {
        final Strategy strategy = new CatchingDaggers(configuration, this, contractManager,
            this.historicalBars, this.spread);
        
        synchronized (this.runningStrategies) {
            if (this.runningStrategies.contains(strategy)) {
                throw new StrategyAlreadyExistsException(contractManager);
            }
            this.runningStrategies.add(strategy);
        }
        
        return strategy;
    }
    
    /**
     * Class private method for removing a strategy from a request queue (list),
     * and retrieving the strategy at the front of the queue ONLY if it changes.
     * MUST be called fron within a synchronized block on the map being
     * affected.
     *
     * @return a strategy to notify it has perimission to go long/short. Will be
     * null if EITHER no strategies are waiting for permission OR if the strategy
     * at the front of the queue has not changed.
     */
    private CatchingDaggers cancelRequestAndGetFirst(final LinkedHashSet<CatchingDaggers> strategies, final CatchingDaggers requestor)
        throws StrategyException {
        final Iterator<CatchingDaggers> iterator;
        final Strategy originalNextStrategy;
        final Strategy originalTopStrategy;
        
        if (null == strategies) {
            throw new StrategyException("Cancellation of request to go long, while no strategies have outstanding requests.");
        }
        
        if (strategies.size() == 0) {
            // Assume a concurrency issue, and ignore.
            return null;
        }
        
        iterator = strategies.iterator();
        originalTopStrategy = iterator.next();
        originalNextStrategy = iterator.next();
        
        if (!strategies.remove(requestor)) {
            // Assume a concurrency issue, and ignore.
            return null;
        }
        if (originalTopStrategy.equals(requestor) &&
            strategies.size() > 0) {
            // We've just remove the strategy with the highest priority,
            // check for another to notify of this change.
            return (CatchingDaggers)originalNextStrategy;
        }
        
        return null;
    }
    
    protected void cancelRequestLong(final String symbol, final CatchingDaggers requestor)
        throws StrategyException {
        CatchingDaggers topStrategy = null;
        
        synchronized (this.longStrategies) {
            final LinkedHashSet<CatchingDaggers> strategies = this.longStrategies.get(symbol);
            
            topStrategy = cancelRequestAndGetFirst(strategies, requestor);
        }
        
        if (null != topStrategy) {
            // XXX: topStrategy.notifyLongRequestApproved();
        }
    }
    
    protected void cancelRequestShort(final String symbol, final CatchingDaggers requestor)
        throws StrategyException {
        CatchingDaggers topStrategy = null;
        
        synchronized (this.shortStrategies) {
            final LinkedHashSet<CatchingDaggers> strategies = this.shortStrategies.get(symbol);
            
            topStrategy = cancelRequestAndGetFirst(strategies, requestor);
        }
        
        if (null != topStrategy) {
            // XXX: topStrategy.notifyShortRequestApproved();
        }
    }
    
    
    /**
     * Used by CatchingDaggers to request permission to go long on a currency.
     * If no other strategies are looking to go long on the given currency,
     * returns true, otherwise returns false. Either way, it adds the strategy
     * to the queue for that currency's long slot. If returns false, will
     * call notifyLongRequestApproved() on the strategy once a slot is available,
     * unless cancelRequestLong() is called on the factory first.
     */
    protected boolean requestLong(final String symbol, final CatchingDaggers requestor)
        throws StrategyException {
        synchronized (this.longStrategies) {
            LinkedHashSet<CatchingDaggers> strategies = this.longStrategies.get(symbol);
            
            if (null == strategies) {
                strategies = new LinkedHashSet<CatchingDaggers>();
                this.longStrategies.put(symbol, strategies);
            }
            
            strategies.add(requestor);
            if (strategies.size() == 1) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Used by CatchingDaggers to request permission to go short on a currency.
     * If no other strategies are looking to go short on the given currency,
     * returns true, otherwise returns false. Either way, it adds the strategy
     * to the queue for that currency's short slot. If returns false, will
     * call notifyShortRequestApproved() on the strategy once a slot is available,
     * unless cancelRequestShort() is called on the factory first.
     */
    protected boolean requestShort(final String symbol, final CatchingDaggers requestor)
        throws StrategyException {
        synchronized (this.shortStrategies) {
            LinkedHashSet<CatchingDaggers> strategies = this.shortStrategies.get(symbol);
            
            if (null == strategies) {
                strategies = new LinkedHashSet<CatchingDaggers>();
                this.shortStrategies.put(symbol, strategies);
            }
            
            strategies.add(requestor);
            if (strategies.size() == 1) {
                return true;
            }
        }
        
        return false;
    }
}
