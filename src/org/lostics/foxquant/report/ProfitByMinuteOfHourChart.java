// $Id: ProfitByMinuteOfHourChart.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.report;

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.LayeredBarRenderer;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import java.sql.Connection;
import java.sql.SQLException;

import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.DefaultFontMapper;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;

import java.util.List;
import java.util.Calendar;
import java.util.Collection;

import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.model.ContractManager;

public class ProfitByMinuteOfHourChart extends AbstractPage {

   public void writePage(final Connection dbConnection, final Document document,
        final PdfWriter writer, final List<Roundturn> roundturns,
        final DefaultFontMapper mapper) throws DocumentException {

        final JFreeChart chart = generateChart(roundturns);

        document.add(new Paragraph("Chart of Profit by Minute of Hour"));
        writeChart(document, writer, chart, mapper);
        document.newPage();

        return;
    }


    public static JFreeChart generateChart(final Collection<Roundturn> roundturn) {
        final JFreeChart chart;
        final CategoryAxis xAxis = new CategoryAxis("Minute of Hour");
        final NumberAxis yAxis = new NumberAxis("Profit (%)");
        final CategoryItemRenderer renderer = new LayeredBarRenderer();
        final CategoryDataset dataset = generateDataset(roundturn);
        final CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);

        chart = new JFreeChart("Profit by Minute of Hour",
            JFreeChart.DEFAULT_TITLE_FONT, plot, false);

        return chart;
    }

    public static CategoryDataset generateDataset(final Collection<Roundturn> roundturns) {
        final Calendar calendar = Calendar.getInstance();
        final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        final double[] profits = new double[60];

        for (Roundturn roundturn: roundturns) {
            calendar.setTime(roundturn.getEntryTime());
            profits[calendar.get(Calendar.MINUTE)] += roundturn.getProfitPercent();
        }

        for (int minute = 0; minute < profits.length; minute++) {
            dataset.addValue((Number)new Double(profits[minute]), 1, Integer.toString(minute));
        }

        return dataset;
    }
}
