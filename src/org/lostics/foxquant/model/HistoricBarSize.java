// $Id: HistoricBarSize.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.model;

public enum HistoricBarSize {
    ONE_SECOND("1 sec", 1000, 110 * 60 * 1000, Integer.toString(110 * 60)
        + " S"),
    FIVE_SECONDS("5 secs", 5000, 110 * 60 * 1000, Integer.toString(110 * 60)
        + " S"),
    FIFTEEN_SECONDS("15 secs", 15000, 24 * 60 * 60 * 1000, "1 D"),
    THIRTY_SECONDS("30 secs", 30000, 24 * 60 * 60 * 1000, "1 D"),
    ONE_MINUTE("1 min", 60 * 1000, 5 * 24 * 60 * 60 * 1000, "5 D"),
    TWO_MINUTES("2 mins", 2 * 60 * 1000, 7 * 24 * 60 * 60 * 1000, "1 W"),
    FIVE_MINUTES("5 mins", 5 * 60 * 1000, 7 * 24 * 60 * 60 * 1000, "1 W"),
    FIFTEEN_MINUTES("15 mins", 15 * 60 * 1000, 7 * 24 * 60 * 60 * 1000, "1 W"),
    THIRTY_MINUTES("30 mins", 30 * 60 * 1000, 7 * 24 * 60 * 60 * 1000, "1 W"),
    ONE_HOUR("1 hour", 60 * 60 * 1000, 7 * 24 * 60 * 60 * 1000, "1 W"),
    ONE_DAY("1 day", 24 * 60 * 60 * 1000, 7 * 24 * 60 * 60 * 1000, "1 W"),
    ONE_WEEK("1 week", 7 * 24 * 60 * 60 * 1000, 52 * 7 * 24 * 60 * 60 * 1000, "52 W");

    private String barID;
    private int barLengthMillis;

    private int requestLengthMillis;
    private String requestLengthSetting;

          HistoricBarSize(final String setBarID, final int setBarLengthMillis, final int setRequestLengthMillis,
            final String setRequestLengthSetting) {
        this.barID = setBarID;
        this.barLengthMillis = setBarLengthMillis;

        this.requestLengthMillis = setRequestLengthMillis;
        this.requestLengthSetting = setRequestLengthSetting;
    }

    public String getBarID() {
        return this.barID;
    }

    public int getBarLengthMillis() {
        return this.barLengthMillis;
    }

    public int getBarLengthSeconds() {
        return this.barLengthMillis / 1000;
    }

    public int getRequestLengthMillis() {
        return this.requestLengthMillis;
    }

    public String getRequestLengthSetting() {
        return this.requestLengthSetting;
    }
}
