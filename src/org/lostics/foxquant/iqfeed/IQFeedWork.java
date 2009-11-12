// $Id: IQFeedWork.java 703 2009-11-11 02:32:34Z jrn $
package org.lostics.foxquant.iqfeed;

import java.io.IOException;

public interface IQFeedWork {
    public void doWork(final IQFeedGateway setGateway)
        throws InterruptedException, IOException, IQFeedException;
}