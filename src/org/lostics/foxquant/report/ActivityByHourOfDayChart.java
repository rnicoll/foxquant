// $Id: ActivityByHourOfDayChart.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.report;

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.LayeredBarRenderer;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import java.util.Calendar;
import java.util.Collection;

import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.model.ContractManager;

public class ActivityByHourOfDayChart extends Object {
    private ActivityByHourOfDayChart() {
    }

    public static JFreeChart generateChart(final Collection<Roundturn> roundturn) {
        final JFreeChart chart;
        final CategoryAxis xAxis = new CategoryAxis("Hour of Day");
        final NumberAxis yAxis = new NumberAxis("Trades");
        final CategoryItemRenderer renderer = new LayeredBarRenderer();
        final CategoryDataset dataset = generateDataset(roundturn);
        final CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);

        chart = new JFreeChart(null, null, plot, false);

        return chart;
    }

    public static CategoryDataset generateDataset(final Collection<Roundturn> roundturns) {
        final Calendar calendar = Calendar.getInstance();
        final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        final int[] activity = new int[24];

        for (Roundturn roundturn: roundturns) {
            calendar.setTime(roundturn.getEntryTime());
            activity[calendar.get(Calendar.HOUR_OF_DAY)]++;
        }

        for (int hour = 0; hour < 24; hour++) {
            dataset.addValue((Number)new Double(activity[hour]), 1, Integer.toString(hour));
        }

        return dataset;
    }
}
