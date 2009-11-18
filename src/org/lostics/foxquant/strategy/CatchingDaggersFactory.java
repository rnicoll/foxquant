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

public class CatchingDaggersFactory implements StrategyFactory<CatchingDaggers> {
    /** The minimum number of bars required before the strategy starts
     * working.
     */
    public static final int DEFAULT_HISTORICAL_BARS = 120;
    
    public static final double DEFAULT_SPREAD = 2;
    
    // Synchronize access to 'tradingRequests' on itself
    private final Map<String, TradingRequestQueue<CatchingDaggers>> tradingRequests
        = new HashMap<String, TradingRequestQueue<CatchingDaggers>>();
    
    private int historicalBars = DEFAULT_HISTORICAL_BARS;
    private double spread = DEFAULT_SPREAD;
    
    private Set<CatchingDaggers> runningStrategies = new HashSet<CatchingDaggers>();
    
    public          CatchingDaggersFactory() {
        // Nothing to do
    }
    
    public          CatchingDaggersFactory(final int setHistoricalBars,
        final double setSpread) {
        this();
        this.historicalBars = setHistoricalBars;
        this.spread = setSpread;
    }

    public void disposeStrategy(final CatchingDaggers strategy) {
        synchronized (this.runningStrategies) {
            this.runningStrategies.remove(strategy);
        }
        // XXX: Tell the strategy to clean up?
    }

    public CatchingDaggers getStrategy(final Configuration configuration,
        final ContractManager contractManager)
        throws StrategyAlreadyExistsException {
        final CatchingDaggers strategy = new CatchingDaggers(configuration, this, contractManager,
            this.historicalBars, this.spread);
        
        synchronized (this.runningStrategies) {
            if (this.runningStrategies.contains(strategy)) {
                throw new StrategyAlreadyExistsException(contractManager);
            }
            this.runningStrategies.add(strategy);
        }
        
        return strategy;
    }
    
    protected void cancelRequest(final TradingRequest request)
        throws StrategyException {
        CatchingDaggers topStrategy = null;
        
        synchronized (this.tradingRequests) {
            TradingRequestQueue<CatchingDaggers> longRequests = this.tradingRequests.get(request.longSymbol);
            TradingRequestQueue<CatchingDaggers> shortRequests = this.tradingRequests.get(request.shortSymbol);
            
            longRequests.removeLongRequest(request);
            shortRequests.removeShortRequest(request);
            // Check for a new request that can be fulfilled
        }
        
        if (null != topStrategy) {
            // XXX: topStrategy.notifyTradingRequestApproved();
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
    protected void request(final TradingRequest request)
        throws StrategyException {
        final boolean okayToGo;
        
        synchronized (this.tradingRequests) {
            TradingRequestQueue<CatchingDaggers> longRequests = this.tradingRequests.get(request.longSymbol);
            TradingRequestQueue<CatchingDaggers> shortRequests = this.tradingRequests.get(request.shortSymbol);
            
            if (null == longRequests) {
                longRequests = new TradingRequestQueue<CatchingDaggers>();
                this.tradingRequests.put(request.longSymbol, longRequests);
            }
            if (null == shortRequests) {
                shortRequests = new TradingRequestQueue<CatchingDaggers>();
                this.tradingRequests.put(request.shortSymbol, shortRequests);
            }
            
            if (longRequests.getLongTop() == null &&
                shortRequests.getShortTop() == null) {
                longRequests.setLongTop(request);
                shortRequests.setShortTop(request);
                okayToGo = true;
            } else {
                longRequests.addLongRequest(request);
                shortRequests.addShortRequest(request);
                okayToGo = false;
            }
        }
        
        if (okayToGo) {
            // XXX: topStrategy.notifyTradingRequestApproved();
        }
        
        return;
    }
    
    /**
     * Used by CatchingDaggers to request permission to go short on a currency.
     * If no other strategies are looking to go short on the given currency,
     * returns true, otherwise returns false. Either way, it adds the strategy
     * to the queue for that currency's short slot. If returns false, will
     * call notifyShortRequestApproved() on the strategy once a slot is available,
     * unless cancelRequestShort() is called on the factory first.
     */
    protected void requestShort(final String symbol, final CatchingDaggers requestor)
        throws StrategyException {
        synchronized (this.tradingRequests) {
            TradingRequestQueue<CatchingDaggers> requests = this.tradingRequests.get(symbol);
            
            if (null == requests) {
                requests = new TradingRequestQueue<CatchingDaggers>();
                this.tradingRequests.put(symbol, requests);
            }
            
            requests.addShortRequest(new TradingRequest(requestor, "", symbol));
        }
        
        return;
    }
}
