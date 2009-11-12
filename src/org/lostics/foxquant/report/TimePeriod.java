// $Id: TimePeriod.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.report;

import java.util.Date;

public class TimePeriod extends Object implements Comparable<TimePeriod> {
    private final Date start;
    private final Date end;

    protected   TimePeriod(final Date setStart, final Date setEnd) {
        this.start = setStart;
        this.end = setEnd;
    }

    public int compareTo(final TimePeriod periodB) {
        if (this.start.equals(periodB.start)) {
            return this.end.compareTo(periodB.end);
        }

        return this.start.compareTo(periodB.start);
    }

    protected boolean contains(final Date date) {
        return this.start.before(date) &&
            this.end.after(date);
    }

    public Date getEnd() {
        return this.end;
    }

    public Date getStart() {
        return this.start;
    }
}
