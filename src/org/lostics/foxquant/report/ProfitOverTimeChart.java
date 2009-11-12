// $Id: ProfitOverTimeChart.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.report;

import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import java.sql.Connection;
import java.sql.SQLException;

import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.DefaultFontMapper;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;

import java.util.List;
import java.util.Collection;

import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.model.ContractManager;

public class ProfitOverTimeChart extends AbstractPage {

    public void writePage(final Connection dbConnection, final Document document,
        final PdfWriter writer, final List<Roundturn> roundturns,
        final DefaultFontMapper mapper) throws DocumentException {

        final JFreeChart chart = generateChart(roundturns);

        document.add(new Paragraph("Chart of Profit By Hour of Day"));
        writeChart(document, writer, chart, mapper);
        document.newPage();

        return;
    }



    public static JFreeChart generateChart(final Collection<Roundturn> roundturns) {
        final JFreeChart chart;
        final DateAxis xAxis = new DateAxis("Time");
        final NumberAxis yAxis = new NumberAxis("Profit (%)");
        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        final XYDataset dataset = generateDataset(roundturns);
        final XYPlot plot;

        renderer.setSeriesLinesVisible(0, false);
        plot = new XYPlot(dataset, xAxis, yAxis, renderer);
        chart = new JFreeChart("Profit vs Entry Time",
            JFreeChart.DEFAULT_TITLE_FONT, plot, false);

        return chart;
    }

    public static XYDataset generateDataset(final Collection<Roundturn> roundturns) {
        final DefaultXYDataset dataset = new DefaultXYDataset();
        int idx = 0;
        final double[] profits;
        final double[] times;

        profits = new double[roundturns.size()];
        times = new double[roundturns.size()];

        for (Roundturn roundturn: roundturns) {
            times[idx] = roundturn.getEntryTime().getTime();
            profits[idx] = roundturn.getProfitPercent();
            idx++;
        }

        final double[][] data = new double[2][];
        data[0] = times;
        data[1] = profits;

        dataset.addSeries("Profit", data);

        return dataset;
    }
}
