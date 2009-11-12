// $Id: GenerateExcelReport.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import org.lostics.foxquant.database.DatabaseUnavailableException;
import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.model.ContractManager;
import org.lostics.foxquant.report.Excel;

public class GenerateExcelReport extends Object {
    public static void main(final String[] argv)
        throws Configuration.ConfigurationException, DatabaseUnavailableException,
            InterruptedException, IOException, SQLException {
        final Configuration configuration = FoxQuant.getConfiguration();
        final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmm");
        final HSSFWorkbook workbook;

        if (null == configuration) {
            return;
        }

        workbook = Excel.generateTradeWorkbook(configuration);

        final java.io.File file = new java.io.File("trades_"
            + dateFormat.format(new Date()) + ".xls");
        final java.io.OutputStream outputStream = new java.io.FileOutputStream(file);
        try {
            workbook.write(outputStream);
        } finally {
            outputStream.close();
        }

        return;
    }
}
