// $Id: UnitTestContractManager.java 691 2009-11-09 19:44:32Z  $
package org.lostics.foxquant.backtest;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;

import com.ib.client.ContractDetails;
import com.ib.client.Contract;
import com.ib.client.Order;

import org.lostics.foxquant.ib.OrderType;
import org.lostics.foxquant.model.AbstractContractManager;
import org.lostics.foxquant.model.ContractManager;
import org.lostics.foxquant.model.ContractPosition;
import org.lostics.foxquant.model.EntryOrder;
import org.lostics.foxquant.model.ExitOrders;
import org.lostics.foxquant.model.PeriodicData;
import org.lostics.foxquant.model.StrategyAlreadyExistsException;
import org.lostics.foxquant.model.StrategyException;
import org.lostics.foxquant.model.StrategyFactory;
import org.lostics.foxquant.model.Strategy;
import org.lostics.foxquant.model.TickData;

/**
 * Data source for testing. Takes in a list of periodic data, and feeds it back
 * out when run.
 */
public class UnitTestContractManager extends AbstractContractManager {    
    private final Strategy strategy;

    /** The minimum time interval between periodic data entries, in seconds. */
    private final long periodMillis;
    
    /** The quantity of the asset being traded, that we "have" at this point. */
    private int position = 0;

    /** The periodic data to be produced when run() is called. */
    private final List<PeriodicData> rows = new ArrayList<PeriodicData>();

    /**
     * Constructs a new list data source.
     *
     * @param setData the periodic data to be produced when run() is called.
     * Copies the given data into the data source's internal buffer, rather
     * keeping the original list.
     * @param strategyFactory the strategy factory to generate a strategy from.
     * If null, no strategy is set up (useful if this contract manager isn't going
     * to be used for trading).
     * @param setPeriodMillis the minimum time interval between entries in
     * setData.
     */
    public          UnitTestContractManager(final ContractDetails setContractDetails,
        final StrategyFactory strategyFactory, final List<PeriodicData> setData,
        final long setPeriodMillis)
        throws StrategyAlreadyExistsException {
        super(setContractDetails);
        
        this.periodMillis = setPeriodMillis;
        this.rows.addAll(setData);
        if (null != strategyFactory) {
            this.strategy = strategyFactory.getStrategy(null, this);
        } else {
            this.strategy = null;
        }
    }

    /**
     * Constructs a new list data source.
     *
     * @param setData the periodic data to be produced when run() is called.
     * Copies the given data into the data source's internal buffer, rather
     * keeping the original list.
     * @param strategyFactory the strategy factory to generate a strategy from.
     * If null, no strategy is set up (useful if this contract manager isn't going
     * to be used for trading).
     * @param setPeriodMillis the minimum time interval between entries in
     * setData.
     */
    public          UnitTestContractManager(final StrategyFactory strategyFactory, final List<PeriodicData> setData,
        final long setPeriodMillis)
        throws StrategyAlreadyExistsException {
        this(generateTestContractDetails(), strategyFactory, setData, setPeriodMillis);
    }

    /**
     * Constructs a new contract manager for unit testing. This constructor
     * does not set a strategy, and is therefore unsuitable for trading with.
     */
    public          UnitTestContractManager(final ContractDetails setContractDetails)
        throws StrategyAlreadyExistsException {
        this(setContractDetails, null, new ArrayList<PeriodicData>(), 1000);
    }

    /**
     * Constructs a new contract manager for unit testing. This constructor
     * does not set a strategy, and is therefore unsuitable for trading with.
     */
    public          UnitTestContractManager()
        throws StrategyAlreadyExistsException {
        this(generateTestContractDetails());
    }

    public void close() {
    }
    
    public static ContractDetails generateTestContractDetails() {
        final ContractDetails contractDetails = new ContractDetails();
        final Contract contract = new Contract();
        
        contractDetails.m_minTick = 0.0005;
        contractDetails.m_summary = contract;
        contract.m_secType = "CASH";
        contract.m_symbol = "GBP";
        contract.m_currency = "USD";
        contract.m_localSymbol = "GBP.USD";
        
        return contractDetails;
    }

    public long getBarPeriod() {
        return this.periodMillis;
    }
    
    public EntryOrder getOrdersFromFlat()
        throws StrategyException {
        return this.strategy.getOrdersFromFlat();
    }
    
    public Strategy getStrategy() {
        return this.strategy;
    }

    public boolean isContractForexNZD() {
        // XXX: We probably want to make this an option to the constructor.
        return false;
    }

    public boolean isFlat() {
        return this.position == 0;
    }

    public boolean isLong() {
        return this.position > 0;
    }

    public boolean isShort() {
        return this.position < 0;
    }

    public void run() {
        PeriodicData lastRow = null;
    
        // We just run through the data without stopping, for this unusual case
        for (PeriodicData row: this.rows) {
            lastRow = row;
            try {
                this.strategy.backfillMinuteBar(row);
            } catch(StrategyException e) {
                // XXX: Need to report these upwards
                System.err.println("Strategy through exception during backtesting: "
                    + e);
            }
        }
        
        if (null != lastRow) {
            final TickData tickData = new TickData(System.currentTimeMillis(),
                lastRow.low, lastRow.high,
                1, 1);
            final TickData[] tickDataArray = new TickData[1];
            
            tickDataArray[0] = tickData;
            try {
                this.strategy.handleTick(tickDataArray);
            } catch(StrategyException e) {
                // XXX: Need to report these upwards
                System.err.println("Strategy through exception during backtesting: "
                    + e);
            }
        }
    }

    public int size() {
        return this.rows.size();
    }
}
