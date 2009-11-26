// $Id: ConnectionManager.java 706 2009-11-11 10:41:13Z jrn $
package org.lostics.foxquant.ib;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.EClientSocket;
import com.ib.client.EWrapper;
import com.ib.client.Execution;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.TickType;
import com.ib.client.UnderComp;

import org.apache.log4j.Logger;

import org.lostics.foxquant.database.DatabaseUnavailableException;
import org.lostics.foxquant.database.DatabaseThread;
import org.lostics.foxquant.iqfeed.IQFeedException;
import org.lostics.foxquant.iqfeed.IQFeedGateway;
import org.lostics.foxquant.model.ContractKey;
import org.lostics.foxquant.model.ContractManager;
import org.lostics.foxquant.model.ContractManagerConsumer;
import org.lostics.foxquant.model.ErrorListener;
import org.lostics.foxquant.model.HistoricBarSize;
import org.lostics.foxquant.model.HistoricalDataConsumer;
import org.lostics.foxquant.model.HistoricalDataSource;
import org.lostics.foxquant.model.OrderStatus;
import org.lostics.foxquant.model.PeriodicData;
import org.lostics.foxquant.model.StrategyAlreadyExistsException;
import org.lostics.foxquant.model.StrategyFactory;
import org.lostics.foxquant.model.TickData;
import org.lostics.foxquant.util.ConcurrentQueueMap;
import org.lostics.foxquant.Configuration;
import org.lostics.foxquant.FoxQuant;
import org.lostics.foxquant.SMSGateway;
import org.lostics.foxquant.TwitterGateway;

public class ConnectionManager extends Object implements HistoricalDataSource {
    public static final String CONTRACT_SECURITY_TYPE_BAG = "BAG";
    public static final String CONTRACT_SECURITY_TYPE_CASH = "CASH";
    public static final String CONTRACT_SECURITY_TYPE_FORWARD = "FOP";
    public static final String CONTRACT_SECURITY_TYPE_FUTURE = "FOP";
    public static final String CONTRACT_SECURITY_TYPE_INDEX = "IND";
    public static final String CONTRACT_SECURITY_TYPE_OPTION = "OPT";
    public static final String CONTRACT_SECURITY_TYPE_STOCK = "STK";

    public static final int ERROR_ALREADY_CONNECTED = 501;
    public static final int ERROR_CONNECT_FAIL = 502;
    public static final int ERROR_UPDATE_TWS = 503;
    public static final int ERROR_NOT_CONNECTED = 504;

    /** The delay to put between each historical data request. */
    public static final int    HISTORICAL_DATA_REQUEST_PACING = 20000;
    
    public static final int TRIGGER_METHOD_DEFAULT = 0;
    public static final int TRIGGER_METHOD_DOUBLE_BID_ASK = 1;
    public static final int TRIGGER_METHOD_LAST = 2;
    public static final int TRIGGER_METHOD_DOUBLE_LAST = 3;
    public static final int TRIGGER_METHOD_BID_ASK = 4;
    public static final int TRIGGER_METHOD_LAST_BID_ASK = 7;
    public static final int TRIGGER_METHOD_MID_POINT = 8;

    public static final String TICKER_TYPE_BID = "BID";
    public static final String TICKER_TYPE_MIDPOINT = "MIDPOINT";
    public static final String TICKER_TYPE_ASK = "ASK";

    public static final int DEFAULT_PORT = 7496;

    // No other sizes are supported anyway
    public static final int REAL_TIME_BAR_PERIOD_SECONDS = 5;

    /* --- Private static variables start here ----------------------------- */

    /** Number of hours to backfill contract managers */
    public static final int BACKFILL_HOURS = 3;

    /** Time zone to be used for time formatting. */
    public static final TimeZone timeZone = TimeZone.getTimeZone("GMT");

    /* --- Non-static variables start here --------------------------------- */

    private       boolean connected;

    // Synchronize access to clientSocket, on clientSocket
    private final EClientSocket clientSocket;

    private final Configuration configuration;
    private ConnectionManager.Wrapper wrapper;

    private int clientID;
    private boolean isClosed = false;
    private boolean isConnected = false;

    private static final Logger log = Logger.getLogger(ConnectionManager.class);

    /** Unique serial number assigned to each connection manager to help
     * group orders together by session.
     */
    private int sessionID;
    
    /**
     * Next order ID to use. Initially retrieved from TWS on connection.
     */
    private AtomicInteger nextOrderID = new AtomicInteger();

    private final DatabaseThread databaseThread;
    private final IQFeedGateway iqFeedGateway;
    private final SMSGateway smsGateway;
    private final TwitterGateway twitterGateway;

    private final List<ConnectionStatusListener> connectionListeners
        = Collections.synchronizedList(new ArrayList<ConnectionStatusListener>());

    private final List<ErrorListener> errorListeners
        = Collections.synchronizedList(new ArrayList<ErrorListener>());

    /** Consumers for contract details as they arrive from TWS. */
    private final ConcurrentQueueMap<Integer, ContractDetailsConsumer> contractDetailsConsumers
        = new ConcurrentQueueMap<Integer, ContractDetailsConsumer>();

