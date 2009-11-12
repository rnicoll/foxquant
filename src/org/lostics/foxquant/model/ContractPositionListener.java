// $Id: ContractPositionListener.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.model;

/**
 * Interface for listening to changes in contract position.
 */
public interface ContractPositionListener {
    public void positionChanged(final ContractPosition targetPositionType,
        final double targetPosition, final double actualPosition);
}
