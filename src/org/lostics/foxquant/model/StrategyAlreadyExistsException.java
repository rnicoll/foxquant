// $Id: StrategyException.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.model;

/**
 * Represents that a strategy cannot be created because an instance of it
 * is already active. This is mostly a sanity check to ensure multiple contract
 * managers are not running with the same cntract.
 */
public class StrategyAlreadyExistsException extends Exception {
    public          StrategyAlreadyExistsException(final ContractManager contractManager) {
        super("Strategy already exists for contract "
            + contractManager.getContract() + ".");
    }
}
