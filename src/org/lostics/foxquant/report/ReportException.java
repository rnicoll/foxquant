// $Id: ReportException.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.report;

public class ReportException extends Exception {

        public ReportException(String s) {
                super(s);
        }

        public ReportException(String s, Throwable t) {
                super(s, t);
        }

}
