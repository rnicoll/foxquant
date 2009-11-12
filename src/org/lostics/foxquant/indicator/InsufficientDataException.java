// $Id: InsufficientDataException.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.indicator;

/**
 * Throw where an indicator has been asked to provide output before it has
 * been given sufficient input data, or if there are gaps in the input data.
 */
public class InsufficientDataException extends Exception {
    protected       InsufficientDataException(final String indicatorName,
        final int dataPointsFound, final int dataPointsRequired) {
        super("Insufficient data to calculate indicator \""
            + indicatorName + "\": have "
            + dataPointsFound + " of "
            + dataPointsRequired + " data points needed.");
    }
}
