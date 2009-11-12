// $Id: GeneratePDFReport.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.Document;

import org.lostics.foxquant.database.DatabaseUnavailableException;
import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.model.ContractManager;
import org.lostics.foxquant.report.Excel;
import org.lostics.foxquant.report.EverythingReport;
import org.lostics.foxquant.report.ReportException;
import org.lostics.foxquant.report.Roundturn;

public class GeneratePDFReport extends Object {
    public static void main(final String[] argv)
        throws Configuration.ConfigurationException, DatabaseUnavailableException,
            IOException, SQLException, ReportException {
        final Configuration configuration = FoxQuant.getConfiguration();
        final Connection dbConnection;
        final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmm");

        if (null == configuration) {
            return;
        }

        dbConnection = configuration.getDBConnection();
        try {
            final List<Roundturn> roundturns
                = Roundturn.get(dbConnection, Excel.getStartOfWeek());
            final java.io.File file = new java.io.File("trades_"
                + dateFormat.format(new Date()) + ".pdf");
            new EverythingReport().writeReport(dbConnection, file, roundturns);
        } finally {
            dbConnection.close();
        }

        return;
    }
}
