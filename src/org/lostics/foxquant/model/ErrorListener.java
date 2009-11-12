// $Id: ErrorListener.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.model;

public interface ErrorListener {
    public void error(final int id, final int errorCode, final String errorString);

    public void error(final String errorString);

    public void error(final Exception cause);
}
