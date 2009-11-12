// $Id: TimeInForce.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.ib;

public enum TimeInForce {
    DAY,

    /** Good 'til cancelled. */
    GTC,

    /** Immediate or cancel. */
    IOC,

    /** Good 'til date. */
    GTD
}