    /* ---------------------------------------------------------------------
     * --- Price data ------------------------------------------------------
     * ---------------------------------------------------------------------
     */

    /**
     * Tracks the historical data consumer that each historic request ID refers
     * to.
     */
    private final ConcurrentMap<Integer, HistoricalDataRequest> historicalDataRequests
        = new ConcurrentHashMap<Integer, HistoricalDataRequest>();

    // Synchronize tickerContractManager and tickerCMConsumer access on
    // tickerContractManager.
    private final Map<Integer, TickConsumerState> tickerContractManager
        = new HashMap<Integer, TickConsumerState>();

    /** Tracks the ContractManagerConsumers for each ContractManager. If a
     * contract manager has consumers waiting, it is not ready, and new
     * requests for it should go into the consumer list. If it doesn't have
     * any waiting consumers ready, it must have been prepared already, and is
     * safe to hand out.
     */
    private final Map<ContractManager, Set<ContractManagerConsumer>> tickerCMConsumer
        = new HashMap<ContractManager, Set<ContractManagerConsumer>>();

    /**
     * The next ID to be used for a historic or realtime data request.
     */
    private AtomicInteger nextTickerID = new AtomicInteger(1);

    /* ---------------------------------------------------------------------
     * --- Orders ----------------------------------------------------------
     * ---------------------------------------------------------------------
     */

    // Synchronize access on itself. Blocks so that if no entry is found, a
    // new one can be safely generated without risk of duplication.
    private final Map<Contract, Integer> contractTickerMap
        = new HashMap<Contract, Integer>();

    private final ConcurrentMap<Integer, OrderTracking> orderTrack
        = new ConcurrentHashMap<Integer, OrderTracking>();

    /**
     * Construct a new connection manager for tracking the connection to
     * TWS.
     *
     * @param setConfiguration the configuration to pull SMS gateway
     * and database details from.
     */
    public      ConnectionManager(final Configuration setConfiguration)
        throws DatabaseUnavailableException, IQFeedException, SQLException {
        this.configuration = setConfiguration;

        this.wrapper = new ConnectionManager.Wrapper();
        this.clientSocket = new EClientSocket(this.wrapper);

        this.smsGateway = configuration.getSMSGateway();
        if (null != this.smsGateway) {
            this.smsGateway.start();
        }

        this.twitterGateway = configuration.getTwitterGateway();
        if (null != this.twitterGateway) {
            this.twitterGateway.start();
        }

        this.databaseThread = new DatabaseThread(configuration);
        this.databaseThread.start();

        this.iqFeedGateway = new IQFeedGateway(this.databaseThread, FoxQuant.VERSION);
        this.iqFeedGateway.start();
    }

    /**
     * Adds a new listener for error events from TWS.
     *
     * @param newListener the new listener to add to the listeners. The outcome
     * of adding the same listener twice is undefined, and should be avoided.
     */
    public void addErrorListener(final ErrorListener listener) {
        this.errorListeners.add(listener);
    }

    public void cancelOrder(final int orderID) {
        synchronized(this.clientSocket) {
            this.clientSocket.cancelOrder(orderID);
        }

        return;
    }

    /**
     * Cleans up resources (primarily database connections) in use for this
     * connection manager.
     */
    public void close()
        throws SQLException {
        if (this.isClosed) {
            // Throw an exception?
            return;
        }

        // XXX: Should wait for all backfill handlers to shutdown.

        this.disconnect();

        this.isClosed = true;
        if (null != this.smsGateway) {
            this.smsGateway.close();
        }
        this.databaseThread.close();
        if (null != this.iqFeedGateway) {
            this.iqFeedGateway.close();
        }
        if (null != this.twitterGateway) {
            this.twitterGateway.close();
        }

        if (null != this.smsGateway) {
            try {
                this.smsGateway.join(5000);
                while (this.smsGateway.isAlive()) {
                    log.error("SMS gateway thread did not quit cleanly, interrupting.");
                    this.smsGateway.interrupt();
                    this.smsGateway.join(1000);
                }
            } catch(InterruptedException e) {
                log.error("Interrupted while waiting for SMS gateway thread to exit.");
                return;
            }
        }

        try {
            this.databaseThread.join(15000);
            
            while (this.databaseThread.isAlive()) {
                log.error("Database thread did not quit cleanly, interrupting.");
                this.databaseThread.interrupt();
                this.databaseThread.join(1000);
            }
        } catch(InterruptedException e) {
            log.error("Interrupted while waiting for database thread to exit.");
            return;
        }
        
        if (null != this.iqFeedGateway) {
            try {
                this.iqFeedGateway.join(15000);
                
                while (this.iqFeedGateway.isAlive()) {
                    log.error("IQFeed thread did not quit cleanly, interrupting.");
                    this.iqFeedGateway.interrupt();
                    this.iqFeedGateway.join(1000);
                }
            } catch(InterruptedException e) {
                log.error("Interrupted while waiting for IQFeed thread to exit.");
                return;
            }
        }
        
        if (null != this.twitterGateway) {
            try {
                this.twitterGateway.join(15000);
                
                while (this.twitterGateway.isAlive()) {
                    log.error("Twitter thread did not quit cleanly, interrupting.");
                    this.twitterGateway.interrupt();
                    this.twitterGateway.join(1000);
                }
            } catch(InterruptedException e) {
                log.error("Interrupted while waiting for Twitter thread to exit.");
                return;
            }
        }
    }

