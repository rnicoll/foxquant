// $Id: NotConnectedToTWSException.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.ib;

public class NotConnectedToTWSException extends IllegalStateException {
    protected   NotConnectedToTWSException() {
        super("Not connected to TWS.");
    }

    protected   NotConnectedToTWSException(final String message) {
        super(message);
    }
}
