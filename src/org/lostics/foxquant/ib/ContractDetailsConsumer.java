// $Id: ContractDetailsConsumer.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.ib;

import com.ib.client.ContractDetails;

/**
 * Interface for classes that accept contract details once they have been
 * returned from TWS.
 *
 * There is currently no way of attributing contract detail errors to specific
 * requests, using the TWS API. Given this, there is no error reporting method
 * in this interface. Use a timeout if you have to.
 */
public interface ContractDetailsConsumer {
    /**
     * Called at the end of the response with contract details.
     */
    public void contractDetailsEnd();
    
    /**
     * Receive contract details from TWS. Please note that this may be called
     * more than once for the same contract details in rare circumstances.
     */
    public void contractDetailsReady(final ContractDetails contractDetails);
}
