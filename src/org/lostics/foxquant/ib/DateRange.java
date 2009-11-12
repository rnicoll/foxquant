// $Id: DateRange.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.ib;

import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;

public class DateRange extends Object implements Comparable<DateRange> {
    private Date startDate;
    private Date endDate;

    public  DateRange(final Date setStartDate, final Date setEndDate) {
        this.startDate = setStartDate;
        this.endDate = setEndDate;
    }

    public int compareTo(final DateRange rangeB) {
        if (this.startDate.equals(rangeB.startDate)) {
            return this.endDate.compareTo(rangeB.endDate);
        }
        return this.startDate.compareTo(rangeB.startDate);
    }

    public boolean equals(final Object o) {
        final DateRange rangeB = (DateRange)o;

        return compareTo(rangeB) == 0;
    }

    public int hashCode() {
        int hash = 1;

        hash = 31 * hash + this.startDate.hashCode();
        hash = 31 * hash + this.endDate.hashCode();

        return hash;
    }

    public Date getStartDate() {
        return this.startDate;
    }

    public Date getEndDate() {
        return this.endDate;
    }

    public String toString() {
        return this.startDate.toString() + ":"
            + this.endDate.toString();
    }

    public static SortedSet<DateRange> splitRange(final Date start, final Date end, final long periodMillis) {
        final long totalPeriod = end.getTime() - start.getTime();
        final SortedSet<DateRange> dateRanges = new TreeSet<DateRange>();

        for (long currentPeriodStart = 0; currentPeriodStart < totalPeriod; currentPeriodStart += periodMillis) {
            final Date currentFrom = new Date(start.getTime() + currentPeriodStart);
            final Date currentTo = new Date(currentFrom.getTime() + periodMillis);

            if (currentTo.after(end)) {
                dateRanges.add(new DateRange(currentFrom, end));
            } else {
                dateRanges.add(new DateRange(currentFrom, currentTo));
            }
        }

        return dateRanges;
    }
}
