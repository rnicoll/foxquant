// $Id: AbstractContractManager.java 690 2009-11-09 10:41:12Z  $
package org.lostics.foxquant.model;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.TimeZone;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.Order;

import org.apache.log4j.Logger;

import org.lostics.foxquant.ib.OrderIDUnavailableException;
import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.database.DatabaseUnavailableException;

public abstract class AbstractContractManager extends Thread implements ContractManager {
    // XXX: These times are Interactive Brokers specific, and shouldn't be in here
    private static final int MARKET_CLOSE_HOUR = 17;
    private static final int MARKET_CLOSE_HOUR_NZD = 15;
    private static final int MARKET_CLOSE_MINUTE = 0;

    private static final int MARKET_OPEN_HOUR = MARKET_CLOSE_HOUR;
    private static final int MARKET_OPEN_MINUTE = 15;

    // Need to handle that close and open timezones are not the same, and we're in a third
    // timezone for added chaos.
    private static final TimeZone MARKET_CLOSE_TIMEZONE = TimeZone.getTimeZone("America/New_York");
    private static final TimeZone MARKET_OPEN_TIMEZONE = TimeZone.getTimeZone("America/New_York");

    protected final Contract contract;
    protected final ContractKey contractKey;
    protected final ContractDetails contractDetails;

    protected final Logger log;

    private final Set<ContractPositionListener> positionListeners
        = new CopyOnWriteArraySet<ContractPositionListener>();
    
    /**
     * Formatter for use where prices need to be displayed.
     */
    private final DecimalFormat priceFormat;
    
    protected           AbstractContractManager(final ContractDetails setContractDetails) {
        this.contractDetails = setContractDetails;
        this.contract = setContractDetails.m_summary;
        this.contractKey = new ContractKey(this.contract);
        
        this.log = Logger.getLogger(this.getClass());
        
        final String formattedPrice = Double.toString(this.contractDetails.m_minTick);
        final StringBuilder formatString = new StringBuilder();
        
        if (formattedPrice.indexOf("E") == -1) {
            for (int charIdx = 0; charIdx < formattedPrice.length(); charIdx++) {
                char currentChar = formattedPrice.charAt(charIdx);
                
                if (Character.isDigit(currentChar)) {
                    formatString.append("0");
                } else if (currentChar == '.') {
                    formatString.append(currentChar);
                } else {
                    log.warn("Unexpected character '"
                        + currentChar + "' encountered while processing price string \""
                        + formattedPrice + "\" to generate price format pattern.");
                    // Assume it's unimportant and continue
                }
            }
        } else {
            final String[] parts = formattedPrice.split("E");
            final int e = Integer.valueOf(parts[1]);
            
            // XXX: This only works if there's a single significant digit past
            // the exponent.
            formatString.append("0.");
            for (int digitIdx = 0; digitIdx > e; digitIdx--) {
                formatString.append("0");
            }
        }
        this.priceFormat = new DecimalFormat(formatString.toString());
    }
    
    public final boolean equals(final Object o) {
        final ContractManager contractManagerB = (ContractManager)o;
        
        // XXX: It would be better if contract managers could produce contract
        // keys rather than using the full contract
        
        return this.getContractKey().equals(contractManagerB.getContractKey());
    }
    
    public final int hashCode() {
        return this.getContractKey().hashCode();
    }
    
    public final String toString() {
        return "ContractManager["
            + this.getContract() + "]";
    }

    public void addPositionListener(final ContractPositionListener listener) {
        this.positionListeners.add(listener);
    }

    protected void firePositionChangedNotification(final ContractPosition targetPositionType,
        final double targetPosition, final double actualPosition) {
        for (ContractPositionListener listener: this.positionListeners) {
            listener.positionChanged(targetPositionType, targetPosition, actualPosition);
        }
    }
    
    public String formatTicksAsPrice(final int ticks) {
        final String price;
        synchronized (this.priceFormat) {
            price = this.priceFormat.format(ticks * this.getMinimumTick());
        }
        return price;
    }

