// $Id$
package org.lostics.foxquant.strategy;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;

import org.apache.log4j.Logger;

public class CatchingDaggersDisplay extends JComponent {
    public static final Dimension MINIMUM_SIZE = new Dimension(300, 30);
    public static final Dimension PREFERRED_SIZE = new Dimension(800, 42);
    
    protected               CatchingDaggersDisplay() {
        this.setMinimumSize(MINIMUM_SIZE);
        this.setPreferredSize(PREFERRED_SIZE);
        this.setSize(PREFERRED_SIZE);
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
    }
}
