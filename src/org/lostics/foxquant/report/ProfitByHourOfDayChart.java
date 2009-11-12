// $Id: ProfitByHourOfDayChart.java 685 2009-11-08 01:12:26Z  $
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

import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.DefaultFontMapper;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;

import java.util.List;
import java.util.Calendar;
import java.util.Collection;

import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.model.ContractManager;

public class ProfitByHourOfDayChart extends AbstractPage {

    public void writePage(final Connection dbConnection, final Document document,
        final PdfWriter writer, final List<Roundturn> roundturns, 
        final DefaultFontMapper mapper) throws DocumentException {
        final JFreeChart activityChart = ActivityByHourOfDayChart.generateChart(roundturns);
        final JFreeChart profitChart = generateChart(roundturns);

        document.add(new Paragraph("Chart of Profit by Hour of Day"));

        writeChartPair(document, writer, profitChart, activityChart, mapper);

                document.newPage();

        return;
    }

    public static JFreeChart generateChart(final Collection<Roundturn> roundturn) {
        final JFreeChart chart;
        final CategoryAxis xAxis = new CategoryAxis("Hour of Day");
        final NumberAxis yAxis = new NumberAxis("Profit (%)");
        final CategoryItemRenderer renderer = new LayeredBarRenderer();
        final CategoryDataset dataset = generateDataset(roundturn);
        final CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);

        chart = new JFreeChart("Profit by Hour of Day",
            JFreeChart.DEFAULT_TITLE_FONT, plot, false);

        return chart;
    }

    public static CategoryDataset generateDataset(final Collection<Roundturn> roundturns) {
        final Calendar calendar = Calendar.getInstance();
        final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        final double[] profits = new double[24];

        for (Roundturn roundturn: roundturns) {
            calendar.setTime(roundturn.getEntryTime());
            profits[calendar.get(Calendar.HOUR_OF_DAY)] += roundturn.getProfitPercent();
        }

        for (int hour = 0; hour < 24; hour++) {
            dataset.addValue((Number)new Double(profits[hour]), 1, Integer.toString(hour));
        }

        return dataset;
    }
}
