// $Id: CatchingDaggersFactory.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.strategy;

import org.lostics.foxquant.model.Strategy;

import java.util.LinkedHashSet;

/**
 * Class for tracking strategies requests' for trading permission. This class
 * is NOT thread safe, so access must be externally synchronized.
 */
class TradingRequestQueue<T extends Strategy> extends Object {
    /**
     * Strategy currently given permission to go long on this currency,
     * or null if no currency has that permission.
     */
    private TradingRequest<T> topLong;
    
    /**
     * Strategy currently given permission to go short on this currency,
     * or null if no currency has that permission.
     */
    private TradingRequest<T> topShort;
    
    /**
     * Ordered list of strategies waiting for permission to go long, ordered
     * by when they joined the queue.
     */
    private LinkedHashSet<TradingRequest<T>> queueLong = new LinkedHashSet<TradingRequest<T>>();
    
    /**
     * Ordered list of strategies waiting for permission to go short, ordered
     * by when they joined the queue.
     */
    private LinkedHashSet<TradingRequest<T>> queueShort = new LinkedHashSet<TradingRequest<T>>();
    
    void addLongRequest(final TradingRequest request) {
        this.queueLong.add(request);
    }
    
    void addShortRequest(final TradingRequest request) {
        this.queueShort.add(request);
    }
    
    TradingRequest getLongTop() {
        return this.topLong;
    }
    
    TradingRequest getShortTop() {
        return this.topShort;
    }
    
    final Iterable<TradingRequest<T>> getLongQueue() {
        return this.queueLong;
    }
    
    final Iterable<TradingRequest<T>> getShortQueue() {
        return this.queueShort;
    }
    
    void removeLongRequest(final TradingRequest request) {
        this.queueLong.remove(request);
    }
    
    void removeShortRequest(final TradingRequest request) {
        this.queueShort.remove(request);
    }
    
    void setLongTop(final TradingRequest setRequest) {
        if (null != this.topLong) {
            throw new IllegalStateException("Attempt to set top long request while another request is already at the top.");
        }
        
        this.topLong = setRequest;
        this.queueLong.remove(this.topLong);
    }
    
    void setShortTop(final TradingRequest setRequest) {
        if (null != this.topShort) {
            throw new IllegalStateException("Attempt to set top short request while another request is already at the top.");
        }
        
        this.topShort = setRequest;
        this.queueShort.remove(this.topShort);
    }
}
