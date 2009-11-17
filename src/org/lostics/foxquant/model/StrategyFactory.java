// $Id: StrategyFactory.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.model;

import org.lostics.foxquant.Configuration;

/**
 * Responsible for generating strategy objects. This is used to provide an easy
 * object to be passed to contract managers during set up, and to give strategies
 * a single shared object to handle any shared work.
 */
public interface StrategyFactory<T extends Strategy> {
    public void disposeStrategy(final T strategy);

    public T getStrategy(final Configuration configuration, final ContractManager contractManager)
        throws StrategyAlreadyExistsException;
}
