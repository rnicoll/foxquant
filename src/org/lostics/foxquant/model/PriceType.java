// $Id: PriceType.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.model;

/**
 * Different types of data stored in a PeriodicData entry. Used to allow
 * indicators to be based on different data types by parameter rather than
 * code change.
 */
public enum PriceType {
    HIGH,
    LOW,
    CLOSE,
    OPEN,
    HIGH_LOW_MEAN
}
