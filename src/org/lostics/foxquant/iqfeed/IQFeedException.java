// $Id: IQFeedException.java 703 2009-11-11 02:32:34Z jrn $
package org.lostics.foxquant.iqfeed;

public class IQFeedException extends Exception {
    public          IQFeedException(final String message) {
        super(message);
    }
    
    public          IQFeedException(final Throwable t) {
        super(t);
    }
    
    public          IQFeedException(final String message, final Throwable t) {
        super(message, t);
    }
}