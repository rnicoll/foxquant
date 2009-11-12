// $Id: StrategyFactory.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.model;

import org.lostics.foxquant.Configuration;

/**
 * Responsible for generating strategy objects. This is used to provide an easy
 * object to be passed to contract managers during set up, and to give strategies
 * a single shared object to handle any shared work.
 */
public interface StrategyFactory {
    public void disposeStrategy(final Strategy strategy);

    public Strategy getStrategy(final Configuration configuration, final ContractManager contractManager);
}
