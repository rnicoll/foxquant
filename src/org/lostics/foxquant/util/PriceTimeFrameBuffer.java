// $Id$
package org.lostics.foxquant.util;

/**
 * A buffer which automatically generates a list of average prices during a
 * given price interval (typically 1-5 seconds). Useful for tasks such as
 * looking at high/low within the last 5 minutes in a rolling manner.
 *
 * Not thread safe, must be externally synchronized.
 */
public class PriceTimeFrameBuffer extends Object {
    private final int shortInterval;
    
    /** A ring buffer of mean prices during each interval. */
    private final int[] intervals;
    
    private int intervalsFilled = 0;
    
    /** The start point for iterating through the intervals;
     * remember to loop around at the end.
     */
    private int intervalsOffset = 0;

    private long currentBarStartTime = 0;
    private int currentBar = 0;
    private int currentBarSum = 0;
    private int currentBarCount = 0;

    public      PriceTimeFrameBuffer(final int shortTimeInterval, final int timeIntervalCount) {
        this.shortInterval = shortTimeInterval;
        this.intervals = new int[timeIntervalCount];
    }
    
    public void add(final long currentTime, final int price) {
        if (intervalsFilled == 0 &&
            currentBarSum == 0) {
            currentBarStartTime = currentTime;
        } else {
            long intervalPassed = currentTime - currentBarStartTime;
            
            while (intervalPassed > this.shortInterval) {
                if (this.intervalsFilled < this.intervals.length) {
                    this.intervals[this.intervalsFilled] = this.currentBar;
                    this.intervalsFilled++;
                } else {
                    this.intervals[this.intervalsOffset] = this.currentBar;
                    this.intervalsOffset = (this.intervalsOffset + 1) % this.intervals.length;
                }
            
                this.currentBarStartTime += this.shortInterval;
                intervalPassed = currentTime - this.currentBarStartTime;
                
                this.currentBarSum = 0;
                this.currentBarCount = 0;
            }
        }
        
        this.currentBarSum += price;
        this.currentBarCount++;
        this.currentBar = (int)Math.round(this.currentBarSum / this.currentBarCount);
    }
    
    public int getHigh() {
        if (intervalsFilled == 0 &&
            currentBarSum == 0) {
            throw new IllegalStateException("PriceTimeFrameBuffer.getHigh() called before any data has been provided.");
        }
        
        int high = currentBar;
        
        for (int barIdx = 0; barIdx < this.intervalsFilled; barIdx++) {
            int val = this.intervals[(barIdx + intervalsOffset) % this.intervalsFilled];
            
            if (val > high) {
                high = val;
            }
        }
        
        return high;
    }
    
    public int getLow() {
        if (intervalsFilled == 0 &&
            currentBarSum == 0) {
            throw new IllegalStateException("PriceTimeFrameBuffer.getLow() called before any data has been provided.");
        }
        
        int low = currentBar;
        
        for (int barIdx = 0; barIdx < this.intervalsFilled; barIdx++) {
            int val = this.intervals[(barIdx + intervalsOffset) % this.intervalsFilled];
            
            if (val < low) {
                low = val;
            }
        }
        
        return low;
    }
}
