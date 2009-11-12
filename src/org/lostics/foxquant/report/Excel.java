// $Id: Excel.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.report;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Set;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import org.apache.log4j.Logger;

import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.model.ContractManager;
import org.lostics.foxquant.Configuration;
import org.lostics.foxquant.database.DatabaseUnavailableException;

/**
 * Generates reports as Excel workbooks.
 */
public class Excel extends Object {
    private static final Logger log = Logger.getLogger(Excel.class);

    private static final String[] HEADERS = {
        "Symbol",
        "Action",
        "Entry Time",
        "Entry Price",
        "Exit Time",
        "Exit Price",
        "Gross Profit",
        "Commission",
        "Net Profit",
        "Total Net Profit"
    };

    private static final short[] COLUMN_WIDTHS = {
        2700,
        2700,
        5000,
        2700,
        5000,
        3000,
        3000,
        3000,
        3000,
        3000
    };

    /**
     * Generates an Excel workbook showing trades as pairs of position exit/entry.
     *
     * @param configuration the configuration, used to get a connection to the database.
     */
    public static HSSFWorkbook generateTradeWorkbook(final Configuration configuration)
        throws Configuration.ConfigurationException, DatabaseUnavailableException,
            InterruptedException, SQLException {
        HSSFCell cell;
        final HSSFDataFormat dataFormat;
        final HSSFCellStyle commissionCellStyle;
        final HSSFCellStyle dateCellStyle;
        HSSFRow row;
        final HSSFCellStyle priceCellStyle;
        final HSSFCellStyle profitCellStyle;
        final HSSFSheet sheet;
        final Timestamp since = getStartOfWeek();
        final HSSFWorkbook workbook;

        assert null != configuration;

        workbook = new HSSFWorkbook();
        sheet = workbook.createSheet();
        dataFormat = workbook.createDataFormat();

        commissionCellStyle = workbook.createCellStyle();
        commissionCellStyle.setDataFormat(dataFormat.getFormat("0.000%"));
        dateCellStyle = workbook.createCellStyle();
        dateCellStyle.setDataFormat(dataFormat.getFormat("dd/mm/yyyy hh:mm:ss"));
        priceCellStyle = workbook.createCellStyle();
        priceCellStyle.setDataFormat(dataFormat.getFormat("#,##0.0000#"));
        profitCellStyle = workbook.createCellStyle();
        profitCellStyle.setDataFormat(dataFormat.getFormat("0.00%;[Red]-0.00%"));

        row = sheet.createRow(0);

        for (int headerIdx = 0; headerIdx < HEADERS.length; headerIdx++) {
            cell = row.createCell(headerIdx);
            cell.setCellValue(new HSSFRichTextString(HEADERS[headerIdx]));
            sheet.setColumnWidth(headerIdx, COLUMN_WIDTHS[headerIdx]);
        }

        final Connection dbConnection = configuration.getDBConnection();

        try {
            final List<Roundturn> roundturns = Roundturn.get(dbConnection, since);
            short rowIdx = 1;

            for (Roundturn roundturn: roundturns) {
                HSSFCellStyle cellStyle;

                row = sheet.createRow(rowIdx);

                cell = row.createCell(0);
                cell.setCellValue(new HSSFRichTextString(roundturn.getLocalSymbol()));

                cell = row.createCell(1);
                cell.setCellValue(new HSSFRichTextString(roundturn.getAction()));

                cell = row.createCell(2);
                cell.setCellValue(roundturn.getEntryTime());
                cell.setCellStyle(dateCellStyle);

                cell = row.createCell(3);
                cell.setCellValue(roundturn.getEntryPrice());
                cell.setCellStyle(priceCellStyle);

                cell = row.createCell(4);
                cell.setCellValue(roundturn.getExitTime());
                cell.setCellStyle(dateCellStyle);

                cell = row.createCell(5);
                cell.setCellValue(roundturn.getExitPrice());
                cell.setCellStyle(priceCellStyle);

                cell = row.createCell(6);
                cell.setCellValue(roundturn.getProfitPercent() / 100);
                cell.setCellStyle(profitCellStyle);

                cell = row.createCell(7);
                cell.setCellValue(0.00004);
                cell.setCellStyle(commissionCellStyle);

                cell = row.createCell(8);
                cell.setCellFormula("G"
                    + (rowIdx + 1) + "-H"
                    + (rowIdx + 1));
                cell.setCellStyle(profitCellStyle);

                cell = row.createCell(9);
                if (rowIdx == 1) {
                    cell.setCellFormula("I"
                        + (rowIdx + 1));
                } else {
                    cell.setCellFormula("J"
                        + rowIdx + "+I"
                        + (rowIdx + 1));
                }
                cell.setCellStyle(profitCellStyle);

                rowIdx++;
            }
        } finally {
            dbConnection.close();
        }

        return workbook;
    }

    public static Timestamp getStartOfWeek() {
        // We are only interested in roundturns since the start of the week,
        // which we consider to be 00:00 on Sunday.
        final Calendar calendar = Calendar.getInstance();

        calendar.setTime(new Date(System.currentTimeMillis()));
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            calendar.add(Calendar.DATE, -1);
        }
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return new Timestamp(calendar.getTime().getTime());
    }
}
