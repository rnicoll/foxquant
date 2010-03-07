// $Id: PerTradePDF.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.report;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.itextpdf.text.FontFactory;
import com.itextpdf.text.pdf.DefaultFontMapper;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;

import org.jfree.chart.JFreeChart;

import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.model.ContractManager;
import org.lostics.foxquant.Configuration;
import org.lostics.foxquant.database.DatabaseUnavailableException;

/**
 * Generates PDF reports on trades, at a page per trade.
 */
public class PerTradePDF extends AbstractPage implements Report {
    private final int WIDTH = 500;
    private final int HEIGHT = 400;

    private DecimalFormat currencyFormat = new DecimalFormat("#,###0.00000");

    private final boolean backtest;

    public PerTradePDF() {
        this(false);
    }

    public PerTradePDF(final boolean bt) {
        this.backtest = bt;
    }


    public void writePage(final Connection dbConnection, final Document document,
        final PdfWriter writer, final List<Roundturn> roundturns,
        final DefaultFontMapper mapper) throws DocumentException, SQLException {

        boolean first = true;
        for (Roundturn roundturn: roundturns) {
            if (!first) {
                document.newPage();
            } else {
                first = false;
            }

            generatePage(dbConnection, document, writer, roundturns, mapper, roundturn);
        }

        return;
    }


    public void writeReport(Connection db, java.io.File file, List<Roundturn> roundturns) 
        throws java.io.IOException, ReportException, SQLException {
        PDFReport.writePDFReport(db, file, roundturns, java.util.Arrays.asList(this));
    }

    public void generateReport(final Connection dbConnection,
        final Document document, final PdfWriter writer,
        final List<Roundturn> roundturns)
        throws DocumentException, SQLException {
        final DefaultFontMapper mapper = new DefaultFontMapper();

        FontFactory.registerDirectories();

        boolean first = true;
        for (Roundturn roundturn: roundturns) {
            if (!first) {
                document.newPage();
            } else {
                first = false;
            }

            generatePage(dbConnection, document, writer, roundturns, mapper, roundturn);
        }
    }

    private void generatePage(final Connection dbConnection,
        final Document document, final PdfWriter writer,
        final List<Roundturn> roundturns, final DefaultFontMapper mapper,
        final Roundturn currentRoundturn)
        throws DocumentException, SQLException {
        final JFreeChart chart;
        final PdfPTable table = new PdfPTable(4);

        table.addCell("Contract ID");
        table.addCell(Integer.toString(currentRoundturn.getContractID()));
        table.addCell("Symbol");
        table.addCell(currentRoundturn.getLocalSymbol());

        table.addCell("Entry time");
        table.addCell(currentRoundturn.getEntryTime().toString());
        table.addCell("Entry price");
        table.addCell(currencyFormat.format(currentRoundturn.getEntryPrice()));

        table.addCell("Exit time");
        table.addCell(currentRoundturn.getExitTime().toString());
        table.addCell("Exit price");
        table.addCell(currencyFormat.format(currentRoundturn.getExitPrice()));

        table.addCell("Direction");
        table.addCell(currentRoundturn.getAction());
        table.addCell("Profit");
        if (currentRoundturn.getAction().equals(ContractManager.ORDER_ACTION_BUY)) {
            table.addCell(currencyFormat.format(currentRoundturn.getExitPrice() - currentRoundturn.getEntryPrice()));
        } else {
            table.addCell(currencyFormat.format(currentRoundturn.getEntryPrice() - currentRoundturn.getExitPrice()));
        }

        document.add(table);

        chart = PerTradeProfitOverTimeChart.generateChart(dbConnection, currentRoundturn, this.backtest);

        final PdfContentByte cb = writer.getDirectContent();
        final PdfTemplate tp = cb.createTemplate(WIDTH, HEIGHT);
        final Graphics2D g2 = tp.createGraphics(WIDTH, HEIGHT, mapper);

        tp.setWidth(WIDTH);
        tp.setHeight(HEIGHT);

        chart.draw(g2, new Rectangle(0, 0, WIDTH, HEIGHT));

        g2.dispose();
        cb.addTemplate(tp, 50, 250);

        return;
    }
}
