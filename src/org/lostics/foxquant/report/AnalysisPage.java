// $Id: AnalysisPage.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.report;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.DecimalFormat;

import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.DefaultFontMapper;
import com.lowagie.text.Cell;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Table;

import java.util.List;
import java.util.Calendar;
import java.util.Collection;

import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.model.ContractManager;

public class AnalysisPage extends AbstractPage {
    private Font DEFAULT_FONT = FontFactory.getFont(FontFactory.TIMES, 12);

    private Table generateTable(final List<Roundturn> roundturns, 
        final DefaultFontMapper mapper) throws DocumentException {
        final double[] equity = new double[roundturns.size() + 1];
        int equityIdx = 1;
        final DecimalFormat profitFormat = new DecimalFormat("0.0000#");
        double maxDrawdown = 0;
        double maxLoss = 0;
        double maxProfit = 0;
        double maxPullup = 0;
        int numProfitable = 0;
        int numLosing = 0;
        final Streak losingStreak = new Streak();
        final Streak winningStreak = new Streak();
        double totalChange = 0;
        double totalProfits = 0;
        double totalLosses = 0;
        final Table table;

        equity[0] = 0.0;

        for (Roundturn currentRoundturn: roundturns) {
            final double currentProfit = currentRoundturn.getProfit();

            if (currentProfit > 0.0000) {
                numProfitable++;
                totalProfits += currentProfit;
                if (currentProfit > maxProfit) {
                    maxProfit = currentProfit;
                }
                winningStreak.streakContinues();
                losingStreak.newStreak();
            } else if (currentProfit < 0.0000) {
                numLosing++;
                totalLosses += currentProfit;
                if (currentProfit < maxLoss) {
                    maxLoss = currentProfit;
                }
                losingStreak.streakContinues();
                winningStreak.newStreak();
            }
            // Anything between -0.0001 and +0.0001 inclusive we consider
            // to have gone nowhere.

            totalChange += currentProfit;
            equity[equityIdx] = equity[equityIdx - 1] + currentProfit;
            if (equity[equityIdx] < maxDrawdown) {
                maxDrawdown = equity[equityIdx];
            } else if (equity[equityIdx] > maxPullup) {
                maxPullup = equity[equityIdx];
            }

            equityIdx++;
        }

        table = new Table(2);
        table.setPadding(2);
        table.endHeaders();

        table.addCell(new Phrase("Total p/l", DEFAULT_FONT));
        table.addCell(new Phrase(profitFormat.format(totalChange), DEFAULT_FONT));

        table.addCell(new Phrase("Total trades", DEFAULT_FONT));
        table.addCell(new Phrase(Integer.toString(roundturns.size()), DEFAULT_FONT));
        table.addCell(new Phrase("Profitable trades", DEFAULT_FONT));
        table.addCell(new Phrase(Integer.toString(numProfitable), DEFAULT_FONT));
        table.addCell(new Phrase("Losing trades", DEFAULT_FONT));
        table.addCell(new Phrase(Integer.toString(numLosing), DEFAULT_FONT));

        table.addCell(new Phrase("Total profit", DEFAULT_FONT));
        table.addCell(new Phrase(profitFormat.format(totalProfits), DEFAULT_FONT));
        table.addCell(new Phrase("Total loss", DEFAULT_FONT));
        table.addCell(new Phrase(profitFormat.format(totalLosses), DEFAULT_FONT));

        table.addCell(new Phrase("Avg. profitable trade", DEFAULT_FONT));
        table.addCell(new Phrase(profitFormat.format(totalProfits / numProfitable), DEFAULT_FONT));
        table.addCell(new Phrase("Avg. losing trade", DEFAULT_FONT));
        table.addCell(new Phrase(profitFormat.format(totalLosses / numLosing), DEFAULT_FONT));

        table.addCell(new Phrase("Largest profit", DEFAULT_FONT));
        table.addCell(new Phrase(profitFormat.format(maxProfit), DEFAULT_FONT));
        table.addCell(new Phrase("Largest loss", DEFAULT_FONT));
        table.addCell(new Phrase(profitFormat.format(maxLoss), DEFAULT_FONT));

        table.addCell(new Phrase("Longest winning streak", DEFAULT_FONT));
        table.addCell(new Phrase(Integer.toString(winningStreak.getMaxStreak()), DEFAULT_FONT));
        table.addCell(new Phrase("Longest losing streak", DEFAULT_FONT));
        table.addCell(new Phrase(Integer.toString(losingStreak.getMaxStreak()), DEFAULT_FONT));

        table.addCell(new Phrase("Max pullup", DEFAULT_FONT));
        table.addCell(new Phrase(profitFormat.format(maxPullup), DEFAULT_FONT));
        table.addCell(new Phrase("Max drawdown", DEFAULT_FONT));
        table.addCell(new Phrase(profitFormat.format(maxDrawdown), DEFAULT_FONT));

        return table;
    }

    public void writePage(final Connection dbConnection, final Document document,
        final PdfWriter writer, final List<Roundturn> roundturns, 
        final DefaultFontMapper mapper) throws DocumentException {
        document.add(new Paragraph("Trade Analysis"));

        document.add(generateTable(roundturns, mapper));

        document.newPage();

        return;
    }

    private static class Streak extends Object {
        private int currentStreak = 0;
        private int maxStreak = 0;

        private      Streak() {
        }

        private int getCurrentStreak() {
            return this.currentStreak;
        }

        private int getMaxStreak() {
            return this.maxStreak;
        }

        private void     newStreak() {
            this.currentStreak = 0;
        }

        private void streakContinues() {
            this.currentStreak++;
            if (this.currentStreak > this.maxStreak) {
                this.maxStreak = this.currentStreak;
            }
        }
    }
}
