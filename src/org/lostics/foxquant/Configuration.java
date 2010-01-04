// $Id: Configuration.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.ib.client.Contract;

import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.iqfeed.IQFeedException;
import org.lostics.foxquant.database.DatabaseUnavailableException;

public class Configuration extends Object {
    public static final String DEFAULT_IQCONNECT = "IQConnect.exe";
    public static final int    DEFAULT_PORT = 7496;

    public static final String PROPERTY_IQFEED_IQCONNECT = "iqConnect";
    
    public static final String PROPERTY_JDBC_DRIVER = "jdbcDriver";
    public static final String PROPERTY_JDBC_PASSWORD = "jdbcPassword";
    public static final String PROPERTY_JDBC_URL = "jdbcURL";
    public static final String PROPERTY_JDBC_USER = "jdbcUser";

    public static final String PROPERTY_TXTLOCAL_FROM = "txtlocalFrom";
    public static final String PROPERTY_TXTLOCAL_NUMBER = "txtlocalNumber";
    public static final String PROPERTY_TXTLOCAL_PASSWORD = "txtlocalPassword";
    public static final String PROPERTY_TXTLOCAL_USER = "txtlocalUser";

    public static final String PROPERTY_TWS_HOST = "twsHost";
    public static final String PROPERTY_TWS_PORT = "twsPort";
    public static final String PROPERTY_TWS_CLIENT_ID = "twsClientID";
    
    public static final String PROPERTY_TWITTER_PASSWORD = "twitterPassword";
    public static final String PROPERTY_TWITTER_USER = "twitterUser";

    public static final String[] REQUIRED_PROPERTIES    = {
        PROPERTY_JDBC_PASSWORD,
        PROPERTY_TWS_CLIENT_ID
    };

    private static final Logger log = Logger.getLogger(Configuration.class);

    private boolean firstDBConnection = true;

    public  String iqConnect = null;
    
    public  String jdbcDriver = "org.hsqldb.jdbcDriver";
    public  String jdbcPassword;
    public  String jdbcURL = "jdbc:hsqldb:hsql://localhost/forex";
    public  String jdbcUser = "forex";

    public  String txtlocalFrom = null;
    public  SMSGateway.PhoneNumber txtlocalNumber = null;
    public  String txtlocalPassword = null;
    public  String txtlocalUser = null;

    public  int    twsClientID;
    public  String twsHost = "localhost";
    public  int    twsPort = DEFAULT_PORT;
    
    public  String twitterPassword = null;
    public  String twitterUser = null;

    public         Configuration() {
    }

    /**
     * Constructs and CONNECTS a connection manager.
     */
    public  ConnectionManager getConnectionManager()
        throws DatabaseUnavailableException, IQFeedException, SQLException {
        final ConnectionManager connectionManager = new ConnectionManager(this);

        return connectionManager;
    }

    public  Connection getDBConnection()
        throws DatabaseUnavailableException {
        final Connection connection;

        try {
            connection = DriverManager.getConnection(this.jdbcURL, this.jdbcUser, this.jdbcPassword);
        } catch(SQLException e) {
            throw new DatabaseUnavailableException(e);
        }

        return connection;
    }
    
    /**
     * Returns the name of the IQConnect.exe program for starting IQFeed.
     * Defaults, unsurprisingly, to "IQConnect.exe". Alternatives might be 
     * the full path if required, or something more complex (for example to run
     * IQConnect via WINE).
     */
    public String getIQConnectName() {
        return null == this.iqConnect
            ? DEFAULT_IQCONNECT
            : this.iqConnect;
    }

    /**
     * Returns an SMS gateway, based on this configuration, if the required configuration
     * fields have been supplied.
     *
     * @return an SMS gateway if enough configuration has been provided, or null otherwise.
     */
    public  SMSGateway getSMSGateway() {
        if (null != this.txtlocalUser &&
            null != this.txtlocalPassword &&
            null != this.txtlocalNumber) {
            return new SMSGateway(this.txtlocalFrom, this.txtlocalUser, this.txtlocalPassword);
        }

        return null;
    }

