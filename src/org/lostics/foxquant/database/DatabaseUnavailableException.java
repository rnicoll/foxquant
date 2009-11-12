// $Id: DatabaseUnavailableException.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.database;

import java.sql.SQLException;

/**
 * Represents a failure to connect to the SQL database.
 */
public class DatabaseUnavailableException extends Exception {
    public          DatabaseUnavailableException(final SQLException cause) {
        super(cause);
    }
}
