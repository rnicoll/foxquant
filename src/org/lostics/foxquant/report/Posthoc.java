// $Id: Posthoc.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.report;


import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.model.OrderStatus;
import org.lostics.foxquant.Configuration;
import org.lostics.foxquant.FoxQuant;


public class Posthoc {

	private final ResultSet rs;

	public Posthoc(final ResultSet rs) {
		this.rs = rs;
	}



    public static void main(String[] args) throws Exception {
        final Configuration configuration = FoxQuant.getConfiguration();

        if (null == configuration) {
            return;
        }

        final Connection dbConnection = configuration.getDBConnection();
	final Posthoc posthoc = get(dbConnection);

	posthoc.dump(System.out);

	dbConnection.close();
    }


    public static Posthoc get(final Connection dbConnection) 
		throws SQLException {

	final PreparedStatement statement = dbConnection.prepareStatement(
	    "SELECT C.LOCAL_SYMBOL SYMBOL, "
            + " SA.RECEIVED_AT ENTRY_TIME, TA.ORDER_ID, " // SA.AVG_FILL_PRICE ENTRY_PRICE, "
           // + "SB.RECEIVED_AT EXIT_TIME, SB.AVG_FILL_PRICE EXIT_PRICE, "
		+ "(SB.AVG_FILL_PRICE - SA.AVG_FILL_PRICE) * 10000 PROFIT, "
		//+ "BGU.FAST GU_FAST, BGU.SLOW GU_SLOW, "
                + "BGU.SIGNAL * 10000 GU_SIGNAL, BGU.MOMENTUM * 10000 GU_MOMENTUM, "
		//+ "BUC.FAST UC_FAST, BUC.SLOW UC_SLOW, "
                + "BUC.SIGNAL * 10000 UC_SIGNAL, BUC.MOMENTUM * 10000 UC_MOMENTUM "
            + "FROM CONTRACT C "
            + "JOIN TRADE_ORDER TA ON TA.CONTRACT_ID=C.CONTRACT_ID "
            + "JOIN TRADE_ORDER TB ON TB.CONTRACT_ID=C.CONTRACT_ID "
            + "JOIN CONTRACT_POSITION CP ON CP.ENTRY_ORDER=TA.ORDER_ID AND CP.EXIT_ORDER=TB.ORDER_ID "
            + "JOIN ORDER_STATUS SA ON SA.ORDER_ID=TA.ORDER_ID "
            + "JOIN ORDER_STATUS SB ON SB.ORDER_ID=TB.ORDER_ID "
		+ "LEFT JOIN WILL_IT_BLEND BGU ON TA.CREATED_AT=BGU.BAR_START "
			+ "JOIN CONTRACT CGU ON BGU.CONTRACT_ID=CGU.CONTRACT_ID "
		+ "LEFT JOIN WILL_IT_BLEND BUC ON TA.CREATED_AT=BUC.BAR_START "
			+ "JOIN CONTRACT CUC ON BUC.CONTRACT_ID=CUC.CONTRACT_ID "
            + "WHERE CP.EXIT_ORDER IS NOT NULL AND SA.STATUS=? AND SB.STATUS=? "
		+ "AND CGU.LOCAL_SYMBOL = 'GBP.USD' "
		+ "AND CUC.LOCAL_SYMBOL = 'USD.CAD' "
            + "ORDER BY SA.RECEIVED_AT");


            statement.setString(1, OrderStatus.Filled.toString());
            statement.setString(2, OrderStatus.Filled.toString());

            final ResultSet rs = statement.executeQuery();

	return new Posthoc(rs);
	}


	public void dump(final PrintStream out) throws Exception {
		final ResultSetMetaData rsmd = rs.getMetaData();

		out.print("|");
		for(int i = 1; i <= rsmd.getColumnCount(); i++) {
			out.print(" " + rsmd.getColumnLabel(i) + "\t|");
		}
		out.println("");

		while(rs.next()) {
			out.print("|");
			for(int i = 1; i <= rsmd.getColumnCount(); i++) {
				if(rsmd.getColumnType(i) == Types.DOUBLE) {
					out.printf(" % -2.3f\t|", rs.getDouble(i));
				} else {
					out.print(" " + rs.getString(i) + "\t|");
				}
			}
			out.println("");
		}

	}


}