    /**
     * Returns an Twitter gateway, based on this configuration, if the required configuration
     * fields have been supplied.
     *
     * @return an Twitter gateway if enough configuration has been provided, or null otherwise.
     */
    public  TwitterGateway getTwitterGateway() {
        if (null != this.twitterUser &&
            null != this.twitterPassword) {
            return new TwitterGateway(this.twitterUser, this.twitterPassword);
        }

        return null;
    }

    /**
     * Loads and sanity checks the configuration file.
     *
     * @param file the file to load the configuration from.
     *
     * @throws ConfigurationException if there was a problem with the contents
     * of the configuration file.
     * @throws IOException if there was a problem loading the configuration file
     * from disk.
     */
    public void load(final File file)
        throws ConfigurationException, IOException {
        final Class clazz = this.getClass();
        InputStream reader;
        final Enumeration<?> propertyNames;
        final Properties properties = new Properties();

        try {
            reader = new FileInputStream(file);
        } catch(IOException e) {
            throw new NoConfigurationException("Could not open configuration file \""
                + file.toString() + "\" for reading.");
        }
        
        reader = new BufferedInputStream(reader);
        try {
            properties.load(reader);
        } finally {
            reader.close();
        }

        for (String propertyKey: REQUIRED_PROPERTIES) {
            if (!properties.keySet().contains(propertyKey)) {
                throw new MissingRequiredPropertyException(propertyKey,
                    "Missing required property \""
                        + propertyKey + "\" from configuration file \""
                        + file.getName() + "\".");
            }
        }

        propertyNames = properties.propertyNames();
        while (propertyNames.hasMoreElements()) {
            final String propertyKey = (String)propertyNames.nextElement();
            final String propertyValue = properties.getProperty(propertyKey);

            if (propertyKey.equals(PROPERTY_IQFEED_IQCONNECT)) {
                this.iqConnect = propertyValue;
            } else if (propertyKey.equals(PROPERTY_JDBC_DRIVER)) {
                this.jdbcDriver = propertyValue;
            } else if (propertyKey.equals(PROPERTY_JDBC_PASSWORD)) {
                this.jdbcPassword = propertyValue;
            } else if (propertyKey.equals(PROPERTY_JDBC_URL)) {
                this.jdbcURL = propertyValue;
            } else if (propertyKey.equals(PROPERTY_JDBC_USER)) {
                this.jdbcUser = propertyValue;
            } else if (propertyKey.equals(PROPERTY_TXTLOCAL_FROM)) {
                this.txtlocalFrom = propertyValue;
                try {
                    SMSGateway.validateFrom(this.txtlocalFrom);
                } catch(IllegalArgumentException e) {
                    throw new IllegalPropertyException(PROPERTY_TXTLOCAL_FROM, propertyValue, e.getMessage());
                }
            } else if (propertyKey.equals(PROPERTY_TXTLOCAL_NUMBER)) {
                try {
                    this.txtlocalNumber = new SMSGateway.PhoneNumber(propertyValue);
                } catch(IllegalArgumentException e) {
                    throw new IllegalPropertyException(PROPERTY_TXTLOCAL_NUMBER, propertyValue, "Expected mobile phone number in configuration property \""
                        + PROPERTY_TXTLOCAL_NUMBER + "\".");
                }
            } else if (propertyKey.equals(PROPERTY_TXTLOCAL_PASSWORD)) {
                this.txtlocalPassword = propertyValue;
            } else if (propertyKey.equals(PROPERTY_TXTLOCAL_USER)) {
                this.txtlocalUser = propertyValue;
            } else if (propertyKey.equals(PROPERTY_TWS_HOST)) {
                this.twsHost = propertyValue;
            } else if (propertyKey.equals(PROPERTY_TWS_PORT)) {
                try {
                    this.twsPort = Integer.parseInt(propertyValue);
                } catch(NumberFormatException e) {
                    throw new IllegalPropertyException(PROPERTY_TWS_PORT, propertyValue, "Expected integer port in configuration property \""
                        + PROPERTY_TWS_PORT + "\".");
                }
            } else if (propertyKey.equals("twsClientID")) {
                try {
                    this.twsClientID = Integer.parseInt(propertyValue);
                } catch(NumberFormatException e) {
                    throw new IllegalPropertyException(PROPERTY_TWS_CLIENT_ID, propertyValue, "Expected integer client ID in configuration property \""
                        + PROPERTY_TWS_CLIENT_ID + "\".");
                }
            } else if (propertyKey.equals(PROPERTY_TWITTER_PASSWORD)) {
                this.twitterPassword = propertyValue;
            } else if (propertyKey.equals(PROPERTY_TWITTER_USER)) {
                this.twitterUser = propertyValue;
            } else {
                log.warn("Unrecognised configuration parameter \""
                    + propertyKey + "\".");
            }
        }

        if (null == this.txtlocalNumber ||
            null == this.txtlocalUser ||
            null == this.txtlocalPassword) {
            this.txtlocalNumber = null;
            this.txtlocalUser = null;
            this.txtlocalPassword = null;
        }
        
        // Ensure that if only one of the username or password is given,
        // both are stored here as null
        if (null == this.twitterPassword ||
            null == this.twitterUser) {
            this.twitterPassword = null;
            this.twitterUser = null;
        }

        try {
            Class.forName(this.jdbcDriver).newInstance();
        } catch(ClassNotFoundException e) {
            throw new IllegalPropertyException(PROPERTY_JDBC_DRIVER, this.jdbcDriver, "Expected class name in configuration property \""
                + PROPERTY_JDBC_DRIVER + "\", but could not find given class \""
                + this.jdbcDriver + "\".");
        } catch(IllegalAccessException e) {
            throw new IllegalPropertyException(PROPERTY_JDBC_DRIVER, this.jdbcDriver, "Can not access JDBC driver class \""
                + this.jdbcDriver + "\" given in configuration property \""
                + PROPERTY_JDBC_DRIVER + "\".", e);
        } catch(InstantiationException e) {
            throw new IllegalPropertyException(PROPERTY_JDBC_DRIVER, this.jdbcDriver, "Could not instantiate JDBC driver class \""
		+ this.jdbcDriver + "\" given in configuration property \""
                + PROPERTY_JDBC_DRIVER + "\".", e);
        }
            

        return;
    }