    public Contract getContract() {
        return this.contract;
    }

    public ContractDetails getContractDetails() {
        return this.contractDetails;
    }

    public ContractKey getContractKey() {
        return this.contractKey;
    }

    public int getContractID() {
        return this.getContract().m_conId;
    }

    public Date getMarketCloseTime(final Date now)
        throws IllegalStateException {
        final Calendar calendar = Calendar.getInstance();

        calendar.setTime(now);
        calendar.setTimeZone(MARKET_CLOSE_TIMEZONE);
        if (this.isContractForexNZD()) {
            calendar.set(Calendar.HOUR_OF_DAY, MARKET_CLOSE_HOUR_NZD);
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, MARKET_CLOSE_HOUR);
        }
        calendar.set(Calendar.MINUTE, MARKET_CLOSE_MINUTE);
        if (calendar.getTime().before(now)) {
            calendar.add(Calendar.DATE, 1);
        }

        return calendar.getTime();
    }

    public long getMarketCloseDuration(final Date now) {
        final Calendar calendar = Calendar.getInstance();

        calendar.setTime(now);
        calendar.setTimeZone(MARKET_CLOSE_TIMEZONE);
        if (this.isContractForexNZD()) {
            calendar.set(Calendar.HOUR_OF_DAY, MARKET_CLOSE_HOUR_NZD);
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, MARKET_CLOSE_HOUR);
        }
        calendar.set(Calendar.MINUTE, MARKET_CLOSE_MINUTE);
        if (calendar.getTime().after(now)) {
            calendar.add(Calendar.DATE, -1);
        }
        while (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
            calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            calendar.add(Calendar.DATE, -1);
        }

        return now.getTime() - calendar.getTime().getTime();
    }

    /**
     * Retrieves how long since the market last opened. Does not take into
     * account whether there has been a market close since the last open.
     */
    public long getMarketOpenDuration(final Date now) {
        final Calendar calendar = Calendar.getInstance();

        calendar.setTime(now);
        calendar.setTimeZone(MARKET_OPEN_TIMEZONE);
        calendar.set(Calendar.HOUR_OF_DAY, MARKET_OPEN_HOUR);
        calendar.set(Calendar.MINUTE, MARKET_OPEN_MINUTE);
        if (calendar.getTime().after(now)) {
            calendar.add(Calendar.DATE, -1);
        }

        return now.getTime() - calendar.getTime().getTime();
    }
    
    public double getMinimumTick() {
        return this.getContractDetails().m_minTick;
    }

    public boolean isMarketOpen(final Date now) {
        final Calendar calendar = Calendar.getInstance();
        int dayOfWeek;
        int hourOfDay;
        final int minute;
        final int marketCloseHour = this.isContractForexNZD()
            ? MARKET_CLOSE_HOUR_NZD
            : MARKET_CLOSE_HOUR;

        calendar.setTime(now);
        calendar.setTimeZone(MARKET_OPEN_TIMEZONE);

        dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        minute = calendar.get(Calendar.MINUTE);

        if (dayOfWeek == Calendar.SATURDAY) {
            return false;
        }

        if (dayOfWeek == Calendar.SUNDAY) {
            if (hourOfDay == MARKET_OPEN_HOUR) {
                return minute >= MARKET_OPEN_MINUTE;
            }

            return hourOfDay > MARKET_OPEN_HOUR;
        }

        // That's the Saturday/Sunday before open handled, now close time is our next risk.
        calendar.setTimeZone(MARKET_CLOSE_TIMEZONE);
        dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        if (dayOfWeek == Calendar.FRIDAY) {
            if (hourOfDay == marketCloseHour) {
                return minute < MARKET_CLOSE_MINUTE;
            }

            return hourOfDay < marketCloseHour;
        }

        if (hourOfDay != marketCloseHour) {
            return true;
        }

        return minute < MARKET_CLOSE_MINUTE ||
            minute >= MARKET_OPEN_MINUTE;
    }

    public void removePositionListener(final ContractPositionListener listener) {
        this.positionListeners.remove(listener);
    }
}
