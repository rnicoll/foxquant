// $Id$
package org.lostics.foxquant.strategy;

import org.lostics.foxquant.model.StrategyException;
import org.lostics.foxquant.model.Strategy;

/**
 * Class for tracking strategies requests' for trading permission.
 */
public class TradingRequest extends Object {
    // We have to use a pair of booleans, rather than an enum, to
    // avoid threading issues (we don't want approval coming through
    // to mess with indications of whether the request is queued).
    private boolean isQueued = false;
    private boolean isApproved = false;

    public final String longSymbol;
    public final String shortSymbol;
    public final CatchingDaggersFactory factory;
    public final CatchingDaggers strategy;
    
    public          TradingRequest(final CatchingDaggersFactory setFactory,
        final CatchingDaggers setStrategy, final String setLongSymbol, final String setShortSymbol) {
        this.factory = setFactory;
        this.strategy = setStrategy;
        this.longSymbol = setLongSymbol;
        this.shortSymbol = setShortSymbol;
    }
    
    public boolean equals(final Object o) {
        final TradingRequest requestB = (TradingRequest)o;
        
        return requestB.longSymbol.equals(requestB.longSymbol) &&
            this.shortSymbol.equals(requestB.shortSymbol);
    }
    
    public int hashCode() {
        int hashCode = 1;
        
        hashCode = (hashCode * 31) + this.longSymbol.hashCode();
        hashCode = (hashCode * 31) + this.shortSymbol.hashCode();
        
        return hashCode;
    }
    
    /**
     * Removes this trading request from the factory's queue for the given currency
     * pair, if it has been added previously. This MUST only be called from a single
     * thread (the contract manager via the strategy).
     *
     * @return true if removed, false if not. This is NOT the same as whether it
     * is on the queue, only whether the state has been changed.
     */
    public boolean cancelIfQueued()
        throws StrategyException {
        if (this.isQueued) {
            this.factory.cancelRequest(this);
            this.isApproved = false;
            this.isQueued = false;
            
            return true;
        }
        
        return false;
    }
    
    public boolean isApproved() {
        return this.isApproved;
    }
    
    public void notifyRequestApproved() {
        this.isApproved = true;
        this.strategy.notifyTradingRequestApproved();
    }
    
    /**
     * Adds this trading request to the factory's queue for the given currency
     * pair, if it hasn't already been added. This MUST only be called from a single
     * thread (the contract manager via the strategy).
     *
     * @return true if added, false if not. This is NOT the same as whether it
     * is on the queue, only whether the state has been changed.
     */
    public boolean queueIfInactive()
        throws StrategyException {
        if (!this.isQueued) {
            this.isApproved = false;
            this.factory.request(this);
            this.isQueued = true;
            
            return true;
        }
        
        return false;
    }
}
