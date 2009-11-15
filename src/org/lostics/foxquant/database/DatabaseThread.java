// $Id: DatabaseThread.java 706 2009-11-11 10:41:13Z jrn $
package org.lostics.foxquant.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;

import com.ib.client.ContractDetails;
import com.ib.client.Contract;
import com.ib.client.TickType;

import org.lostics.foxquant.model.ContractManager;
import org.lostics.foxquant.model.PeriodicData;
import org.lostics.foxquant.model.TickData;
import org.lostics.foxquant.Configuration;

public class DatabaseThread extends Thread {
    public static final int QUEUE_SIZE = 500;
    public static final int POOL_SIZE = 250;

    private static final String PERIODIC_DATA_STATEMENT = "INSERT IGNORE INTO MINUTE_BAR "
        + "(CONTRACT_ID, BAR_TYPE, BAR_START, OPEN, HIGH, LOW, CLOSE) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String PRICE_STATEMENT = "INSERT INTO TICK "
        + "(CONTRACT_ID, RECEIVED_AT, BID_PRICE, ASK_PRICE, BID_SIZE, ASK_SIZE) "
        + "VALUES (?, ?, ?, ?, ?, ?)";

    private static final Logger log = Logger.getLogger(DatabaseThread.class);

    // Synchronization is done on this object
    private final Object notificationObject = new Object();   

    private final Configuration configuration;
    private Connection connection;
    private PreparedStatement periodicDataStatement;
    private PreparedStatement priceStatement;

    private boolean stop = false;
    
    // All access to workQueue and priceTickPool must be synchronized on
    // notificationObject.
    private BlockingQueue<DatabaseWork> workQueue
        = new ArrayBlockingQueue<DatabaseWork>(QUEUE_SIZE);
    private Stack<PeriodicDataWork> periodicDataPool
        = new Stack<PeriodicDataWork>();
    private Stack<PriceTick> priceTickPool
        = new Stack<PriceTick>();

    public          DatabaseThread(final Configuration setConfiguration)
        throws DatabaseUnavailableException, SQLException {
        for (int queueIdx = 0; queueIdx < POOL_SIZE; queueIdx++) {
            periodicDataPool.push(new PeriodicDataWork());
            priceTickPool.push(new PriceTick());
        }

        this.configuration = setConfiguration;
        this.connection = configuration.getDBConnection();
        this.setName("Database");
    }

    public void close() {
        this.stop = true;
        synchronized (this.notificationObject) {
            this.notificationObject.notifyAll();
        }
    }
    
    /**
     * Returns a connection to the database. MUST only be called from within
     * the database thread. The connection returned should not be closed by the
     * calling method, as it is instead re-used and closed by the Database Thread
     * when it's finished with it.
     */
    protected Connection getConnection() {
        return this.connection;
    }
    
    /**
     * Returns a prepared statement that can be used for writing out price
     * ticks. MUST only be called from within the database thread. This
     * will become invalid if reconnect() is called.
     */
    protected PreparedStatement getPeriodicDataStatement() {
        return this.periodicDataStatement;
    }
    
    /**
     * Returns a prepared statement that can be used for writing out price
     * ticks. MUST only be called from within the database thread. This
     * will become invalid if reconnect() is called.
     */
    protected PreparedStatement getPriceStatement() {
        return this.priceStatement;
    }
    
    /**
     * Re-adds a periodic data work object to the pool, after it's been
     * written out. MUST only be called from within the database thread.
     */
    protected void poolPeriodicData(final PeriodicDataWork work) {
        this.periodicDataPool.push(work);
    }
    
    /**
     * Re-adds a price tick work object to the pool, after it's been
     * written out. MUST only be called from within the database thread.
     */
    protected void poolPriceTick(final PriceTick work) {
        this.priceTickPool.push(work);
    }

    public boolean queueContractDetails(final ContractDetails contractDetails) {
        final boolean success;

        synchronized (this.notificationObject) {
            success = this.workQueue.offer(new ContractDetailsWork(contractDetails));
            this.notificationObject.notify();
        }

        return success;
    }

    public boolean queuePeriodicData(final ContractDetails contractDetails,
        final PeriodicData periodicData) {
        final boolean success;
        final PeriodicDataWork work;

        try {
            work = this.periodicDataPool.pop();
        } catch(java.util.EmptyStackException e) {
            // We've exhausted the available pool of periodic data objects
            return false;
        }

        work.update(contractDetails, periodicData);

        synchronized (this.notificationObject) {
            success = this.workQueue.offer(work);
            if (success) {
                this.notificationObject.notify();
            } else {
                this.periodicDataPool.push(work);
            }
        }

        return success;
    }

    public boolean queueTick(final ContractManager setContractManager,
        final TickData setTickData) {
        final boolean success;
        final PriceTick work;

        try {
            work = this.priceTickPool.pop();
        } catch(java.util.EmptyStackException e) {
            // We've exhausted the available pool of price tick objects
            return false;
        }

        work.contractManager = setContractManager;
        work.tickData = setTickData;

        synchronized (this.notificationObject) {
            success = this.workQueue.offer(work);
            if (success) {
                this.notificationObject.notify();
            } else {
                this.priceTickPool.push(work);
            }
        }

        return success;
    }
    
    protected void reconnect()
        throws DatabaseUnavailableException, SQLException {
        try {
            this.connection.close();
        } catch(SQLException e) {
            // Expected, ignore.
        }
        this.connection = configuration.getDBConnection();
        this.priceStatement = this.connection.prepareStatement(PRICE_STATEMENT);
        this.periodicDataStatement = this.connection.prepareStatement(PERIODIC_DATA_STATEMENT);
    }

    public void run() {
        try {
            this.priceStatement = this.connection.prepareStatement(PRICE_STATEMENT);
            this.periodicDataStatement = this.connection.prepareStatement(PERIODIC_DATA_STATEMENT);
        } catch(SQLException e) {
            log.error("Database thread failed to set up.");
            // XXX: Need to notify the object that created the thread
            return;
        }
        
        try {
            while (!this.stop) {
                final List<DatabaseWork> workList = new ArrayList<DatabaseWork>();
            
                synchronized (this.notificationObject) {
                    while (0 == this.workQueue.size()) {
                        this.notificationObject.wait();

                        if (this.stop) {
                            return;
                        }
                    }
                    this.workQueue.drainTo(workList);
                }
                
                for (DatabaseWork work: workList) {
                    try {
                        work.write(this);
                    } catch(DatabaseUnavailableException e) {
                        log.error("Error writing data out to database.", e);
                    } catch(SQLException e) {
                        log.error("Error writing data out to database.", e);
                    }
                    work.dispose(this);
                }
            }
            
            try {
                this.periodicDataStatement.close();
                this.priceStatement.close();
                this.connection.close();
            } catch(SQLException e) {
                // Don't care
            }
        } catch(InterruptedException e) {
            log.error("Database thread interrupted.");
            return;
        }

        return;
    }
}
