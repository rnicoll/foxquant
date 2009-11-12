// $Id: PerTradeProfitOverTimeChart.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.report;

import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.model.ContractManager;

public class PerTradeProfitOverTimeChart extends Object {
    private PerTradeProfitOverTimeChart() {
    }

    public static JFreeChart generateChart(final Connection dbConnection,
        final Roundturn roundturn, final boolean backtest)
        throws SQLException {
        final JFreeChart chart;
        final DateAxis xAxis = new DateAxis("Time");
        final NumberAxis yAxis = new NumberAxis("Profit");
        final XYAreaRenderer renderer = new XYAreaRenderer();
        final XYDataset dataset = generateDataset(dbConnection, roundturn, backtest);
        final XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);

        chart = new JFreeChart("Profit vs Time",
            JFreeChart.DEFAULT_TITLE_FONT, plot, false);

        return chart;
    }

    public static XYDataset generateDataset(final Connection dbConnection,
        final Roundturn roundturn, final boolean backtest)
        throws SQLException {
        final DefaultXYDataset dataset = new DefaultXYDataset();
        final long entryCount;
        int entryIndex = 0;
        final boolean isBuy = roundturn.getAction().equals(ContractManager.ORDER_ACTION_BUY);
        final double[] profits;
        final long[] times;
        final PreparedStatement statement
            = dbConnection.prepareStatement("SELECT B.BAR_START, B.CLOSE "
                + (backtest ? "FROM MINUTE_BAR B "
                            : "FROM REALTIME_BAR B ")
                + "WHERE B.CONTRACT_ID=? "
                + "AND B.BAR_START>=? "
                + "AND B.BAR_START<=?");

        entryCount = (roundturn.getExitTime().getTime() - roundturn.getEntryTime().getTime())
            / (1000 * ConnectionManager.REAL_TIME_BAR_PERIOD_SECONDS);
        times = new long[(int)entryCount];
        profits = new double[(int)entryCount];

        try {
            final ResultSet rs;

            statement.setInt(1, roundturn.getContractID());
            statement.setTimestamp(2, roundturn.getEntryTime());
            statement.setTimestamp(3, roundturn.getExitTime());
            rs = statement.executeQuery();
            while (rs.next()) {
                times[entryIndex] = rs.getTimestamp("BAR_START").getTime();
                profits[entryIndex] = isBuy
                    ? rs.getDouble("CLOSE") - roundturn.getEntryPrice()
                    : roundturn.getEntryPrice() - rs.getDouble("CLOSE");

                entryIndex++;

                if (entryIndex == entryCount) {
                    break;
                }
            }
            rs.close();
        } finally {
            statement.close();
        }

        final double[][] data = new double[2][];
        data[0] = new double[entryIndex];
        data[1] = new double[entryIndex];
        for (int i = 0; i < entryIndex; i++) {
            data[0][i] = (double)times[i];
            data[1][i] = profits[i];
        }

        dataset.addSeries("Profit", data);

        return dataset;
    }
}
