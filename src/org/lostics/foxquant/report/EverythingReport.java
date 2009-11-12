// $Id: EverythingReport.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.report;

import java.util.List;

public class EverythingReport extends PDFReport {

    final List<AbstractPage> pages = java.util.Arrays.asList(
        new AnalysisPage(),
        //new ProfitOverTimeChart(),
        //new ProfitVsPreviousProfitChart(),
        new ProfitByDayOfWeekChart(),
        new ProfitByHourOfDayChart()
        //new ProfitByMinuteOfHourChart()
    );

    List<AbstractPage> getPages() { return this.pages; }

}
