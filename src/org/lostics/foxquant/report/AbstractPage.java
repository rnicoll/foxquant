// $Id: AbstractPage.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.report;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.lowagie.text.pdf.DefaultFontMapper;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;

import org.jfree.chart.JFreeChart;

import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.model.ContractManager;
import org.lostics.foxquant.Configuration;
import org.lostics.foxquant.database.DatabaseUnavailableException;

/**
 * Support class / definition for a page in a PDF document.
 */
public abstract class AbstractPage {
    protected final int SINGLE_WIDTH = 480;
    protected final int SINGLE_HEIGHT = 400;

    protected final int PAIR_WIDTH = SINGLE_WIDTH;
    protected final int PAIR_HEIGHT_A = 400;
    protected final int PAIR_HEIGHT_B = 100;

    protected DecimalFormat currencyFormat;

    protected AbstractPage() {
     this.currencyFormat = new DecimalFormat("#,###0.00000");
    }


    public abstract void writePage(final Connection dbConnection, final Document document, 
            final PdfWriter writer, final List<Roundturn> roundturns,
            final DefaultFontMapper mapper) throws java.io.IOException, DocumentException, SQLException;

    protected void writeChart(final Document document, final PdfWriter writer,
        final JFreeChart chart, final DefaultFontMapper mapper)
        throws DocumentException {
        final PdfContentByte cb = writer.getDirectContent();
        final Graphics2D g2;

        g2 = cb.createGraphics(SINGLE_WIDTH + 60, SINGLE_HEIGHT + 200, mapper);
        chart.draw(g2, new Rectangle(60, 0, SINGLE_WIDTH, SINGLE_HEIGHT));

        g2.dispose();
    }

    protected void writeChartPair(final Document document, final PdfWriter writer,
        final JFreeChart chart, final JFreeChart subChart,
        final DefaultFontMapper mapper)
        throws DocumentException {
        final PdfContentByte cb = writer.getDirectContent();
        final Graphics2D g2;

        g2 = cb.createGraphics(PAIR_WIDTH + 120, PAIR_HEIGHT_A + PAIR_HEIGHT_B + 100, mapper);
        chart.draw(g2, new Rectangle(60, 0, PAIR_WIDTH, PAIR_HEIGHT_A));
        subChart.draw(g2, new Rectangle(60, PAIR_HEIGHT_A, PAIR_WIDTH, PAIR_HEIGHT_B));

        g2.dispose();
    }

}
