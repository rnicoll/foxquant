// $Id$
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

import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.model.PeriodicData;

class PeriodicDataWork extends Object implements DatabaseWork {
    private ContractDetails contractDetails;
    private PeriodicData periodicData;

    protected   PeriodicDataWork(final ContractDetails setContractDetails,
        final PeriodicData setPeriodicData) {
        this.contractDetails = setContractDetails;
        this.periodicData = setPeriodicData;
    }
    
    public void dispose(final DatabaseThread databaseThread) {
        return;
    }
    
    public void write(final DatabaseThread databaseThread)
        throws SQLException {
        final Connection dbConnection = databaseThread.getConnection();
        // XXX: We really ought to have a PERIODIC_BAR table that tracks bar length,
        // instead
        final PreparedStatement barStatement
            = dbConnection.prepareStatement("INSERT IGNORE INTO MINUTE_BAR "
                + "(CONTRACT_ID, BAR_TYPE, BAR_START, OPEN, HIGH, LOW, CLOSE) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)");

        barStatement.setInt(1, this.contractDetails.m_summary.m_conId);
        barStatement.setString(2, ConnectionManager.TICKER_TYPE_MIDPOINT);
        barStatement.setTimestamp(3, this.periodicData.startTime);
        barStatement.setDouble(4, this.periodicData.open * this.contractDetails.m_minTick);
        barStatement.setDouble(5, this.periodicData.high * this.contractDetails.m_minTick);
        barStatement.setDouble(6, this.periodicData.low * this.contractDetails.m_minTick);
        barStatement.setDouble(7, this.periodicData.close * this.contractDetails.m_minTick);

        barStatement.executeUpdate();

        return;
    }
}
