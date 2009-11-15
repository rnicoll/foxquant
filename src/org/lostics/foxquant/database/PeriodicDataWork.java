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

import org.apache.log4j.Logger;

import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.model.PeriodicData;

class PeriodicDataWork extends Object implements DatabaseWork {
    private Logger log;
    
    private ContractDetails contractDetails;
    private PeriodicData periodicData;

    protected   PeriodicDataWork(final ContractDetails setContractDetails,
        final PeriodicData setPeriodicData) {
        this.log  = Logger.getLogger(this.getClass());
        this.contractDetails = setContractDetails;
        this.periodicData = setPeriodicData;
    }
    
    public void dispose(final DatabaseThread databaseThread) {
        return;
    }
    
    public void write(final DatabaseThread databaseThread)
        throws SQLException {
        for (int attemptCount = 0; attemptCount < 2; attemptCount++) {
            final PreparedStatement barStatement = databaseThread.getPeriodicDataStatement();
            
            try {
                barStatement.setInt(1, this.contractDetails.m_summary.m_conId);
                barStatement.setString(2, ConnectionManager.TICKER_TYPE_MIDPOINT);
                barStatement.setTimestamp(3, this.periodicData.startTime);
                barStatement.setDouble(4, this.periodicData.open * this.contractDetails.m_minTick);
                barStatement.setDouble(5, this.periodicData.high * this.contractDetails.m_minTick);
                barStatement.setDouble(6, this.periodicData.low * this.contractDetails.m_minTick);
                barStatement.setDouble(7, this.periodicData.close * this.contractDetails.m_minTick);

                barStatement.executeUpdate();
                
                // Exit here if successful
                
                break;
            } catch(SQLException e) {
                log.warn("Database thread caught exception while writing periodic data to database. Going to try again incase this is temporary issue.", e);
                try {
                    databaseThread.reconnect();
                } catch(Exception internalE) {
                    log.error("Database thread caught exception while attempting recovery.", internalE);
                    break;
                }
            }
        }

        return;
    }
}
