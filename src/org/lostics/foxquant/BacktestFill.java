// $Id: BacktestFill.java 698 2009-11-10 22:25:25Z jrn $
package org.lostics.foxquant;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ib.client.ContractDetails;
import com.ib.client.Contract;

import org.lostics.foxquant.database.DatabaseUnavailableException;
import org.lostics.foxquant.ib.DateRange;
import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.iqfeed.IQFeedException;
import org.lostics.foxquant.model.ContractKey;
import org.lostics.foxquant.model.HistoricalDataConsumer;
import org.lostics.foxquant.model.HistoricBarSize;
import org.lostics.foxquant.model.PeriodicData;

public class BacktestFill extends Object implements HistoricalDataConsumer {
    public  static final String BAR_TYPE_MIDPOINT = ConnectionManager.TICKER_TYPE_MIDPOINT;

    private static final int BACKFILL_MONTHS = 9;
    
    // This is also the notification object and synchronization object
    private static Map<ContractKey, BacktestFill> workList = new HashMap<ContractKey, BacktestFill>();
    
    private final ContractKey contractKey;

    private     BacktestFill(final ContractKey setContractKey)
        throws SQLException {
        contractKey = setContractKey;
    }

    public void handleHistoricPriceFinished() {
        synchronized (this.workList) {
            this.workList.remove(this.contractKey);
            this.workList.notifyAll();
        }
    }

    public void handleHistoricPriceError(final Exception e) {
        System.err.println("Error while retrieving historic prices: "
            + e);
    }

    public void handleHistoricPriceNoData() {
        System.err.println("No data to retrieve while attempting to fetch historic prices.");
    }

    public void handleHistoricPrice(final PeriodicData periodicData, final boolean hasGaps) {
        // Written to the database by IQFeed itself
    }

    public static void main(final String[] argv)
        throws Configuration.ConfigurationException, DatabaseUnavailableException,
            InterruptedException, InvocationTargetException,
            IOException, IQFeedException, SQLException {
        final Configuration configuration = FoxQuant.getConfiguration();
        final ConnectionManager connectionManager;

        if (null == configuration) {
            return;
        }

        connectionManager = configuration.getConnectionManager();
        try {
            final Connection dbConnection = configuration.getDBConnection();

            try {
                final List<ContractDetails> contractDetails = getContractDetails(dbConnection);

                connectionManager.connect(dbConnection);

                for (ContractDetails currentContractDetails: contractDetails) {
                    final Contract currentContract = currentContractDetails.m_summary;
                    final ContractKey contractKey = new ContractKey(currentContract);
                    final BacktestFill client = new BacktestFill(contractKey);
                    Date startDate = getLastDataDate(dbConnection, currentContract, BAR_TYPE_MIDPOINT);
                    final Date endDate = new Date();
                    
                    synchronized (BacktestFill.workList) {
                        BacktestFill.workList.put(contractKey, client);
                    }
                    
                    // FIXME: Need to not request data that's entirely outside
                    // market opening hours
                    System.out.println("Requesting bid "
                        + currentContract.m_localSymbol + " from "
                        + startDate + " to "
                        + endDate);
                    connectionManager.getIQFeedGateway().requestHistoricalData(client,
                        currentContractDetails, startDate, endDate, HistoricBarSize.ONE_MINUTE);
                }
            } finally {
                dbConnection.close();
            }
            
            int remaining;
            
            synchronized (BacktestFill.workList) {
                remaining = BacktestFill.workList.keySet().size();
            }
            while (remaining > 0) {
                synchronized (BacktestFill.workList) {
                    BacktestFill.workList.wait(5000);
                    remaining = BacktestFill.workList.keySet().size();
                }
            }
        } finally {
            connectionManager.close();
            System.out.println("Connection manager closed.");
        }
        
        final Thread thread = Thread.currentThread();
        final ThreadGroup threadGroup = thread.getThreadGroup();
        final Thread[] allThreads = new Thread[threadGroup.activeCount()];
        final int threadCount = threadGroup.enumerate(allThreads);
        
        for (int threadIdx = 0; threadIdx < threadCount; threadIdx++) {
            System.out.println("Thread: "
                + allThreads[threadIdx].getName());
        }

        return;
    }

    public static Date getLastDataDate(final Connection dbConnection,
        final Contract contract, final String barType)
        throws SQLException {
        Date date;
        final PreparedStatement statement = dbConnection.prepareStatement("SELECT MAX(BAR_START) FROM MINUTE_BAR "
            + "WHERE CONTRACT_ID=? AND BAR_TYPE=?");

        try {
            final ResultSet rs;

            statement.setInt(1, contract.m_conId);
            statement.setString(2, barType);
            rs = statement.executeQuery();
            if (rs.next()) {
                date = rs.getTimestamp(1);
            } else {
                date = null;
            }
            rs.close();

            if (null == date) {
                final Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                calendar.add(Calendar.MONTH, -BACKFILL_MONTHS);
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                date = calendar.getTime();
            }
        } finally {
            statement.close();
        }

        return date;
    }

    public static List<ContractDetails> getContractDetails(final Connection dbConnection)
        throws SQLException {
        final List<ContractDetails> contracts = new ArrayList<ContractDetails>();
        final PreparedStatement statement
            = dbConnection.prepareStatement("SELECT C.SYMBOL, C.SEC_TYPE, C.EXCHANGE, C.CURRENCY, C.LOCAL_SYMBOL, C.CONTRACT_ID, C.EXPIRY, C.STRIKE, C.CONTRACT_RIGHT, "
                + "C.MULTIPLIER, C.EXCHANGE, C.PRIMARY_EXCHANGE, C.MARKET_NAME, C.TRADING_CLASS, C.MIN_TICK, C.PRICE_MAGNIFIER, C.NOTES "
            + "FROM CONTRACT C ");
        try {
            final ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                final ContractDetails contractDetails = new ContractDetails();
                final Contract contract = new Contract();
                
                contractDetails.m_summary = contract;

                contract.m_symbol = rs.getString("SYMBOL");
                contract.m_secType = rs.getString("SEC_TYPE");
                contract.m_exchange = rs.getString("EXCHANGE");
                contract.m_currency = rs.getString("CURRENCY");
                contract.m_localSymbol = rs.getString("LOCAL_SYMBOL");
                contract.m_conId = rs.getInt("CONTRACT_ID");
                contractDetails.m_marketName = rs.getString("MARKET_NAME");
                contractDetails.m_tradingClass = rs.getString("TRADING_CLASS");
                contractDetails.m_minTick = rs.getDouble("MIN_TICK");
                contractDetails.m_priceMagnifier = rs.getInt("PRICE_MAGNIFIER");
                contractDetails.m_notes = rs.getString("NOTES");

                contracts.add(contractDetails);
            }
            rs.close();
        } finally {
            statement.close();
        }

        return contracts;
    }
}
