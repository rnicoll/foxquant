// $Id: DatabaseThread.java 706 2009-11-11 10:41:13Z jrn $
package org.lostics.foxquant.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayDeque;
import java.util.Deque;

import org.apache.log4j.Logger;

import com.ib.client.ContractDetails;
import com.ib.client.Contract;
import com.ib.client.TickType;

import org.lostics.foxquant.model.ContractManager;
import org.lostics.foxquant.model.PeriodicData;
import org.lostics.foxquant.model.TickData;
import org.lostics.foxquant.Configuration;

public class DatabaseThread extends Thread {
    public static final int QUEUE_SIZE = 5000;

    private static final String PRICE_STATEMENT = "INSERT INTO TICK "
        + "(CONTRACT_ID, RECEIVED_AT, BID_PRICE, ASK_PRICE, BID_SIZE, ASK_SIZE) "
        + "VALUES (?, ?, ?, ?, ?, ?)";

    private static final Logger log = Logger.getLogger(DatabaseThread.class);

    // Synchronization is done on this object
    private final Object notificationObject = new Object();   

    private final Configuration configuration;
    private Connection connection;
    private PreparedStatement priceStatement;

    private boolean stop = false;
    private Deque<DatabaseWork> workQueue
        = new ArrayDeque<DatabaseWork>(QUEUE_SIZE);
    private Deque<PriceTick> priceTickPool
        = new ArrayDeque<PriceTick>(QUEUE_SIZE);

    public          DatabaseThread(final Configuration setConfiguration)
        throws DatabaseUnavailableException, SQLException {
        for (int queueIdx = 0; queueIdx < QUEUE_SIZE; queueIdx++) {
            priceTickPool.offer(new PriceTick());
        }

        this.configuration = setConfiguration;
        this.connection = configuration.getDBConnection();
        this.priceStatement = this.connection.prepareStatement(PRICE_STATEMENT);
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
    protected PreparedStatement getPriceStatement() {
        return this.priceStatement;
    }
    
    /**
     * Re-adds a price tick to the pool of price ticks, after it's been
     * written out. MUST only be called from within the database thread.
     */
    protected void poolPriceTick(final PriceTick tick) {
        this.priceTickPool.offer(tick);
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

        synchronized (this.notificationObject) {
            success = this.workQueue.offer(new PeriodicDataWork(contractDetails,
                periodicData));
            this.notificationObject.notify();
        }

        return success;
    }

    public boolean queueTick(final ContractManager setContractManager,
        final TickData setTickData) {
        final boolean success;

        synchronized (this.notificationObject) {
            final PriceTick tick = this.priceTickPool.poll();

            if (null == tick) {
                // We've exhausted the available queue space!
                return false;
            }

            tick.contractManager = setContractManager;
            tick.tickData = setTickData;

            success = this.workQueue.offer(tick);
            this.notificationObject.notify();
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
    }

    //public boolean contractDetails(final ContractDetails contractDetails) {
    //    return this.contractDetailsQueue.offer(contractDetails);
    //}

    public void run() {
        while (!this.stop) {
            DatabaseWork work;
        
            synchronized (this.notificationObject) {
                work = this.workQueue.poll();
                while (null == work) {
                    try {
                        this.notificationObject.wait();
                    } catch(InterruptedException e) {
                        log.error("Database thread interrupted.");
                    }
                    work = this.workQueue.poll();
                }
            }

            try {
                work.write(this);
            } catch(DatabaseUnavailableException e) {
                log.error("Error writing data out to database.", e);
            } catch(SQLException e) {
                log.error("Error writing data out to database.", e);
            }
            work.dispose(this);
        }
        try {
            this.priceStatement.close();
            this.connection.close();
        } catch(SQLException e) {
            // Don't care
        }

        return;
    }
}
