// $Id$
package org.lostics.foxquant.strategy;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;

import org.apache.log4j.Logger;

import org.lostics.foxquant.model.EntryOrder;
import org.lostics.foxquant.util.PriceTimeFrameBuffer;

public class CatchingDaggersDisplay extends JComponent {
    public static final Dimension MINIMUM_SIZE = new Dimension(300, 24);
    public static final Dimension PREFERRED_SIZE = new Dimension(800, 36);
    
    // Track mean maximum price over 5 second intervals, looking back 60
    // intervals (5 minutes).
    private PriceTimeFrameBuffer maxPrices = new PriceTimeFrameBuffer(5000, 60);
    // Track mean minimum price over 5 second intervals, looking back 60
    // intervals (5 minutes).
    private PriceTimeFrameBuffer minPrices = new PriceTimeFrameBuffer(5000, 60);
    
    private boolean validPrices = false;
    
    // Highest and lowest price seen in the latest data
    private HighLowPrice highLowPrices = new HighLowPrice();
    
    private int lastBid;
    private int lastAsk;
    
    private boolean validOrder = false;
    private boolean orderIsLong;
    private int entryPrice;
    private int exitStopPrice;
    private int exitLimitPrice;
    
    protected               CatchingDaggersDisplay() {
        this.setMinimumSize(MINIMUM_SIZE);
        this.setPreferredSize(PREFERRED_SIZE);
        this.setSize(PREFERRED_SIZE);
    }
    
    protected void update(final long currentTime,
        final Integer bid, final Integer ask, final EntryOrder entryOrder) {
        boolean needRepaint = false;
        
        synchronized(this) {            
            this.highLowPrices.reset();
        
            if (null != bid &&
                null != ask) {
                if (this.lastBid != bid) {
                    this.highLowPrices.addPrice(bid);
                    
                    this.lastBid = bid;
                    needRepaint = true;
                }
                if (this.lastAsk != ask) {
                    this.highLowPrices.addPrice(ask);
                    
                    this.lastAsk = ask;
                    needRepaint = true;
                }
                this.validPrices = true;
            } else {
                this.validPrices = false;
            }
            
            if (null != entryOrder) {
                if (this.orderIsLong != entryOrder.isLong()) {
                    this.orderIsLong = entryOrder.isLong();
                    needRepaint = true;
                }
                
                // Sure, we shouldn't ever see an extreme entry price, but
                // best to be sure
                this.highLowPrices.addPrice(entryOrder.getEntryLimitPrice());
                
                this.highLowPrices.addPrice(entryOrder.getExitLimitPrice());
                this.highLowPrices.addPrice(entryOrder.getExitStopPrice());
                    
                if (this.entryPrice != entryOrder.getEntryLimitPrice()) {
                    this.entryPrice = entryOrder.getEntryLimitPrice();
                    needRepaint = true;
                }
                if (this.exitLimitPrice != entryOrder.getExitLimitPrice()) {
                    this.exitLimitPrice = entryOrder.getExitLimitPrice();
                    needRepaint = true;
                }
                if (this.exitStopPrice != entryOrder.getExitStopPrice()) {
                    this.exitStopPrice = entryOrder.getExitStopPrice();
                    needRepaint = true;
                }
            
                this.validOrder = true;
            } else {
                this.validOrder = false;
            }
            
            if (this.highLowPrices.arePricesValid()) {
                this.maxPrices.add(currentTime, this.highLowPrices.getHighPrice());
                this.minPrices.add(currentTime, this.highLowPrices.getLowPrice());
            }
        }
        
        if (needRepaint) {
            this.repaint();
        }
    }
    
    public void paintComponent(final Graphics g) {
        super.paintComponent(g);
        
        final int x = this.getX();
        final int y = this.getY();
        final int width = this.getWidth();
        final int height = this.getHeight();
        final int redEnd = width * 3 / 4;
        final int blueStart = width / 4;
        
        for (int xOffset = 0; xOffset < width; xOffset++) {
            final int red = Math.max(30, 30 + 160 - (160 * xOffset / redEnd));
            final int blue = Math.max(30, 30 + (160 * (xOffset - blueStart)) / redEnd);
            final Color color = new Color(red, 20, blue);
            
            g.setColor(color);
            g.drawLine(x + xOffset, y, x + xOffset, y + height);
        }
        
        if (this.validPrices &&
            this.validOrder) {
            final int maxPrice;
            final int minPrice;
            final int priceRange;
            final int top;
            final int barHeight;
            final int mid;
            final int askOffset;
            final int bidOffset;
            
            synchronized(this) {
                // Get the highest of the recent prices, and the absolute most recent set
                maxPrice = Math.max(this.maxPrices.getHigh(), this.highLowPrices.getHighPrice());
                minPrice = Math.min(this.minPrices.getLow(), this.highLowPrices.getLowPrice());
                
                priceRange = maxPrice - minPrice;
                top = (int)Math.round(this.getHeight() / 3.0);
                barHeight = this.getHeight() - (top * 2);
                mid = (int)Math.round(this.getHeight() / 2.0);
                askOffset = (maxPrice - this.lastAsk) * this.getWidth() / priceRange;
                bidOffset = (priceRange - this.lastBid + minPrice) * this.getWidth() / priceRange;
            }
            
            g.setColor(Color.BLACK);
            g.fillRect(askOffset, mid - 1, bidOffset - askOffset, 3);
            g.fillRect(askOffset, top, 2, barHeight);
            g.fillRect(bidOffset - 2, top, 2, barHeight);
        }
    }
    
    private static class HighLowPrice extends Object {
        private int lowPrice;
        private int highPrice;
        private boolean pricesValid;
        
        private         HighLowPrice() {
            reset();
        }
        
        private void addPrice(final int price) {
            if (!this.pricesValid) {
                this.lowPrice = price;
                this.highPrice = price;
                this.pricesValid = true;
            } else {
                if (price < this.lowPrice) {
                    this.lowPrice = price;
                }
                if (price > this.highPrice) {
                    this.highPrice = price;
                }
            }
        }
        
        private boolean arePricesValid() {
            return this.pricesValid;
        }
        
        private int getHighPrice() {
            return this.highPrice;
        }
        
        private int getLowPrice() {
            return this.lowPrice;
        }
        
        private void reset() {
            this.pricesValid = false;
        }
    }
}
