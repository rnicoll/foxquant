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

import org.apache.log4j.Logger;

import org.lostics.foxquant.database.DatabaseThread;
import org.lostics.foxquant.model.HistoricBarSize;
import org.lostics.foxquant.model.HistoricalDataConsumer;
import org.lostics.foxquant.model.HistoricalDataSource;
import org.lostics.foxquant.model.PeriodicData;
import org.lostics.foxquant.Configuration;

public class IQFeedGateway extends Thread implements HistoricalDataSource {
    // Size, in characters, of the admin socket read buffer.
    private static final int ADMIN_BUFFER_SIZE = 512;

    // 5 seconds as milliseconds
    private static final long CONNECTION_RETRY_DELAY = 5000;
    
    private static final int MAX_CONNECT_ATTEMPTS = 3;

    protected static final String MSG_END = "!ENDMSG!";
    protected static final String MSG_NO_DATA = "!NO_DATA!";
    
    private static final String PARAMETER_PRODUCT = "-product";
    private static final String PARAMETER_VERSION = "-version";

    protected static final int DEFAULT_ADMIN_PORT = 9300;
    protected static final int DEFAULT_HISTORICAL_PORT = 9100;
    protected static final TimeZone TIMEZONE = TimeZone.getTimeZone("EST");
    
    private static final String PRODUCT_ID = "JAMES_NICOLL_1268";

    private static final Logger log = Logger.getLogger(IQFeedGateway.class);

    private final Configuration configuration;
    private final DatabaseThread databaseThread;
    private final InetAddress localhost;
    private boolean stop = false;
    private final String version;
    
    private Socket adminSocket;
    private BufferedReader adminReader;
    private BufferedWriter adminWriter;
    
    private Socket historicalSocket;
    private BufferedReader historicalReader;
    private BufferedWriter historicalWriter;
    
    private Process iqConnect = null;
    
    private final BlockingQueue<IQFeedWork> workQueue = new ArrayBlockingQueue<IQFeedWork>(100);

