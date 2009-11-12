// $Id: ProfitByDayOfWeekChart.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.report;

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.LayeredBarRenderer;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import com.lowagie.text.pdf.DefaultFontMapper;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;

import java.sql.Connection;

import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.model.ContractManager;

public class ProfitByDayOfWeekChart extends AbstractPage {

    public void writePage(final Connection dbConnection, 
        final Document document, final PdfWriter writer,
        final List<Roundturn> roundturns, final DefaultFontMapper mapper) throws DocumentException {
        final JFreeChart chart;
        
        document.add(new Paragraph("Chart of Profit by Day of Week"));

        chart = generateChart(roundturns);
        writeChart(document, writer, chart, mapper);

        document.newPage();

        return;
    }

    public static JFreeChart generateChart(final Collection<Roundturn> roundturn) {
        final JFreeChart chart;
        final CategoryAxis xAxis = new CategoryAxis("Day of Week");
        final NumberAxis yAxis = new NumberAxis("Profit (%)");
        final CategoryItemRenderer renderer = new LayeredBarRenderer();
        final CategoryDataset dataset = generateDataset(roundturn);
        final CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);

        chart = new JFreeChart("Profit by Day of Week",
            JFreeChart.DEFAULT_TITLE_FONT, plot, false);

        return chart;
    }

    public static CategoryDataset generateDataset(final Collection<Roundturn> roundturns) {
        final Calendar calendar = Calendar.getInstance();
        final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        // While the exact numbers used by Calendar are unclear, I'm going
        // to bet they run 0-6, or 1-7. This isn't a fantastic plan, I'll
        // admit, but does for now...
        final double[] profits = new double[8];

        for (Roundturn roundturn: roundturns) {
            calendar.setTime(roundturn.getEntryTime());
            profits[calendar.get(Calendar.DAY_OF_WEEK)] += roundturn.getProfitPercent();
        }

        dataset.addValue((Number)new Double(profits[Calendar.SUNDAY]), 1, "Sunday");
        dataset.addValue((Number)new Double(profits[Calendar.MONDAY]), 1, "Monday");
        dataset.addValue((Number)new Double(profits[Calendar.TUESDAY]), 1, "Tuesday");
        dataset.addValue((Number)new Double(profits[Calendar.WEDNESDAY]), 1, "Wednesday");
        dataset.addValue((Number)new Double(profits[Calendar.THURSDAY]), 1, "Thursday");
        dataset.addValue((Number)new Double(profits[Calendar.FRIDAY]), 1, "Friday");
        dataset.addValue((Number)new Double(profits[Calendar.SATURDAY]), 1, "Saturday");

        return dataset;
    }
}
