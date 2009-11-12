// $Id: TickConsumerState.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.ib;

import org.lostics.foxquant.model.TickData;

class TickConsumerState {
    private final TickData tickData = new TickData();
    private final TWSContractManager contractManager;

    protected       TickConsumerState(final TWSContractManager setContractManager) {
        this.contractManager = setContractManager;
    }

    protected TWSContractManager getContractManager() {
        return this.contractManager;
    }

    protected TickData getCopyOfTickData() {
        return tickData.getCopy();
    }

    protected void setAskPrice(final Double newPrice) {
        this.tickData.timeMillis = System.currentTimeMillis();
        
        if (newPrice != null) {
            this.tickData.askPrice = (int)(newPrice / contractManager.getMinimumTick());
        } else {
            this.tickData.askPrice = null;
        }
    }

    protected void setAskSize(final Integer newSize) {
        this.tickData.timeMillis = System.currentTimeMillis();
        this.tickData.askSize = newSize;
    }

    protected void setBidPrice(final Double newPrice) {
        this.tickData.timeMillis = System.currentTimeMillis();
        
        if (newPrice != null) {
            this.tickData.bidPrice = (int)(newPrice / contractManager.getMinimumTick());
        } else {
            this.tickData.bidPrice = null;
        }
    }

    protected void setBidSize(final Integer newSize) {
        this.tickData.timeMillis = System.currentTimeMillis();
        this.tickData.bidSize = newSize;
    }

    protected void setTime(final long newTime) {
        this.tickData.timeMillis = newTime;
    }
}
