// $Id: ContractDetailsWork.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashSet;
import java.util.Set;

import com.ib.client.ContractDetails;
import com.ib.client.Contract;

class ContractDetailsWork extends Object implements DatabaseWork {
    private ContractDetails contractDetails;

    protected   ContractDetailsWork(final ContractDetails setContractDetails) {
        this.contractDetails = setContractDetails;
    }
    
    public void dispose(final DatabaseThread databaseThread) {
        return;
    }
    
    /**
     * Ensures the given list of exchanges exist in the database. This is done
     * before they are listed as associated with a contract. We do this here because
     * Interactive Brokers is an authorative source for valid exchanges.
     */
    private void ensureExchangesExist(final Connection dbConnection,
        final String[] exchanges)
        throws SQLException {
        final PreparedStatement statement = dbConnection.prepareStatement(
            "INSERT IGNORE INTO EXCHANGE "
                + "(EXCHANGE) "
                + "VALUES (?)");
        try {
            for (String exchange: exchanges) {
                statement.setString(1, exchange.trim());
                statement.executeUpdate();
            }
        } finally {
            statement.close();
        }
    }
    
    /**
     * Ensures the given list of order types exist in the database. This is done
     * before they are listed as associated with a contract. We do this here because
     * Interactive Brokers is an authorative source for valid order types.
     */
    private void ensureOrderTypesExist(final Connection dbConnection,
        final String[] orderTypes)
        throws SQLException {
        final PreparedStatement statement = dbConnection.prepareStatement(
            "INSERT IGNORE INTO ORDER_TYPE "
                + "(ORDER_TYPE) "
                + "VALUES (?)");
        try {
            for (String orderType: orderTypes) {
                statement.setString(1, orderType.trim());
                statement.executeUpdate();
            }
        } finally {
            statement.close();
        }
    }
    
    /**
     * Sets the list of exchanges a contract can be traded on.
     */
    private void setExchanges(final Connection dbConnection,
        final Contract contract, final String[] exchanges)
        throws SQLException {
        final Set<String> missingExchanges = new HashSet<String>();
        final PreparedStatement statement;
        
        for (String exchange: exchanges) {
            missingExchanges.add(exchange.trim());
        }
            
        statement = dbConnection.prepareStatement(
            "SELECT CONTRACT_ID,EXCHANGE "
                + "FROM CONTRACT_EXCHANGE "
                + "WHERE CONTRACT_ID=?",
            ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
        try {
            final ResultSet rs;
            
            statement.setInt(1, contract.m_conId);
            rs = statement.executeQuery();
            while (rs.next()) {
                final String exchange = rs.getString("EXCHANGE");
                if (missingExchanges.contains(exchange)) {
                    missingExchanges.remove(exchange);
                } else {
                    rs.deleteRow();
                }
            }
            if (missingExchanges.size() > 0) {
                rs.moveToInsertRow();
                for (String exchange: missingExchanges) {
                    rs.updateInt(1, contract.m_conId);
                    rs.updateString(2, exchange.trim());
                    rs.insertRow();
                }
            }
            
            rs.close();
        } finally {
            statement.close();
        }
    }
    
    /**
     * Sets the list of orderTypes a contract can be traded on.
     */
    private void setOrderTypes(final Connection dbConnection,
        final Contract contract, final String[] orderTypes)
        throws SQLException {
        final Set<String> missingOrderTypes = new HashSet<String>();
        final PreparedStatement statement;
        
        for (String orderType: orderTypes) {
            missingOrderTypes.add(orderType.trim());
        }
            
        statement = dbConnection.prepareStatement(
            "SELECT CONTRACT_ID, ORDER_TYPE "
                + "FROM CONTRACT_ORDER_TYPE "
                + "WHERE CONTRACT_ID=?",
            ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
        try {
            final ResultSet rs;
            
            statement.setInt(1, contract.m_conId);
            rs = statement.executeQuery();
            while (rs.next()) {
                final String orderType = rs.getString("ORDER_TYPE");
                if (missingOrderTypes.contains(orderType)) {
                    missingOrderTypes.remove(orderType);
                } else {
                    rs.deleteRow();
                }
            }
            if (missingOrderTypes.size() > 0) {
                rs.moveToInsertRow();
                for (String orderType: missingOrderTypes) {
                    rs.updateInt(1, contract.m_conId);
                    rs.updateString(2, orderType.trim());
                    rs.insertRow();
                }
            }
            
            rs.close();
        } finally {
            statement.close();
        }
    }
    
    public void write(final DatabaseThread databaseThread)
        throws SQLException {
        final Contract contract = contractDetails.m_summary;
        final Connection dbConnection = databaseThread.getConnection();
        PreparedStatement statement;

        // Make sure there's an entry in the table
        statement = dbConnection.prepareStatement("INSERT IGNORE INTO CONTRACT "
            + "(CONTRACT_ID, SYMBOL, SEC_TYPE, CURRENCY) "
            + "VALUES (?, ?, ?, ?)");
        try {
            statement.setInt(1, contract.m_conId);
            statement.setString(2, contract.m_symbol);
            statement.setString(3, contract.m_secType);
            statement.setString(4, contract.m_currency);
            statement.executeUpdate();
        } finally {
            statement.close();
        }
        
        // Then update that entry
        statement = dbConnection.prepareStatement("UPDATE CONTRACT "
            + "SET EXPIRY=?, STRIKE=?, CONTRACT_RIGHT=?, MULTIPLIER=?, EXCHANGE=?, "
            + "LOCAL_SYMBOL=?, PRIMARY_EXCHANGE=?, MARKET_NAME=?, TRADING_CLASS=?, "
            + "MIN_TICK=?, PRICE_MAGNIFIER=?, NOTES=? "
            + "WHERE CONTRACT_ID=?");
        try {
            // Contract fields
            statement.setString(1, contract.m_expiry);
            statement.setDouble(2, contract.m_strike);
            statement.setString(3, contract.m_right);
            statement.setString(4, contract.m_multiplier);
            statement.setString(5, contract.m_exchange);
            statement.setString(6, contract.m_localSymbol);
            statement.setString(7, contract.m_primaryExch);

            // Contract details fields
            statement.setString(8, contractDetails.m_marketName);
            statement.setString(9, contractDetails.m_tradingClass);
            statement.setDouble(10, contractDetails.m_minTick);
            statement.setInt(11, contractDetails.m_priceMagnifier);
            statement.setString(12, contractDetails.m_notes);

            statement.setInt(13, contract.m_conId);

            statement.executeUpdate();
        } finally {
            statement.close();
        }
        
        final String[] orderTypes = contractDetails.m_orderTypes.split(",");
        final String[] validExchanges = contractDetails.m_validExchanges.split(",");
        
        ensureExchangesExist(dbConnection, validExchanges);
        ensureOrderTypesExist(dbConnection, orderTypes);
        setExchanges(dbConnection, contract, validExchanges);
        setOrderTypes(dbConnection, contract, orderTypes);

        return;
    }
}
