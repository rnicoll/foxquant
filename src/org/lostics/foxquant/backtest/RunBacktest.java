// $Id: RunBacktest.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.backtest;

import java.io.IOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.List;

import com.ib.client.ContractDetails;
import com.ib.client.Contract;
import com.ib.client.Order;

import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;

import org.apache.log4j.Logger;

import org.lostics.foxquant.ib.ContractFactory;
import org.lostics.foxquant.model.*;
import org.lostics.foxquant.strategy.CatchingDaggersFactory;
import org.lostics.foxquant.strategy.CatchingDaggers;
import org.lostics.foxquant.report.*;
import org.lostics.foxquant.*;

/** A Mostly disposable runner for a backtest.
 * This class exists mostly as a worked example of how to run a single 
 * backtest, and for dev purposed.
 * A real backtesting engine would do all sorts of clever things, 
 * like reporting stats, that this does not.
 */

public class RunBacktest implements Runnable {
    private static final String[][] CURRENCIES = {
        {"AUD", "NZD"},
        {"AUD", "USD"},
        {"EUR", "CHF"},
        {"EUR", "GBP"},
        {"EUR", "USD"},
        {"GBP", "USD"},
        {"USD", "CAD"},
        {"USD", "CHF"},
        {"USD", "JPY"}
    };
    
    private static final Logger log = Logger.getLogger(RunBacktest.class);

    private final BacktestContractManager contractManager;
    private final Connection    db;
    private final ContractDetails contractDetails;
    private final Contract      contract;
    private final Timestamp     startTime;
    private final Timestamp     endTime;

    private       List<Roundturn> roundturns;

    public static void main(String[] args) throws Exception {
        log.debug("Running backtest...");
        final Configuration config = FoxQuant.getConfiguration();

        final Timestamp start;
        final Timestamp end;
        final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        final String startStr = "2007-10-01";
        final String endStr = "2008-10-22";

        start   = new Timestamp(dateFormat.parse(startStr).getTime());
        end     = new Timestamp(dateFormat.parse(endStr).getTime());

        for (int currencyIdx = 0; currencyIdx < CURRENCIES.length; currencyIdx++) {
            final String baseCurrency = CURRENCIES[currencyIdx][0];
            final String purchaseCurrency = CURRENCIES[currencyIdx][1]; 

            final RunBacktest backtest = new RunBacktest(config, baseCurrency, purchaseCurrency, start, end);

            backtest.run();
            backtest.generateReport("backtest_" + startStr + "_" + endStr + "_"
                + baseCurrency + purchaseCurrency + ".pdf");
            backtest.close();
            System.gc();
        }
    }

    public RunBacktest(final Configuration config, final String baseCurrency, final String purchaseCurrency, 
            final Timestamp startTime, final Timestamp setEndTime) throws Exception {
            this(config, "BID", baseCurrency, purchaseCurrency, startTime, setEndTime);
    }
       
    public RunBacktest(final Configuration config, 
            final String bidBarType,
            final String baseCurrency, final String purchaseCurrency, 
            final Timestamp startTime, final Timestamp setEndTime)
            throws Exception {
        this.db = config.getDBConnection();
        this.contractDetails  = ContractFactory.getForexContractDetails(db, baseCurrency, purchaseCurrency);
        this.contract = this.contractDetails.m_summary;
        log.debug("Using contract " + this.contract.m_conId);
        this.startTime      = startTime;
        this.endTime        = setEndTime;
        log.debug("between " + startTime + " and " + endTime);

        // XXX: Can this SQL be moved out of the constructor?
        // Somewhere where it can be run over multiple strategies?
        final long dataPeriod = 60 * 1000; // one minute
        final PreparedStatement statement = db.prepareStatement(
            "SELECT BAR_START START_TIME, OPEN, HIGH, LOW, CLOSE "
            + "FROM MINUTE_BAR "
            + "WHERE CONTRACT_ID = ? "
                + "AND BAR_TYPE = ? "
                + "AND BAR_START >= ? "
                + "AND BAR_START <= ? "
            , ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY );

        statement.setFetchSize(Integer.MIN_VALUE);

        statement.setInt(1, contract.m_conId);
        statement.setString(2, bidBarType);

        statement.setTimestamp(3, this.startTime);
        statement.setTimestamp(4, this.endTime);

        this.contractManager = new BacktestContractManager(this.contractDetails,
            statement.executeQuery(), dataPeriod);
        
        // This also sets up the contractManager.
        final Strategy strategy = new CatchingDaggersFactory().getStrategy(null, this.contractManager);
    }

    public void run() { 
        log.debug("Running contract manager");
        this.contractManager.run();
        this.roundturns = this.contractManager.getRoundturns();
        log.info("Got " + this.roundturns.size() + " roundturns.");
    }

    
    public  void generateReport(final String filename) 
        throws DocumentException, IOException, SQLException, ReportException {
        final java.io.File file = new java.io.File(filename);

        PDFReport.writePDFReport(db, file, roundturns, java.util.Arrays.asList(
            new AnalysisPage(),
            //new ProfitOverTimeChart(),
            //new ProfitVsPreviousProfitChart(),
            new ProfitByDayOfWeekChart(),
            new ProfitByHourOfDayChart()
            //new ProfitByMinuteOfHourChart()
        ));

        return;
    }

    public void close() throws SQLException {
        this.db.close();
    }
}
