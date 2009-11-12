// $Id: PriceTick.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.database;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

import org.apache.log4j.Logger;

import org.lostics.foxquant.model.ContractManager;
import org.lostics.foxquant.model.TickData;

class PriceTick extends Object implements DatabaseWork {
    private Logger log;
    
    protected ContractManager contractManager;
    protected TickData tickData;

    protected         PriceTick() {
        log  = Logger.getLogger(this.getClass());
    }
    
    public void dispose(final DatabaseThread databaseThread) {
        databaseThread.poolPriceTick(this);
    }
    
    public void write(final DatabaseThread databaseThread)
        throws DatabaseUnavailableException, SQLException {
        final TickData tick = this.tickData;
        boolean completed = false;
        int errorCount = 0;
        PreparedStatement priceStatement = databaseThread.getPriceStatement();

        while (!completed &&
            errorCount < 2) {
            try {
                priceStatement.setInt(1, this.contractManager.getContractID());
                priceStatement.setTimestamp(2, new java.sql.Timestamp(tick.timeMillis));
                if (tick.bidPrice != null) {
                    priceStatement.setDouble(3, tick.bidPrice);
                } else {
                    priceStatement.setNull(3, Types.DOUBLE);
                }
                if (tick.askPrice != null) {
                    priceStatement.setDouble(4, tick.askPrice);
                } else {
                    priceStatement.setNull(4, Types.DOUBLE);
                }
                if (tick.bidSize != null) {
                    priceStatement.setDouble(5, tick.bidSize);
                } else {
                    priceStatement.setNull(5, Types.INTEGER);
                }
                if (tick.askSize != null) {
                    priceStatement.setDouble(6, tick.askSize);
                } else {
                    priceStatement.setNull(6, Types.INTEGER);
                }
                priceStatement.executeUpdate();
                completed = true;
            } catch(SQLException e) {
                log.warn("Database thread caught exception while writing tick to database. Going to try again incase this is temporary issue.", e);
                try {
                    if (errorCount < 1) {
                        // Attempt recovery
                        databaseThread.reconnect();
                        priceStatement = databaseThread.getPriceStatement();

                        errorCount++;
                    }
                } catch(Exception internalE) {
                    log.error("Database thread caught exception while attempting recovery.", internalE);
                }
            }
        }

        return;
    }
}
