// $Id: FoxQuant.java 702 2009-11-11 00:10:10Z jrn $
package org.lostics.foxquant;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.swing.UIManager;

import org.apache.log4j.Logger;

import org.jvnet.substance.skin.SubstanceRavenGraphiteLookAndFeel;

import org.lostics.foxquant.database.DatabaseUnavailableException;
import org.lostics.foxquant.iqfeed.IQFeedException;
import org.lostics.foxquant.ui.MainFrame;

public class FoxQuant extends Object {
    /** Default configuration file name, relative to the user's home
     * directory.
     */
    public static final String DEFAULT_CONFIG_FILENAME = ".foxquant.cfg";
    
    public static final String VERSION = "0.1";

    private static final Logger log = Logger.getLogger(FoxQuant.class);

    public static Configuration getConfiguration()
        throws Configuration.ConfigurationException, IOException {
        final Configuration configuration = new Configuration();

        try {
            File configFile = new File(System.getProperty("user.home"));

            configFile = new File(configFile, DEFAULT_CONFIG_FILENAME);
            configuration.load(configFile);
        } catch(Configuration.NoConfigurationException e) {
            System.err.println(e.getMessage());
            return null;
        }

        return configuration;
    }

    public static void main(final String[] argv)
        throws Configuration.ConfigurationException,
            InvocationTargetException, IOException, SQLException {
        final Configuration configuration = getConfiguration();

        if (null == configuration) {
            return;
        }

        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        UIManager.setLookAndFeel(new SubstanceRavenGraphiteLookAndFeel());
                    } catch(Exception e) {
                        log.error("Substance RavenGraphite L&F failed to initialise.", e);
                    }
                
                    try {
                        MainFrame.createAndShowUI(configuration);
                    } catch(DatabaseUnavailableException e) {
                        System.err.println("Unable to connect to database: " + e);
                    } catch(IQFeedException e) {
                        System.err.println("Unable to connect to IQFeed: " + e);
                    } catch(SQLException e) {
                        System.err.println("Caught SQLException: " + e);
                    }
                }
            });
        } catch(InterruptedException e) {
            // Odd, but ignorable
        }

        return;
    }
}
