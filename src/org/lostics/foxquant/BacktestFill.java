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
import java.util.List;
import java.util.Set;

import com.ib.client.ContractDetails;
import com.ib.client.Contract;

import org.lostics.foxquant.database.DatabaseUnavailableException;
import org.lostics.foxquant.ib.DateRange;
import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.iqfeed.IQFeedException;
import org.lostics.foxquant.model.HistoricalDataConsumer;
import org.lostics.foxquant.model.HistoricBarSize;
import org.lostics.foxquant.model.PeriodicData;

public class BacktestFill extends Object implements HistoricalDataConsumer {
    public  static final String BAR_TYPE_BID      = ConnectionManager.TICKER_TYPE_BID;
    public  static final String BAR_TYPE_MIDPOINT = ConnectionManager.TICKER_TYPE_MIDPOINT;
    public  static final String BAR_TYPE_ASK    = ConnectionManager.TICKER_TYPE_ASK;

    private static final int BACKFILL_MONTHS = 9;

    // Synchronize access on barInsertStatement
    private PreparedStatement barInsertStatement;

    private     BacktestFill(final Connection dbConnection)
        throws SQLException {
        this.barInsertStatement
            = dbConnection.prepareStatement("INSERT INTO MINUTE_BAR "
                + "(CONTRACT_ID, BAR_TYPE, BAR_START, OPEN, HIGH, LOW, CLOSE) "
                + "(SELECT ?, ?, ?, ?, ?, ?, ? "
                    + "FROM (SELECT 1 FROM DUAL) mutex "
                    + "LEFT OUTER JOIN MINUTE_BAR bb "
                    + "ON bb.CONTRACT_ID=? AND bb.BAR_TYPE=? AND bb.BAR_START=? "
                    + "WHERE bb.CONTRACT_ID IS NULL)");
    }

    private void close()
        throws SQLException {
        this.barInsertStatement.close();
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
                final BacktestFill client = new BacktestFill(dbConnection);
                try {
                    final List<ContractDetails> contractDetails = getContractDetails(dbConnection);

                    connectionManager.connect(dbConnection);

                    for (ContractDetails currentContractDetails: contractDetails) {
                        final Contract currentContract = currentContractDetails.m_summary;
                        
                        client.barInsertStatement.setInt(1, currentContract.m_conId);
                        client.barInsertStatement.setString(2, BAR_TYPE_MIDPOINT);
                        client.barInsertStatement.setInt(8, currentContract.m_conId);
                        client.barInsertStatement.setString(9, BAR_TYPE_MIDPOINT);

                        Date startDate = getLastDataDate(dbConnection, currentContract, BAR_TYPE_MIDPOINT);
                        final Date endDate = new Date();
                        System.out.println("Requesting bid "
                            + currentContract.m_localSymbol + " from "
                            + startDate + " to "
                            + endDate);
                        connectionManager.requestHistoricalData(client,
                            currentContractDetails, startDate, endDate, HistoricBarSize.ONE_MINUTE);
                    }
                } finally {
                    client.close();
                }
            } finally {
                dbConnection.close();
            }
        } finally {
            connectionManager.close();
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

    public void handleHistoricPriceFinished() {
        // Do nothing
    }

    public void handleHistoricError(final Exception e) {
        System.err.println("Error while retrieving historic prices: "
            + e);
    }

    public void handleHistoricPrice(final PeriodicData periodicData, final boolean hasGaps) {
        // FIXME: Incoming values are in multiples of minimum tick, need to convert
        // back to actual prices.
        try {
            synchronized(this.barInsertStatement) {
                barInsertStatement.setTimestamp(3, periodicData.startTime);
                barInsertStatement.setDouble(4, periodicData.open);
                barInsertStatement.setDouble(5, periodicData.high);
                barInsertStatement.setDouble(6, periodicData.low);
                barInsertStatement.setDouble(7, periodicData.close);
                barInsertStatement.setTimestamp(10, periodicData.startTime);
                barInsertStatement.executeUpdate();
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<ContractDetails> getContractDetails(final Connection dbConnection)
        throws SQLException {
        final List<ContractDetails> contracts = new ArrayList<ContractDetails>();
        final PreparedStatement statement
            = dbConnection.prepareStatement("SELECT C.CONTRACT_ID, C.EXPIRY, C.STRIKE, C.CONTRACT_RIGHT, "
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
