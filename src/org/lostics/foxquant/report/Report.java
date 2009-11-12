// $Id: Report.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.report;

import java.sql.Connection;
import java.sql.SQLException;

import java.io.File;
import java.io.IOException;

import java.util.List;

import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.model.ContractManager;
import org.lostics.foxquant.Configuration;
import org.lostics.foxquant.database.DatabaseUnavailableException;

public interface Report {

    public void writeReport(final Connection db, final File file, final List<Roundturn> roundturns)
        throws IOException, ReportException, SQLException;

}

