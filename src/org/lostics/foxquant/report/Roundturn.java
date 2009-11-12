// $Id: Roundturn.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.report;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.lostics.foxquant.database.DatabaseUnavailableException;
import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.model.OrderStatus;
import org.lostics.foxquant.model.ContractManager;
import org.lostics.foxquant.Configuration;

public class Roundturn extends Object {
    private final String action;
    private final int contractID;
    private final double entryPrice;
    private final Timestamp entryTime;
    private final double exitPrice;
    private final Timestamp exitTime;
    private final String localSymbol;

    public          Roundturn(final int setContractID, final String setLocalSymbol,
        final String setAction,
        final Timestamp setEntryTime, final double setEntryPrice,
        final Timestamp setExitTime, final double setExitPrice) {
        this.contractID = setContractID;
        this.localSymbol = setLocalSymbol;
        this.action = setAction;
        this.entryTime = setEntryTime;
        this.entryPrice = setEntryPrice;
        this.exitTime = setExitTime;
        this.exitPrice = setExitPrice;
    }

    /**
     * Loads all roundturns since the optional date "since" from the database.
     * A roundturn is a matching order to enter the market, and order to exit
     * the market. Orders that did are not completely filled, or orders that
     * never had a matching exit, are disregarded.
     *
     * @param configuration the configuration, used to generate a database
     * connection.
     * @param since the date to retrieve roundturns that the entry order
     * was PLACED after. Can be null, in which case all roundturns are
     * retrieved.
     * @return a list of roundturns, sorted by the time that the entry order
     * COMPLETED at.
     */
    public static List<Roundturn> get(final Configuration configuration,
        final Timestamp since)
        throws DatabaseUnavailableException, SQLException {
        final Connection dbConnection = configuration.getDBConnection();
        final List<Roundturn> roundturns;

        try {
            roundturns = get(dbConnection, since);
        } finally {
            dbConnection.close();
        }

        return roundturns;
    }

    /**
     * Loads all roundturns since the optional date "since" from the database.
     * A roundturn is a matching order to enter the market, and order to exit
     * the market. Orders that did are not completely filled, or orders that
     * never had a matching exit, are disregarded.
     *
     * @param dbConnection a connection to the database.
     * @param since the date to retrieve roundturns that the entry order
     * was PLACED after. Can be null, in which case all roundturns are
     * retrieved.
     * @return a list of roundturns, sorted by the time that the entry order
     * COMPLETED at.
     */
    public static List<Roundturn> get(final Connection dbConnection,
        final Timestamp since)
        throws SQLException {
        final List<Roundturn> roundturns = new ArrayList<Roundturn>();
        final PreparedStatement statement;

        assert null != dbConnection;

        statement = dbConnection.prepareStatement("SELECT C.CONTRACT_ID, C.LOCAL_SYMBOL SYMBOL, "
            + " SA.RECEIVED_AT ENTRY_TIME, TA.ACTION, SA.AVG_FILL_PRICE ENTRY_PRICE, "
            + "SB.RECEIVED_AT EXIT_TIME, SB.AVG_FILL_PRICE EXIT_PRICE "
            + "FROM CONTRACT C "
            + "JOIN TRADE_ORDER TA ON TA.CONTRACT_ID=C.CONTRACT_ID "
            + "JOIN TRADE_ORDER TB ON TB.CONTRACT_ID=C.CONTRACT_ID "
            + "JOIN CONTRACT_POSITION CP ON CP.ENTRY_ORDER=TA.ORDER_ID AND CP.EXIT_ORDER=TB.ORDER_ID "
            + "JOIN ORDER_STATUS SA ON SA.ORDER_ID=TA.ORDER_ID "
            + "JOIN ORDER_STATUS SB ON SB.ORDER_ID=TB.ORDER_ID "
            + "WHERE CP.EXIT_ORDER IS NOT NULL AND SA.STATUS=? AND SB.STATUS=? "
            + (null != since
                ? "AND TA.CREATED_AT>? "
                : "")
            + "ORDER BY SA.RECEIVED_AT");
        try {
            final ResultSet rs;

            statement.setString(1, OrderStatus.Filled.toString());
            statement.setString(2, OrderStatus.Filled.toString());
            if (null != since) {
                statement.setTimestamp(3, since);
            }
            rs = statement.executeQuery();
            while (rs.next()) {
                final String action = rs.getString("ACTION");
                final int contractID = rs.getInt("CONTRACT_ID");
                final double entryPrice = rs.getDouble("ENTRY_PRICE");
                final double exitPrice = rs.getDouble("EXIT_PRICE");
                final Timestamp entryTime = rs.getTimestamp("ENTRY_TIME");
                final Timestamp exitTime = rs.getTimestamp("EXIT_TIME");
                final Roundturn roundturn;
                final String symbol = rs.getString("SYMBOL");

                roundturn = new Roundturn(contractID, symbol, action, entryTime, entryPrice,
                    exitTime, exitPrice);
                roundturns.add(roundturn);
            }
            rs.close();
        } finally {
            statement.close();
        }

        return roundturns;
    }

    public String getAction() {
        return this.action;
    }

    public int getContractID() {
        return this.contractID;
    }

    public double getEntryPrice() {
        return this.entryPrice;
    }

    public Timestamp getEntryTime() {
        return this.entryTime;
    }

    public double getExitPrice() {
        return this.exitPrice;
    }

    public Timestamp getExitTime() {
        return this.exitTime;
    }

    public String getLocalSymbol() {
        return this.localSymbol;
    }

    public double getProfit() {
        if (this.getAction().equals(ContractManager.ORDER_ACTION_BUY)) {
            return this.getExitPrice() - this.getEntryPrice();
        } else {
            return this.getEntryPrice() - this.getExitPrice();
        }
    }

    /**
     * Returns the profit as a percentage of entry price.
     */
    public double getProfitPercent() {
        return getProfit() * 100 / getEntryPrice();
    }
}
