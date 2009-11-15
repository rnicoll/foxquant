// $Id: IQFeedGateway.java 706 2009-11-11 10:41:13Z jrn $
package org.lostics.foxquant.iqfeed;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.Date;
import java.util.TimeZone;

import com.ib.client.ContractDetails;

import com.iqfeed.IQ_32;

import org.apache.log4j.Logger;

import org.lostics.foxquant.database.DatabaseThread;
import org.lostics.foxquant.model.HistoricBarSize;
import org.lostics.foxquant.model.HistoricalDataConsumer;
import org.lostics.foxquant.model.HistoricalDataSource;
import org.lostics.foxquant.model.PeriodicData;

public class IQFeedGateway extends Thread implements HistoricalDataSource {
    public static final String MSG_END = "!ENDMSG!";
    public static final String MSG_NO_DATA = "!NO_DATA!";

    public static final int HISTORICAL_DATA_PORT = 9100;
    public static final TimeZone TIMEZONE = TimeZone.getTimeZone("EST");

    private static final Logger log = Logger.getLogger(IQFeedGateway.class);

    private final DatabaseThread databaseThread;
    private final IQ_32 iq32;
    private final InetAddress localhost;
    private boolean stop = false;
    private final String version;
    
    private Socket historicalSocket;
    private BufferedReader historicalReader;
    private BufferedWriter historicalWriter;
    
    private final BlockingQueue<IQFeedWork> workQueue = new ArrayBlockingQueue<IQFeedWork>(100);

    public          IQFeedGateway(final DatabaseThread setDatabaseThread,
        final String setVersion)
        throws IQFeedException {
        this.databaseThread = setDatabaseThread;
        this.version = setVersion;
        
        try {
            this.localhost = InetAddress.getByName("localhost");
        } catch(UnknownHostException e) {
            throw new IQFeedException(e);
        }
        
        this.setName("IQFeed");
        this.iq32 = new InternalIQ();
    }
    
    public void close() {
        this.stop = true;
        this.interrupt();
    }
    
    public DatabaseThread getDatabaseThread() {
        return this.databaseThread;
    }
    
    /**
     * Returns the input stream used for reading replies from the lookup
     * service. MUST only be called from within IQFeedWork implementations
     * called by the IQFeed thread.
     */
    protected BufferedReader getLookupReader() {
        return this.historicalReader;
    }
    
    /**
     * Returns the input stream used for writing messages to the lookup
     * service. MUST only be called from within IQFeedWork implementations
     * called by the IQFeed thread.
     */
    protected BufferedWriter getLookupWriter() {
        return this.historicalWriter;
    }
    
    public void requestHistoricalData(final HistoricalDataConsumer backfillHandler,
        final ContractDetails contractDetails, final Date startDate, final Date endDate,
        final HistoricBarSize barSize)
        throws IllegalArgumentException, InterruptedException {
        final HistoricDataRequest request = new HistoricDataRequest(backfillHandler,
            contractDetails, startDate, endDate, barSize.getBarLengthSeconds());
        this.workQueue.offer(request);
    }
    
    public void run() {
        this.iq32.RegisterClientApp("JAMES_NICOLL_1268", version, "0.11111111");
        
        try {
            this.historicalSocket = new Socket(this.localhost, HISTORICAL_DATA_PORT);
            try {
                this.historicalReader = new BufferedReader(
                    new InputStreamReader(this.historicalSocket.getInputStream())
                );
                this.historicalWriter = new BufferedWriter(
                    new OutputStreamWriter(this.historicalSocket.getOutputStream())
                );
            
                while (!this.stop) {
                    runInnerLoop();
                }
        
                this.historicalSocket.shutdownOutput();
				this.historicalSocket.shutdownInput();
            } finally {
				this.historicalSocket.close();
            }
        } catch(IOException e) {
            // XXX: Attempt to reconnect?
            log.debug("IOException while communicating with IQFeed:", e);
        }
        this.iq32.RemoveClientApp();
    }
    
    private void runInnerLoop()
        throws IOException {
        try {
			final IQFeedWork work = this.workQueue.poll(60, TimeUnit.SECONDS);
            
            if (null == work) {
                return;
            }
            work.doWork(this);
        } catch(InterruptedException e) {
            return;
        } catch(IQFeedException e) {
            log.error("IQFeed exception caught: "
                + e);
            // FIXME: Handle
            return;
        }
    }
    
    private class InternalIQ extends IQ_32 {
        private     InternalIQ() {
            super();
        }
        
        public void IQConnectStatus(int a, int b) {
            log.info("IQConnectStatus("
                + a + ", "
                + b + ");");
            // XXX: Do stuff here
        }
    }
}
