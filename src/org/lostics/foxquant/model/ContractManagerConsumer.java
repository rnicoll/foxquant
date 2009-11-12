// $Id: ContractManagerConsumer.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.model;

import com.ib.client.Contract;

/**
 * Interface for classes that accept contract managers once they have been
 * constructed.
 *
 * Constructing and backfilling a contract manager takes a long time, and
 * may depend on waiting for results from the broker. Given this, we
 * don't pass the contract manager directly back, but instead pass it to
 * the given contract manager consumer.
 */
public interface ContractManagerConsumer {
    public void contractManagerReady(final ContractManager contractManager);
}
