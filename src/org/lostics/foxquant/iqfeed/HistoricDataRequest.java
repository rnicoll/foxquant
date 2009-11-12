// $Id: HistoricDataRequest.java 706 2009-11-11 10:41:13Z jrn $
package org.lostics.foxquant.iqfeed;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.ib.client.ContractDetails;
import com.ib.client.Contract;

import org.apache.log4j.Logger;

import org.lostics.foxquant.database.DatabaseThread;
import org.lostics.foxquant.model.HistoricBarSize;
import org.lostics.foxquant.model.HistoricalDataConsumer;
import org.lostics.foxquant.model.HistoricalDataSource;
import org.lostics.foxquant.model.PeriodicData;

final class HistoricDataRequest extends Object implements IQFeedWork {
    private final int DATAPOINTS_PER_SEND = 30;
    private final int READ_BUFFER_SIZE = 200;

    private static final Logger log = Logger.getLogger(HistoricDataRequest.class);

    private final HistoricalDataConsumer dataConsumer;
    private final ContractDetails contractDetails;
    private final Date startDate;
    private final Date endDate;
    private final int intervalSeconds;
    
    protected           HistoricDataRequest(final HistoricalDataConsumer setConsumer,
        final ContractDetails setContractDetails, final Date setStartDate,
        final Date setEndDate, final int setIntervalSeconds) {
        super();
        
        this.dataConsumer = setConsumer;
        this.contractDetails = setContractDetails;
        this.startDate = setStartDate;
        this.endDate = setEndDate;
        this.intervalSeconds = setIntervalSeconds;
    }
    
    public void doWork(final IQFeedGateway gateway)
        throws InterruptedException, IOException, IQFeedException {
        final DatabaseThread databaseThread = gateway.getDatabaseThread();
        final DateFormat readFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        final DateFormat writeFormat = new SimpleDateFormat("yyyyMMdd HHmmss");
        final BufferedReader reader = gateway.getLookupReader();
        final BufferedWriter writer = gateway.getLookupWriter();
        final Contract contract = this.contractDetails.m_summary;
        // XXX: Need to look these up from the database.
        final String symbol = contract.m_symbol + contract.m_currency + ".FXCM";
        
        readFormat.setTimeZone(IQFeedGateway.TIMEZONE);
        writeFormat.setTimeZone(IQFeedGateway.TIMEZONE);
        
        writer.write("HIT,"
            + symbol + ","
            + this.intervalSeconds + ","
            + writeFormat.format(this.startDate) + ","
            + writeFormat.format(this.endDate) + ","
            + /* Max datapoints + */ ","
            + /* Begin filter time + */ ","
            + /* End filter time + */ ","
            + /* Data direction + */ ","
            + /* Request ID + */ ","
            + DATAPOINTS_PER_SEND + "\r\n");
        writer.flush();
        
        String response = reader.readLine();
        while (response != null) {
                
            if (response.equals(IQFeedGateway.MSG_END)) {
                this.dataConsumer.handleHistoricPriceFinished();
                return;
            }
            
            final String parts[] = response.split(",");
            final Date responseTime;
            final double high = Double.valueOf(parts[1]);
            final double low = Double.valueOf(parts[2]);
            final double open = Double.valueOf(parts[3]);
            final double close = Double.valueOf(parts[4]);
            final int fixedHigh = (int)(high / this.contractDetails.m_minTick);
            final int fixedLow = (int)(low / this.contractDetails.m_minTick);
            final int fixedOpen = (int)(open / this.contractDetails.m_minTick);
            final int fixedClose = (int)(close / this.contractDetails.m_minTick);
            final PeriodicData data;
            
            try {
                responseTime = readFormat.parse(parts[0]);
            } catch(java.text.ParseException e) {
                throw new IQFeedException("Cannot parse data timestamp \""
                    + parts[0] + "\".");
            }
            data = new PeriodicData(new Timestamp(responseTime.getTime()),
                fixedOpen, fixedHigh, fixedLow, fixedClose);
            this.dataConsumer.handleHistoricPrice(data, false);
            databaseThread.queuePeriodicData(this.contractDetails,
                data);
            
            // XXX: Need timeout, connection closing etc. handling
            response = reader.readLine();
        }
    }
}
