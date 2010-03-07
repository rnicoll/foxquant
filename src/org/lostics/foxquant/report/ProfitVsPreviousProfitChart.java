// $Id: ProfitVsPreviousProfitChart.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.report;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.DefaultFontMapper;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.model.ContractManager;

public class ProfitVsPreviousProfitChart extends AbstractPage {

    public void writePage(final Connection dbConnection, final Document document,
        final PdfWriter writer, final List<Roundturn> roundturns,
        final DefaultFontMapper mapper) throws DocumentException {

        final JFreeChart chart = generateChart(roundturns);

        document.add(new Paragraph("Chart of Profit Vs Previous Profit"));
        writeChart(document, writer, chart, mapper);
        document.newPage();

        return;
    }

    public static JFreeChart generateChart(final Collection<Roundturn> roundturns) {
        final JFreeChart chart;
        final NumberAxis xAxis = new NumberAxis("Previous Profit (%)");
        final NumberAxis yAxis = new NumberAxis("Current Profit (%)");
        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        final XYDataset dataset = generateDataset(roundturns);
        final XYPlot plot;

        renderer.setSeriesLinesVisible(0, false);
        plot = new XYPlot(dataset, xAxis, yAxis, renderer);
        chart = new JFreeChart("Profit vs Previous Profit",
            JFreeChart.DEFAULT_TITLE_FONT, plot, false);

        return chart;
    }

    public static XYDataset generateDataset(final Collection<Roundturn> roundturns) {
        final DefaultXYDataset dataset = new DefaultXYDataset();
        final List<ProfitPair> profitPairs = new ArrayList<ProfitPair>();
        final List<ProfitTime> profitsByTime = new ArrayList<ProfitTime>();

        if (roundturns.size() < 2) {
            return dataset;
        }

        for (Roundturn roundturn: roundturns) {
            profitsByTime.add(new ProfitTime(roundturn.getExitTime(),
                roundturn.getProfitPercent()));
        }
        Collections.sort(profitsByTime);

        for (Roundturn roundturn: roundturns) {
            ProfitPair profitPair = new ProfitPair();
            ProfitTime previousProfit = null;

            for (ProfitTime currentProfit: profitsByTime) {
                if (currentProfit.getExitTime().after(roundturn.getEntryTime())) {
                    break;
                }
                previousProfit = currentProfit;
            }
            if (null == previousProfit) {
                continue;
            }

            profitPair.profitOld = previousProfit.getProfit();
            profitPair.profitNew = roundturn.getProfitPercent();
            profitPairs.add(profitPair);
        }

        final double[][] data = new double[2][];
        data[0] = new double[profitPairs.size()];
        data[1] = new double[profitPairs.size()];

        int idx = 0;
        for (ProfitPair profitPair: profitPairs) {
            data[0][idx] = profitPair.profitOld;
            data[1][idx] = profitPair.profitNew;
            idx++;
        }

        dataset.addSeries("Profit", data);

        return dataset;
    }

    private static class ProfitPair extends Object {
        private double profitNew;
        private double profitOld;

        private             ProfitPair() {
        }
    }

    public static class ProfitTime extends Object implements Comparable<ProfitTime> {
        private Timestamp exitTime;
        private double profit;

        private             ProfitTime(final Timestamp setDate,
            final double setProfit) {
            this.exitTime = setDate;
            this.profit = setProfit;
        }

        public Timestamp getExitTime() { return this.exitTime; }
        public double getProfit() { return this.profit; }

        public int compareTo(final ProfitTime stakeB) {
            return this.exitTime.compareTo(stakeB.exitTime);
        }

        public String toString() {
            return Double.toString(profit) + "@"
                + exitTime;
        }
    }
}
