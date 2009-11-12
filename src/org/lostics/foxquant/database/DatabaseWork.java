// $Id: DatabaseWork.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.database;

import java.sql.SQLException;

interface DatabaseWork {
    public void dispose(final DatabaseThread databaseThread);

    public void write(final DatabaseThread databaseThread)
        throws DatabaseUnavailableException, SQLException;
}
