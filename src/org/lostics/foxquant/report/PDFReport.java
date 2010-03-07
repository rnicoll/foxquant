// $Id: PDFReport.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

import com.itextpdf.text.FontFactory;
import com.itextpdf.text.pdf.DefaultFontMapper;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;

import org.jfree.chart.JFreeChart;

public abstract class PDFReport implements Report {

    abstract List<AbstractPage> getPages();

    public void writeReport(final Connection dbConnection, final File file, 
        final List<Roundturn> roundturns) throws IOException, SQLException, ReportException {
        writePDFReport(dbConnection, file, roundturns, getPages());
    }

   public static void writePDFReport(final Connection dbConnection, final File file, 
        final List<Roundturn> roundturns, final List<? extends AbstractPage> pages) throws IOException, SQLException, ReportException {

        final FileOutputStream outputStream = new FileOutputStream(file);
        try { 
                final Document document = new Document();
                final PdfWriter writer = PdfWriter.getInstance(document, outputStream);
                final DefaultFontMapper mapper = new DefaultFontMapper();
                FontFactory.registerDirectories();

                document.open();
                try {
                        for(AbstractPage page : pages) {
                            page.writePage(dbConnection, document, writer, roundturns, mapper);
                        }
                } finally { 
                    document.close();
                }
        } catch (DocumentException ex) {
            throw new ReportException("Error writing document", ex);
        } finally {
            outputStream.close();
        }
    }

}