    public static class ConfigurationException extends Exception {
        private     ConfigurationException(final String message) {
            super(message);
        }

        private     ConfigurationException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    public static class IllegalPropertyException extends ConfigurationException {
        private final String argumentKey;
        private final String argumentValue;

        private     IllegalPropertyException(final String setPropertyKey,
            final String setPropertyValue, final String message) {
            super(message);

            this.argumentKey = setPropertyKey;
            this.argumentValue = setPropertyValue;
        }

        private     IllegalPropertyException(final String setPropertyKey,
            final String setPropertyValue, final String message, final Throwable cause) {
            super(message, cause);

            this.argumentKey = setPropertyKey;
            this.argumentValue = setPropertyValue;
        }

        public  String  getPropertyKey() {
            return this.argumentKey;
        }

        public  String  getPropertyValue() {
            return this.argumentValue;
        }
    }

    public static class NoConfigurationException extends ConfigurationException {
        private     NoConfigurationException(final String message) {
            super(message);
        }
    }

    public static class MissingRequiredPropertyException extends ConfigurationException {
        private final String argumentKey;

        private     MissingRequiredPropertyException(final String setPropertyKey,
           final String message) {
            super(message);

            this.argumentKey = setPropertyKey;
        }

        public  String  getPropertyKey() {
            return this.argumentKey;
        }
    }
}