    /**
     * @param listener a connection status listener, or null if no listener is
     * to be added.
     * @throws IllegalStateException if this connection manager has already 
     * been closed.
     */
    public void connect(final Connection connection, final ConnectionStatusListener listener)
        throws IllegalStateException, SQLException, java.net.UnknownHostException {
        final PreparedStatement statement;

        if (this.isClosed) {
            throw new IllegalStateException("Connection manager has already had close() called on it.");
        }

        this.clientID = configuration.twsClientID;

        statement = connection.prepareStatement("INSERT INTO SESSION "
            + "(HOSTNAME, CLIENT_ID) "
            + "VALUES (?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
        try {
            final ResultSet rs;
            final InetAddress localhost = InetAddress.getLocalHost();

            statement.setString(1, localhost.getHostName());
            statement.setInt(2, this.clientID);
            statement.executeUpdate();
            rs = statement.getGeneratedKeys();
            rs.next();
            this.sessionID = rs.getInt(1);
            rs.close();
        } finally {
            statement.close();
        }

        if (null != listener) {
            this.connectionListeners.add(listener);
        }
        this.clientSocket.eConnect(this.configuration.twsHost,
            this.configuration.twsPort, this.clientID);
    }

    /**
     * @throws IllegalStateException if this connection manager has already 
     * been closed.
     */
    public void connect(final Connection connection)
        throws IllegalStateException, SQLException, java.net.UnknownHostException {
        this.connect(connection, null);
    }

    /**
     * @throws IllegalStateException if this connection manager has already 
     * been closed.
     */
    public void disconnect()
        throws IllegalStateException {
        // XXX: Should reject disconnect until all contract managers are shutdown

        this.clientSocket.eDisconnect();
    }

    /** Brings everything to a halt. It ain't pretty, it just does it. Does not
     * return, as it calls System.exit() once done.
     */
    public void emergencyStop() {
        try {
            final Set<ContractManager> contractManagers = new HashSet<ContractManager>();
            final Set<Integer> tickerIDs = new HashSet<Integer>();

            log.fatal("****** EMERGENCY STOP ******");

            synchronized (this.tickerContractManager) {
                for (TickConsumerState state: this.tickerContractManager.values()) {
                    contractManagers.add(state.getContractManager());
                }
                tickerIDs.addAll(this.tickerContractManager.keySet());
                tickerContractManager.clear();
            }

            synchronized (this.clientSocket) {
                for (Integer tickerID: tickerIDs) { 
                    this.clientSocket.cancelRealTimeBars(tickerID);
                }

                for (Integer orderID: this.orderTrack.keySet()) {
                    log.error("Cancelling order #"
                        + orderID + " due to emergency stop.");
                    this.clientSocket.cancelOrder(orderID);
                }
            }

            try {
                for (ContractManager contractManager: contractManagers) {
                    contractManager.close();
                }

                this.close();
            } catch(SQLException e) {
                log.error("Ignoring SQLException during emergency stop.", e);
            }

            final Thread thread = Thread.currentThread();
            log.info("Thread: "
                + thread.getName() + ".");
            Thread.dumpStack();
        } finally {
            System.exit(0);
        }

        return;
    }

    /**
     * Sends a contract manager out to the consumers listening for it.
     * Generally this would only be called from BackfillHandler once backfill
     * has been completed.
     */
    protected void fireContractManagerReady(final ContractManager contractManager) {
        // Notify the CM consumers that the contract manager is ready
        final Set<ContractManagerConsumer> consumers;

        synchronized (this.tickerContractManager) {
            consumers = this.tickerCMConsumer.get(contractManager);
            this.tickerCMConsumer.remove(contractManager);
        }

        if (null != consumers) {
            for (ContractManagerConsumer currentConsumer: consumers) {
                currentConsumer.contractManagerReady(contractManager);
            }
        } else {
            // Odd, maybe warn, but not an error condition
        }
    }

    public Configuration getConfiguration() {
        return this.configuration;
    }

    /**
     * Request details of a contract from TWS. Particularly useful for filling
     * in fields such as contract ID.
     *
     * @param contract the contract to retrieve details of.
     * @param consumer the contract details consumer to send the contract
     * details to once they are ready.
     * @throws NotConnectedToTWSException if not connected to Trader
     * Workstation.
     */
    protected void getContractDetails(final Contract contract,
        final ContractDetailsConsumer consumer)
        throws NotConnectedToTWSException {
        final int requestID = this.nextTickerID.getAndAdd(1);

        synchronized (this.clientSocket) {
            if (false == clientSocket.isConnected()) {
                throw new NotConnectedToTWSException();
            }

            clientSocket.reqContractDetails(requestID, contract);
        }
        this.contractDetailsConsumers.offer(requestID, consumer);
    }

    /**
     * Retrieves a manager for the given contract. Doesn't return the contract
     * manager directly, but instead passes it to the given contract manager
     * consumer because it can take a long time to prepare backfill the contract
     * manager.
     *
     * @param sendTo the contract manager consumer to send the contract manager
     * to once it is ready.
     * @param contract the contract to produce a manager for.
     */
    public void getContractManager(final ContractManagerConsumer sendTo,
        final ContractDetails contractDetails, final StrategyFactory strategyFactory)
        throws DatabaseUnavailableException, SQLException, StrategyAlreadyExistsException {
        final Contract contract = contractDetails.m_summary;
        Integer mktTickerID = null;

        // Look up the ticker ID for this contract, and assign one if there
        // isn't one yet.
        synchronized (this.contractTickerMap) {
            mktTickerID = this.contractTickerMap.get(contract);

            if (null == mktTickerID) {
                mktTickerID = this.nextTickerID.getAndAdd(2);
                this.contractTickerMap.put(contract, mktTickerID);
            }
        }

        // Attempt to retrieve the contract manager from the live list of
        // contract managers.
        synchronized (this.tickerContractManager) {
            BackfillHandler backHandler = null;
            TWSContractManager cManager;
            TickConsumerState state = this.tickerContractManager.get(mktTickerID);

            if (null != state) {
                final Set<ContractManagerConsumer> consumers;
            
                cManager = state.getContractManager();
                consumers = this.tickerCMConsumer.get(cManager);

                // If there's a list of CM consumers waiting for notification
                // that it's ready, add this one to the list. Otherwise, it must
                // be live, so notify immediately.
                if (null != consumers &&
                    consumers.size() > 0) {
                    consumers.add(sendTo);
                } else {
                    sendTo.contractManagerReady(cManager);
                }

                return;
            }

            final Set<ContractManagerConsumer> consumers;

            cManager = new TWSContractManager(this, contractDetails, strategyFactory);
            state = new TickConsumerState(cManager);
            this.tickerContractManager.put(mktTickerID, state);

            backHandler = new BackfillHandler(this, mktTickerID, cManager);

            // Generate the list of consumers that want to know when this
            // contract manager is ready
            consumers = new HashSet<ContractManagerConsumer>();
            this.tickerCMConsumer.put(cManager, consumers);
            consumers.add(sendTo);

            backHandler.start();
        }

        // If the contract manager already exists, the method will have returned
        // by now.

        synchronized(this.clientSocket) {
            this.clientSocket.reqMktData(mktTickerID, contract, "", false);
        }

        return;
    }

    public Connection getDBConnection()
        throws DatabaseUnavailableException {
        return this.configuration.getDBConnection();
    }

    public IQFeedGateway getIQFeedGateway() {
        return this.iqFeedGateway;
    }

    public TwitterGateway getTwitterGateway() {
        return this.twitterGateway;
    }

    protected boolean isConnected() {
        final boolean result;

        synchronized(clientSocket) {
            result = clientSocket.isConnected();
        }

        return result;
    }

    protected int generateOrderID(final Connection dbConnection,
        final Contract contract, final Order order)
        throws IllegalStateException, OrderIDUnavailableException, SQLException {
        int orderID = this.nextOrderID.getAndIncrement();
        final PreparedStatement statement;

        statement = dbConnection.prepareStatement("INSERT INTO TRADE_ORDER "
            + "(ORDER_ID, CLIENT_ID, CONTRACT_ID, ACTION, TOTAL_QUANTITY, TYPE, TIME_IN_FORCE, CREATED_AT) "
            + "(SELECT ?, ?, ?, ?, ?, ?, ?, ? "
                + "FROM (SELECT 1) mutex "
                + "LEFT JOIN TRADE_ORDER O ON O.ORDER_ID=? "
                + "WHERE O.ORDER_ID IS NULL)");
        try {            
            statement.setInt(2, this.clientID);
            statement.setInt(3, contract.m_conId);
            statement.setString(4, order.m_action);
            statement.setLong(5, order.m_totalQuantity);
            statement.setString(6, order.m_orderType.toString());
            statement.setString(7, order.m_tif);
            statement.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
            statement.setInt(1, orderID);
            statement.setInt(9, orderID);
            while (statement.executeUpdate() == 0) {
                // If we haven't managed to insert a new row, it must be a collision.
                // Increment the order ID and try again.
                orderID = this.nextOrderID.getAndIncrement();
                statement.setInt(1, orderID);
                statement.setInt(9, orderID);
            }
        } finally {
            statement.close();
        }
        
        order.m_orderId = orderID;

        return orderID;
    }

    /**
     * Places an order. Order ID must be set, and can be generated from
     * generateOrderID() before this is called.
     *
     * @throws IllegalArgumentException if the order ID is not set.
     * @throws IllegalStateException if this connection manager has already 
     * been closed.
     */
    public void placeOrder(final Connection dbConnection,
        final TWSContractManager consumer,
        final Contract contract, final Order order)
        throws IllegalStateException, SQLException {
        final OrderTracking orderTrackDetails;
        final PreparedStatement statement;

        if (this.isClosed) {
            throw new IllegalStateException("Connection manager has already had close() called on it.");
        }
        
        if (order.m_orderId <= 0) {
            throw new IllegalArgumentException("Order must have been assigned an order ID before it is placed in the market. See generateOrderID().");
        }

        statement = dbConnection.prepareStatement("UPDATE TRADE_ORDER "
            + "SET CLIENT_ID=?, CONTRACT_ID=?, ACTION=?, TOTAL_QUANTITY=?, "
            + "TYPE=?, LIMIT_PRICE=?, STOP_PRICE=?, OFFSET_PRICE=?, "
            + "TIME_IN_FORCE=?, OCA_GROUP=?, OCA_TYPE=?, PARENT_ORDER_ID=?, "
            + "BLOCK_ORDER=?, SWEEP_TO_FILL=?, DISPLAY_QUANTITY=?, "
            + "TRIGGER_METHOD=?, OUTSIDE_HOURS=?, HIDDEN=?, PRICE_DISCRETION=?, "
            + "GOOD_AFTER_TIME=?, GOOD_TIL_DATE=? WHERE ORDER_ID=?");
        try {
            final ResultSet rs;

            statement.setInt(1, this.clientID);
            statement.setInt(2, contract.m_conId);
            statement.setString(3, order.m_action);
            statement.setLong(4, order.m_totalQuantity);
            statement.setString(5, order.m_orderType.toString());
            if (order.m_lmtPrice != 0.0) {
                statement.setDouble(6, order.m_lmtPrice);
            } else {
                statement.setNull(6, Types.DOUBLE);
            }
            if (order.m_orderType.equals(OrderType.STP.toString()) ||
                order.m_orderType.equals(OrderType.STPLMT.toString())) {
                statement.setDouble(7, order.m_auxPrice);
                statement.setNull(8, Types.DOUBLE);
            } else if (order.m_orderType.equals(OrderType.REL.toString())) {
                statement.setNull(7, Types.DOUBLE);
                statement.setDouble(8, order.m_auxPrice);
            } else {
                statement.setNull(7, Types.DOUBLE);
                statement.setNull(8, Types.DOUBLE);
            }
            statement.setString(9, order.m_tif);
            statement.setString(10, order.m_ocaGroup);
            statement.setInt(11, order.m_ocaType);
            statement.setInt(12, order.m_parentId);
            statement.setBoolean(13, order.m_blockOrder);
            statement.setBoolean(14, order.m_sweepToFill);
            statement.setInt(15, order.m_displaySize);
            statement.setInt(16, order.m_triggerMethod);
            statement.setBoolean(17, order.m_outsideRth);
            statement.setBoolean(18, order.m_hidden);
            statement.setDouble(19, order.m_discretionaryAmt);
            statement.setString(20, order.m_goodAfterTime);
            statement.setString(21, order.m_goodTillDate);
            statement.setInt(22, order.m_orderId);
            statement.executeUpdate();
        } finally {
            statement.close();
        }

        orderTrackDetails = new OrderTracking(contract, order, consumer);
        this.orderTrack.put(order.m_orderId, orderTrackDetails);

        // We actually place the order by passing it to updateOrder(), which does the same
        // job except it doesn't get an order ID first.
        synchronized (this.clientSocket) {
            this.clientSocket.placeOrder(order.m_orderId, contract, order);
        }

        return;
    }

    /**
     * Removes a running contract manager from the data consumers this streams
     * data to, then closes the contract manager.
     *
     * XXX: There is no scope for removing a contract manager that has been
     * requested.
     *
     * @param contractManager the contract manager to remove.
     * @return true if the contract manager was found and removed successfully,
     * false otherwise.
     */
    public boolean removeContractManager(final ContractManager contractManager)
        throws SQLException {
        final Integer tickerID;

        // Look up the ticker ID for this contract, and assign one if there
        // isn't one yet.
        synchronized (this.contractTickerMap) {
            tickerID = this.contractTickerMap.get(contractManager.getContract());
        }

        if (null == tickerID) {
            return false;
        }

        synchronized (this.clientSocket) {
            this.clientSocket.cancelRealTimeBars(tickerID);
        }

        // Attempt to retrieve the contract manager from the live list of
        // contract managers.
        synchronized (this.tickerContractManager) {
            this.tickerCMConsumer.remove(tickerID);
            this.tickerContractManager.remove(tickerID);
        }

        contractManager.close();

        return true;
    }

    /**
     * Sends a request for historical data to IB; data will be returned to the
     * given backfill handler. May return data twice, or before/after the
     * requested period, because Interactive Broker's API is insane and
     * requires that time periods are described in seconds, days, weeks,
     * months or years, and doesn't allow for requests in seconds over a day.
     * Quite why they couldn't just take a start and end date in milliseconds
     * since epoch I'll never know, but hey this is the same people who merrily
     * translate double to/from String when sending it over the network, so
     * really who knows how they think. Rant finished.
     *
     * NOTE: To ensure IB historical pacing is not exceeded, this sends at
     * most one request every 10 seconds, and therefore can take a significant
     * time to complete.
     *
     * NOTE: Unlike most other requests, the results from historical data
     * are not automatically recorded in the database, due the complexity of
     * determining where they should go.
     *
     * FIXME: We should have any async thread that handles this stuff. As it
     * stands this mess just kinda hopes the data has arrived before we start
     * the contract manager.
     *
     * @param backfillHandler the backfill handler to send historical data to.
     * @param startTime the time to retrieve historical data from.
     * @param endTime the time to retrieve data up to.
     * @throws IllegalArgumentException if start time is not before end time.
     */
    public void requestHistoricalData(final HistoricalDataConsumer backfillHandler,
        final ContractDetails contractDetails, final Date startDate, final Date endDate,
        final HistoricBarSize barSize)
        throws IllegalArgumentException, InterruptedException {
        final String barType = "MIDPOINT";
        final SortedSet<DateRange> dateRanges;

        if (!endDate.after(startDate)) {
            throw new IllegalArgumentException("End time must be after start time for historical data requests.");
        }

        dateRanges = DateRange.splitRange(startDate, endDate, barSize.getRequestLengthMillis());

        for (DateRange currentDateRange: dateRanges) {
            final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss zzz");
            final String endDateStr;
            final int reqID;
            final HistoricalDataRequest request;

            dateFormat.setTimeZone(ConnectionManager.timeZone);
            endDateStr = dateFormat.format(currentDateRange.getEndDate());

            reqID = this.nextTickerID.getAndAdd(1);
            request = new HistoricalDataRequest(reqID, contractDetails, endDateStr,
                barSize, barType, backfillHandler);
            this.historicalDataRequests.put(reqID, request);

            synchronized (this.clientSocket) {
                this.clientSocket.reqHistoricalData(reqID, contractDetails.m_summary,
                    endDateStr, barSize.getRequestLengthSetting(),
                    barSize.getBarID(), barType, 1, 2);
            }

            // Make sure we don't exceed pacing for historical requests
            Thread.currentThread().sleep(HISTORICAL_DATA_REQUEST_PACING);
        }
    }

    /**
     * Removes a listener for error events from TWS.
     *
     * @param listener the listener to remove. No error is reported if the
     * listener is not registered.
     */
    public void removeErrorListener(final ErrorListener listener) {
        this.errorListeners.remove(listener);
    }

    /**
     * Broadcasts an error to all error listeners attached to this connection
     * manager.
     * XXX: Should be moved out into a supervisor layer; here it looks like it
     * represents only errors from TWS, which it doesn't necessarily.
     */
    public void sendError(final Exception e) {
        boolean sent = false;

        for (ErrorListener listener: this.errorListeners) {
            sent = true;
            listener.error(e);
        }
        if (!sent) {
            log.error(e.toString());
        }
    }

    /**
     * Broadcasts an error to all error listeners attached to this connection
     * manager.
     * XXX: Should be moved out into a supervisor layer; here it looks like it
     * represents only errors from TWS, which it doesn't necessarily.
     */
    public void sendError(final String message) {
        boolean sent = false;

        for (ErrorListener listener: this.errorListeners) {
            sent = true;
            listener.error(message);
        }
        if (!sent) {
            log.error(message);
        }
    }

    /**
     * Keeps the pair of an order, and the status consumer for it, together.
     */
    private static class HistoricalDataRequest extends Object {
        private final HistoricBarSize barSize;
        private final HistoricalDataConsumer consumer;
        private final ContractDetails contractDetails;
        private final String endDate;
        private final int requestID;
        private final String tickerType;

        private         HistoricalDataRequest(final int setRequestID,
            final ContractDetails setContractDetails, final String setEndDate,
            final HistoricBarSize setBarSize, final String setTickerType,
            final HistoricalDataConsumer setConsumer) {
            this.requestID = setRequestID;
            this.contractDetails = setContractDetails;
            this.endDate = setEndDate;
            this.barSize = setBarSize;
            this.tickerType = setTickerType;
            this.consumer = setConsumer;
        }
    }

    /**
     * Keeps the pair of an order, and the status consumer for it, together.
     */
    private static class OrderTracking extends Object {
        private final Contract contract;
        private final Order order;
        private final TWSContractManager statusConsumer;

        private     OrderTracking(final Contract setContract,
            final Order setOrder,
            final TWSContractManager setStatusConsumer) {
            this.contract = setContract;
            this.order = setOrder;
            this.statusConsumer = setStatusConsumer;
        }
    }

    /**
     * Wrapper class to be passed to TWS for responses to be fed through.
     */
    private class Wrapper extends Object implements EWrapper {
        private         Wrapper() {
        }

        public void accountDownloadEnd(final String accountName) {
        }

        public void bondContractDetails(final int reqId, final ContractDetails contractDetails) {
        }

        public void connectionClosed() {
            for (ConnectionStatusListener listener: ConnectionManager.this.connectionListeners) {
                listener.disconnected();
            }
        }

        public void currentTime(long time) {
        }

        public void contractDetails(final int requestID, final ContractDetails contractDetails) {
            ContractDetailsConsumer consumer;
            final ContractKey contractKey = new ContractKey(contractDetails.m_summary);
            final Contract contract = contractDetails.m_summary;

            consumer = ConnectionManager.this.contractDetailsConsumers.poll(requestID);
            if (null != consumer) {
                consumer.contractDetailsReady(contractDetails);
            }
            ConnectionManager.this.databaseThread.queueContractDetails(contractDetails);
        }

        public void contractDetailsEnd(final int requestID) {
            ContractDetailsConsumer consumer;

            consumer = ConnectionManager.this.contractDetailsConsumers.poll(requestID);
            if (null != consumer) {
                consumer.contractDetailsEnd();
            }
        }

        public void deltaNeutralValidation(int reqId, UnderComp underComp) {
        }

        public void error(final int id, final int errorCode, final String errorString) {
            boolean sent = false;

            for (ErrorListener listener: ConnectionManager.this.errorListeners) {
                sent = true;
                listener.error(id, errorCode, errorString);
            }
            if (!sent) {
                log.error("Error #"
                    + id + ","
                    + errorCode + ":"
                    + errorString);
            }
        }

        public void error(final String errorString) {
            ConnectionManager.this.sendError(errorString);
        }

        public void error(final Exception cause) {
            for (ErrorListener listener: ConnectionManager.this.errorListeners) {
                listener.error(cause);
            }

            log.error("Exception from TWS: "
                + cause);
        }

        public void execDetails(int orderID, Contract contract, Execution execution) {
        }

        public void execDetailsEnd(int orderID) {
        }

        public void fundamentalData(int reqId, String data) {
        }

        public void historicalData(int reqID, String timeStr, double open, double high, double low,
            double close, int volume, int count, double wap, boolean hasGaps) {
            final ContractDetails contractDetails;
            final HistoricalDataConsumer historicalDataConsumer;
            final HistoricalDataRequest historicalDataRequest;
            final boolean finished;
            final java.sql.Timestamp date;
            final long time;

            historicalDataRequest = ConnectionManager.this.historicalDataRequests.get(reqID);
            historicalDataConsumer = historicalDataRequest.consumer;
            contractDetails = historicalDataRequest.contractDetails;

            finished = timeStr.indexOf("finished") >= 0;
            if (finished) {
                historicalDataConsumer.handleHistoricPriceFinished();
                return;
            }

            try {
                time = Long.parseLong(timeStr);
            } catch(NumberFormatException e) {
                ConnectionManager.this.sendError("Mangled time string \""
                    + timeStr + "\" received for historical data feed.");
                return;
            }

            date = new java.sql.Timestamp(time * 1000);
            final PeriodicData periodicData = new PeriodicData(date,
                (int)(open / contractDetails.m_minTick), (int)(high / contractDetails.m_minTick),
                (int)(low / contractDetails.m_minTick), (int)(close / contractDetails.m_minTick));
            try {
                historicalDataConsumer.handleHistoricPrice(periodicData, hasGaps);
            } catch(Exception e) {
                // XXX: Should remove the consumer
                log.error("Caught error from contract manager: "
                    + e.toString());
                e.printStackTrace(System.err);
            }
            if (!ConnectionManager.this.databaseThread.queuePeriodicData(contractDetails, periodicData)) {
                log.error("Could not queue periodic data work for database thread.");
            }
        }

        public void managedAccounts( String accountsList) {
        }

        public void nextValidId( int orderID) {
            ConnectionManager.this.nextOrderID.set(orderID);
        }

        public void orderStatus( int orderID, String statusCode, int filled, int remaining,
                double avgFillPrice, int permID, int parentID, double lastFillPrice,
                int clientID, String whyHeld) {
            final OrderStatus status;
            final OrderTracking tracker = ConnectionManager.this.orderTrack.get(orderID);

            if (null == tracker) {
                // Not interested
                return;
            }

            try {
                status = OrderStatus.valueOf(statusCode);
            } catch(IllegalArgumentException e) {
                // XXX: Should begin shutdown
                log.error("Unable to parse order status \""
                    + statusCode + "\", expected one of "
                    + java.util.EnumSet.allOf(OrderStatus.class) + ".");
                return;
            }

            try {
                final Connection dbConnection
                    = ConnectionManager.this.configuration.getDBConnection();

                try {
                    final PreparedStatement orderIDStatement;
                    final PreparedStatement orderStatusStatement
                        = dbConnection.prepareStatement("INSERT INTO ORDER_STATUS "
                            + "(ORDER_ID, STATUS, FILLED, REMAINING, AVG_FILL_PRICE, "
                                + "PERM_ID, PARENT_ID, LAST_FILLED_PRICE, CLIENT_ID, WHY_HELD, RECEIVED_AT) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)");

                    orderStatusStatement.setInt(1, orderID);
                    orderStatusStatement.setString(2, status.toString());
                    orderStatusStatement.setInt(3, filled);
                    orderStatusStatement.setInt(4, remaining);
                    orderStatusStatement.setDouble(5, avgFillPrice);
                    orderStatusStatement.setInt(6, permID);
                    orderStatusStatement.setInt(7, parentID);
                    orderStatusStatement.setDouble(8, lastFillPrice);
                    orderStatusStatement.setInt(9, clientID);
                    orderStatusStatement.setString(10, whyHeld);
                    orderStatusStatement.executeUpdate();
                    orderStatusStatement.close();

                    orderIDStatement
                        = dbConnection.prepareStatement("UPDATE TRADE_ORDER "
                            + "SET PERM_ID=? "
                            + "WHERE ORDER_ID=?");
                    orderIDStatement.setInt(1, permID);
                    orderIDStatement.setInt(2, orderID);
                    orderIDStatement.executeUpdate();
                    orderIDStatement.close();
                } finally {
                    dbConnection.close();
                }
            } catch(DatabaseUnavailableException e) {
                ConnectionManager.this.sendError("Unable to write order status back to database due to: "
                    + e);
            } catch(SQLException e) {
                ConnectionManager.this.sendError("Unable to write order status back to database due to: "
                    + e);
            }

            tracker.statusConsumer.orderStatusChanged(new OrderStatusDetails(orderID, status, filled,
                remaining, avgFillPrice, lastFillPrice, whyHeld));

            // If the order is completely filled, SMS user and remove from tracker.
            if (status == OrderStatus.Filled &&
                remaining == 0) {
                final Order order = tracker.order;

                ConnectionManager.this.orderTrack.remove(orderID);

                try {
                    // XXX: Need to ensure we only trigger SMS sending once
                    if (null != ConnectionManager.this.smsGateway) {
                        ConnectionManager.this.smsGateway.sendMessage(ConnectionManager.this.configuration.txtlocalNumber,
                            order.m_action + " "
                            + order.m_totalQuantity + " "
                            + tracker.contract.m_localSymbol + "@"
                            + avgFillPrice);
                    }
                } catch(Exception e) {
                    ConnectionManager.this.sendError("Caught exception while sending SMS notification of trade: "
                        + e);
                }
            } else {
                if (status == OrderStatus.Filled) {
                    log.debug("Order partially filled, "
                        + remaining + " remaining.");
                }
            }
        }

        public void openOrder( int orderID, Contract contract, Order order, OrderState orderState) {
        }

        public void openOrderEnd() {
        }

        public void realtimeBar(int reqID, long time, double open, double high, double low, double close, long volume, double wap, int count) {
            return;
        }

        public void receiveFA(int faDataType, String xml) {
        }

        public void scannerParameters(String xml) {
        }

        public void scannerData(int reqID, int rank, ContractDetails contractDetails, String distance,
                String benchmark, String projection, String legsStr) {
        }

        public void scannerDataEnd(int reqId) {
        }

        public void tickPrice(int tickerID, int field, double price, int canAutoExecute) {
            boolean writeOut = true;
            final long now = System.currentTimeMillis();
            final TickConsumerState state;
            final TWSContractManager contractManager;

            synchronized (ConnectionManager.this.tickerContractManager) {
                state = ConnectionManager.this.tickerContractManager.get(tickerID);
            }

            if (null == state) {
                log.warn("Received price tick for unknown contract, ticker ID #"
                    + tickerID + ".");
                return;
            }

            contractManager = state.getContractManager();
            state.setTime(now);
            switch(field) {
                case TickType.BID:
                    state.setBidPrice(price);
                    break;
                case TickType.ASK:
                    state.setAskPrice(price);
                    break;
                default:
                    writeOut = false;
                    break;
            }

            if (writeOut) {
                final TickData tickData = state.getCopyOfTickData();
                try {
                    contractManager.handleTick(tickData);
                } catch(Exception e) {
                    // XXX: Should remove the consumer if this happens too often
                    log.error("Caught error from contract manager: "
                        + e.toString());
                    e.printStackTrace(System.err);
                }
                
                ConnectionManager.this.databaseThread.queueTick(contractManager, tickData);
            }

            return;
        }

        public void tickSize(int tickerID, int field, int size) {
            // As we get so many tick size changes, and the strategies I'
            // working on don't require them, this only copies them into the data
            // it doesn't actually write them anywhere. This means nothing sees
            // the size change until the next price change goes through.

            final long now = System.currentTimeMillis();
            final TWSContractManager contractManager;
            final TickConsumerState state;

            synchronized (ConnectionManager.this.tickerContractManager) {
                state = ConnectionManager.this.tickerContractManager.get(tickerID);
            }

            if (null == state) {
                log.warn("Received price tick for unknown contract, ticker ID #"
                    + tickerID + ".");
                return;
            }

            contractManager = state.getContractManager();
            state.setTime(now);
            switch(field) {
                case TickType.BID_SIZE:
                    state.setBidSize(size);
                    break;
                case TickType.ASK_SIZE:
                    state.setAskSize(size);
                    break;
            }

            return;
        }

        public void tickOptionComputation( int tickerID, int field, double impliedVol,
                double delta, double modelPrice, double pvDividend) {
        }

        public void tickGeneric(int tickerID, int tickType, double value) {
        }

        public void tickSnapshotEnd(int reqID) {
        }

        public void tickString(int tickerID, int tickType, String value) {
        }

        public void tickEFP(int tickerID, int tickType, double basisPoints,
                String formattedBasisPoints, double impliedFuture, int holdDays,
                String futureExpiry, double dividendImpact, double dividendsToExpiry) {
        }

        public void updateAccountValue(final String key, final String value,
            final String currency, final String accountName) {
        }

        public void updatePortfolio(Contract contract, int position, double marketPrice, double marketValue,
                double averageCost, double unrealizedPNL, double realizedPNL, String accountName) {
        }

        public void updateAccountTime(String timeStamp) {
        }

        public void updateMktDepth( int tickerID, int position, int operation, int side, double price, int size) {
        }

        public void updateMktDepthL2( int tickerID, int position, String marketMaker, int operation,
                int side, double price, int size) {
        }

        public void updateNewsBulletin( int msgID, int msgType, String message, String origExchange) {
        }
    }
}
