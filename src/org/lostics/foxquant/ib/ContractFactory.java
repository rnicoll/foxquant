// $Id: ContractFactory.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.ib;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;

import org.apache.log4j.Logger;

import org.lostics.foxquant.model.ContractKey;
import org.lostics.foxquant.util.ConcurrentQueueMap;

public class ContractFactory extends Object implements ContractDetailsConsumer {
    private final ConnectionManager connectionManager;
    
    private static final Logger log = Logger.getLogger(ContractFactory.class);

    /** Consumers for contract details as they arrive from TWS. */
    private final ConcurrentQueueMap<ContractKey, ContractDetailsConsumer> contractConsumers
        = new ConcurrentQueueMap<ContractKey, ContractDetailsConsumer>();

    public          ContractFactory(final ConnectionManager setConnectionManager) {
        this.connectionManager = setConnectionManager;
    }

    public void contractDetailsEnd() {
        // XXX: Need to be able to report this back in cases where we don't have
        // contract details, as a failure to look up a contract.
        log.info("Received contract details end.");
    }

    public void contractDetailsReady(final ContractDetails contractDetails) {
        final ContractKey contractKey = new ContractKey(contractDetails.m_summary);
        ContractDetailsConsumer consumer;

        consumer = this.contractConsumers.poll(contractKey);
        while (null != consumer) {
            consumer.contractDetailsReady(contractDetails);
            consumer = this.contractConsumers.poll(contractKey);
        }
    }

    /**
     * Attempts to load forex contract details from the database.
     */
    public static ContractDetails getForexContractDetails(final Connection dbConnection,
        final String baseCurrency, final String purchaseCurrency)
        throws SQLException {
        final ContractDetails contractDetails = new ContractDetails();
        final Contract contract = new Contract();
        Integer contractID = null;
        final StringBuilder exchanges = new StringBuilder();
        final StringBuilder orderTypes = new StringBuilder();
        PreparedStatement statement;
        
        contractDetails.m_summary = contract;
        contract.m_symbol = baseCurrency;
        contract.m_secType = ConnectionManager.CONTRACT_SECURITY_TYPE_CASH;
        contract.m_currency = purchaseCurrency;
        contract.m_exchange = "IDEALPRO";
        contract.m_localSymbol = baseCurrency + "."
            + purchaseCurrency;

        statement = dbConnection.prepareStatement("SELECT C.CONTRACT_ID, C.EXPIRY, C.STRIKE, C.CONTRACT_RIGHT, "
                + "C.MULTIPLIER, C.EXCHANGE, C.PRIMARY_EXCHANGE, C.MARKET_NAME, C.TRADING_CLASS, C.MIN_TICK, C.PRICE_MAGNIFIER, C.NOTES "
            + "FROM CONTRACT C "
            + "WHERE C.SYMBOL=? "
            + "AND C.SEC_TYPE=? "
            + "AND C.EXCHANGE=? "
            + "AND C.CURRENCY=? "
            + "AND C.LOCAL_SYMBOL=?");

        try {
            final ResultSet rs;

            statement.setString(1, contract.m_symbol);
            statement.setString(2, contract.m_secType);
            statement.setString(3, contract.m_exchange);
            statement.setString(4, contract.m_currency);
            statement.setString(5, contract.m_localSymbol);
            rs = statement.executeQuery();
            if (rs.next()) {
                contractID = rs.getInt("CONTRACT_ID");
                contract.m_conId = contractID;
                contractDetails.m_marketName = rs.getString("MARKET_NAME");
                contractDetails.m_tradingClass = rs.getString("TRADING_CLASS");
                contractDetails.m_minTick = rs.getDouble("MIN_TICK");
                contractDetails.m_priceMagnifier = rs.getInt("PRICE_MAGNIFIER");
                contractDetails.m_notes = rs.getString("NOTES");
            }
            rs.close();
       } finally {
            statement.close();
        }

        if (null != contractID) {
            // Load exchanges as a comma-separated list (because that's how TWS
            // stores it, not because it's a good idea).
            statement = dbConnection.prepareStatement("SELECT E.EXCHANGE "
                + "FROM CONTRACT_EXCHANGE E "
                + "WHERE E.CONTRACT_ID=? "
                + "ORDER BY EXCHANGE");
            try {
                final ResultSet rs;
                
                statement.setInt(1, contract.m_conId);
                rs = statement.executeQuery();
                while (rs.next()) {
                    if (exchanges.length() > 0) {
                        exchanges.append(",");
                    }
                    exchanges.append(rs.getString("EXCHANGE"));
                }
                rs.close();
                contractDetails.m_validExchanges = exchanges.toString();
            } finally {
                statement.close();
            }
            
            // Load order types
            statement = dbConnection.prepareStatement("SELECT O.ORDER_TYPE "
                + "FROM CONTRACT_ORDER_TYPE O "
                + "WHERE O.CONTRACT_ID=? "
                + "ORDER BY ORDER_TYPE");
            try {
                final ResultSet rs;
                
                statement.setInt(1, contract.m_conId);
                rs = statement.executeQuery();
                while (rs.next()) {
                    if (orderTypes.length() > 0) {
                        orderTypes.append(",");
                    }
                    orderTypes.append(rs.getString("ORDER_TYPE"));
                }
                rs.close();
                contractDetails.m_orderTypes = orderTypes.toString();
            } finally {
                statement.close();
            }

            return contractDetails;
        }

        return null;
    }

    public void getForexContractDetails(final Connection dbConnection,
        final String baseCurrency, final String purchaseCurrency,
        final ContractDetailsConsumer consumer)
        throws SQLException {
        final Contract contract;
        ContractDetails contractDetails = getForexContractDetails(dbConnection,
            baseCurrency, purchaseCurrency);
        final ContractKey contractKey;

        if (null != contractDetails) {
            consumer.contractDetailsReady(contractDetails);
            return;
        }

        contract = new Contract();
        contract.m_symbol = baseCurrency;
        contract.m_secType = this.connectionManager.CONTRACT_SECURITY_TYPE_CASH;
        contract.m_currency = purchaseCurrency;
        contract.m_exchange = "IDEALPRO";
        contract.m_localSymbol = baseCurrency + "."
            + purchaseCurrency;
        contractKey = new ContractKey(contract);

        this.contractConsumers.offer(contractKey, consumer);
        this.connectionManager.getContractDetails(contract, this);
    }
}