    public          IQFeedGateway(final Configuration setConfiguration,
        final DatabaseThread setDatabaseThread, final String setVersion)
        throws IQFeedException {
        this.configuration = setConfiguration;
        this.databaseThread = setDatabaseThread;
        this.version = setVersion;
        
        try {
            this.localhost = InetAddress.getByName("localhost");
        } catch(UnknownHostException e) {
            throw new IQFeedException(e);
        }
        
        this.setName("IQFeed");
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
    
    /**
     * Handles a dataline coming from the admin data feed.
     *
     * @throws IQFeedException if the incoming data contained an error.
     */
    private void handleAdminInput(final String line)
        throws IQFeedException {
        if (line.indexOf("S,STATS") == 0) {
            // XXX: Do something useful with this data.
        } else {
            log.warn("Unhandled admin input: "
                + line);
        }
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
        int connectionAttempts;
    
        for (connectionAttempts = 0; connectionAttempts < MAX_CONNECT_ATTEMPTS; connectionAttempts++) {
            try {
                this.adminSocket = new Socket(this.localhost, DEFAULT_ADMIN_PORT);
            } catch(IOException e) {
                this.adminSocket = null;
            }
            
            if (null != this.adminSocket) {
                break;
            }
            
            try {
                this.iqConnect = Runtime.getRuntime().exec(new String[] {
                    this.configuration.getIQConnect(),
                    PARAMETER_PRODUCT, PRODUCT_ID,
                    PARAMETER_VERSION, this.version});
            } catch(IOException e) {
                log.error("IOException while running IQConnect application: "
                    + e);
                return;
            } catch(SecurityException e) {
                log.error("SecurityException while running IQConnect application: "
                    + e);
                return;
            }
            try {
                Thread.currentThread().sleep(CONNECTION_RETRY_DELAY);
            } catch(InterruptedException e) {
                log.error("IQFeed gateway interrupted while attempting to connect to IQConnect.");
                return;
            }
        }
        
        if (connectionAttempts >= MAX_CONNECT_ATTEMPTS) {
            log.error("Could not connect to admin socket after "
                + MAX_CONNECT_ATTEMPTS + " attempts, giving up.");
            return;
        }
        
        try {
            try {
                this.adminReader = new BufferedReader(
                    new InputStreamReader(this.adminSocket.getInputStream())
                );
                this.adminWriter = new BufferedWriter(
                    new OutputStreamWriter(this.adminSocket.getOutputStream())
                );
                
                if (null == this.iqConnect) {
                    this.adminWriter.write("S,REGISTER CLIENT APP,"
                        + PRODUCT_ID + ","
                        + this.version + "\r\n");
                    this.adminWriter.flush();
                    String response = this.adminReader.readLine();
                    // XXX: Need a timeout here
                    if (!response.equals("S,REGISTER CLIENT APP COMPLETED,")) {
                        log.error("Unexpected response while trying to register client app: "
                            + response);
                        return;
                    }
                }
            
    
                for (connectionAttempts = 0; connectionAttempts < MAX_CONNECT_ATTEMPTS; connectionAttempts++) {
                    try {
                        this.historicalSocket = new Socket(this.localhost, DEFAULT_HISTORICAL_PORT);
                    } catch(IOException e) {
                        this.historicalSocket = null;
                    }
                    
                    if (null != this.historicalSocket) {
                        break;
                    }
                    try {
                        Thread.currentThread().sleep(CONNECTION_RETRY_DELAY);
                    } catch(InterruptedException e) {
                        log.error("IQFeed gateway interrupted while attempting to connect to IQConnect historical data service.");
                        return;
                    }
                }
                
                if (connectionAttempts >= MAX_CONNECT_ATTEMPTS) {
                    log.error("Could not connect to historical socket after "
                        + MAX_CONNECT_ATTEMPTS + " attempts, giving up.");
                    return;
                }
                
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
                
                this.adminWriter.write("S,REMOVE CLIENT APP,"
                    + PRODUCT_ID + ","
                    + this.version + "\r\n");
                this.adminWriter.flush();
                // XXX: Need a timeout here
                // XXX: Need to have a single input stream for admin stuff,
                // to handle excess stats lines.
                String response = this.adminReader.readLine();
                if (!response.equals("S,REMOVE CLIENT APP COMPLETED,")) {
                    log.warn("Unexpected response while trying to remove client app: "
                        + response);
                }
                
                this.adminSocket.shutdownOutput();
                this.adminSocket.shutdownInput();
            } finally {
                this.adminSocket.close();
            }
        } catch(IOException e) {
            // XXX: Attempt to reconnect?
            log.debug("IOException while communicating with IQFeed: ", e);
        }
    }
    
    private void runInnerLoop()
        throws IOException {
        final char[] adminBuffer = new char[ADMIN_BUFFER_SIZE];
        int adminBufferUsed = 0;
        
        try {
			final IQFeedWork work = this.workQueue.poll(1, TimeUnit.SECONDS);
            int charRead = this.adminReader.read(adminBuffer, adminBufferUsed, ADMIN_BUFFER_SIZE - adminBufferUsed);
            
            if (charRead > 0) {
                int adminBufferConsumed = 0;
                final int oldAdminBufferUsed = adminBufferUsed;
                
                adminBufferUsed += charRead;
                if (adminBufferUsed >= ADMIN_BUFFER_SIZE) {
                    log.error("IQFeed admin data feed has overflowed its buffer. Buffer contents: "
                        + adminBuffer);
                    return;
                }
                
                for (int charIdx = oldAdminBufferUsed; charIdx < adminBufferUsed; charIdx++) {
                    if (adminBuffer[charIdx] == '\n') {
                        final String line = new String(adminBuffer, adminBufferConsumed, charIdx - adminBufferConsumed);
                        adminBufferConsumed = charIdx + 1;
                        handleAdminInput(line);
                    }
                }
                
                if (adminBufferConsumed >= adminBufferUsed) {
                    adminBufferUsed = 0;
                } else {
                    adminBufferUsed = adminBufferUsed - adminBufferConsumed;
                    
                    for (int charIdx = 0; charIdx < adminBufferUsed; charIdx++) {
                        adminBuffer[charIdx] = adminBuffer[adminBufferConsumed + charIdx];
                    }
                }
            }
            
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
}
