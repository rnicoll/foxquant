// $Id$
package org.lostics.foxquant.strategy;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;

import org.apache.log4j.Logger;

import org.lostics.foxquant.model.EntryOrder;

public class CatchingDaggersDisplay extends JComponent {
    public static final Dimension MINIMUM_SIZE = new Dimension(300, 24);
    public static final Dimension PREFERRED_SIZE = new Dimension(800, 36);
    
    private boolean validPrices = false;
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
    
    protected void update(final Integer bid, final Integer ask, final EntryOrder entryOrder) {
        boolean needRepaint = false;
        
        synchronized(this) {
            if (null != bid &&
                null != ask) {
                if (this.lastBid != bid) {
                    this.lastBid = bid;
                    needRepaint = true;
                }
                if (this.lastAsk != ask) {
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
            final int red = Math.max(0, 255 - (255 * xOffset / redEnd));
            final int blue = Math.max(0, (255 * (xOffset - blueStart)) / redEnd);
            final Color color = new Color(red, 0, blue);
            
            g.setColor(color);
            g.drawLine(x + xOffset, y, x + xOffset, y + height);
        }
        
        if (this.validPrices &&
            this.validOrder) {
            final int maxPrice;
            final int minPrice;
            final int priceRange;
            final int top;
            final int bottom;
            final int mid;
            final int askOffset;
            final int bidOffset;
            
            synchronized(this) {
                if (this.orderIsLong) {
                    maxPrice = Math.max(this.lastAsk, this.exitLimitPrice);
                    minPrice = Math.min(this.lastBid, this.exitStopPrice);
                } else {
                    maxPrice = Math.max(this.lastAsk, this.exitStopPrice);
                    minPrice = Math.min(this.lastBid, this.exitLimitPrice);
                }
                
                priceRange = maxPrice - minPrice;
                top = (int)Math.round(this.getHeight() / 3.0);
                bottom = this.getHeight() - top;
                mid = (int)Math.round(this.getHeight() / 2.0);
                askOffset = (maxPrice - this.lastAsk) * this.getWidth() / priceRange;
                bidOffset = (priceRange - this.lastBid + minPrice) * this.getWidth() / priceRange;
            }
            
            g.setColor(Color.BLACK);
            g.drawLine(askOffset, mid, bidOffset, mid);
            g.drawLine(askOffset, top, askOffset, bottom);
            g.drawLine(bidOffset, top, bidOffset, bottom);
        }
    }
}
