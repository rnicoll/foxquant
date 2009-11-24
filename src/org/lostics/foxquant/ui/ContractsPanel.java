// $Id: ContractsPanel.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.Container;
import java.awt.Dimension;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Spring;
import javax.swing.SpringLayout;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;

import org.lostics.foxquant.database.DatabaseUnavailableException;
import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.ib.ContractDetailsConsumer;
import org.lostics.foxquant.ib.ContractFactory;
import org.lostics.foxquant.model.ContractManagerConsumer;
import org.lostics.foxquant.model.ContractManager;
import org.lostics.foxquant.model.ErrorListener;
import org.lostics.foxquant.model.PositionNotFlatException;
import org.lostics.foxquant.model.StrategyAlreadyExistsException;
import org.lostics.foxquant.strategy.CatchingDaggers;
import org.lostics.foxquant.Configuration;

public class ContractsPanel extends JPanel {
    private final   Configuration        configuration;
    private final   MainFrame            mainFrame;

    private final   JTabbedPane tabbedPane = new JTabbedPane();
    
    private final   JTextField baseCurrencyField = new JTextField("GBP", 3);
    private final   JTextField purchaseCurrencyField = new JTextField("USD", 3);
    private final   JButton contractButton = new JButton("Get Contract");

    private         Map<String, ContractManager> contractManagers
        = new HashMap<String, ContractManager>();

    protected       ContractsPanel(final Configuration setConfiguration,
        final MainFrame setMainFrame) {
        super();

        final SpringLayout layout = new SpringLayout();

        this.mainFrame = setMainFrame;
        this.configuration = setConfiguration;
        
        this.setLayout(layout);
        
        this.tabbedPane.setPreferredSize(new Dimension(MainFrame.PANEL_WIDTH, 120));
        
        this.contractButton.addActionListener(this.new ContractAction());
        this.contractButton.setEnabled(false);

        this.setName("Contracts");

        this.add(this.baseCurrencyField);
        this.add(this.purchaseCurrencyField);
        this.add(this.contractButton);
        this.add(this.tabbedPane);
        
        layout.putConstraint(SpringLayout.WEST, this.baseCurrencyField, 5, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.NORTH, this.baseCurrencyField, 5, SpringLayout.NORTH, this);
        
        layout.putConstraint(SpringLayout.WEST, this.purchaseCurrencyField, 0, SpringLayout.EAST, this.baseCurrencyField);
        layout.putConstraint(SpringLayout.NORTH, this.purchaseCurrencyField, 5, SpringLayout.NORTH, this);
        
        layout.putConstraint(SpringLayout.WEST, this.contractButton, 3, SpringLayout.EAST, this.purchaseCurrencyField);
        layout.putConstraint(SpringLayout.NORTH, this.contractButton, 5, SpringLayout.NORTH, this);
        layout.putConstraint(SpringLayout.EAST, this, Spring.constant(10, 200, 10000), SpringLayout.EAST, this.contractButton);
        
        layout.putConstraint(SpringLayout.WEST, tabbedPane, 5, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.NORTH, tabbedPane, 5, SpringLayout.SOUTH, this.contractButton);
        layout.putConstraint(SpringLayout.EAST, this, 5, SpringLayout.EAST, tabbedPane);
        layout.putConstraint(SpringLayout.SOUTH, this, 5, SpringLayout.SOUTH, tabbedPane);
        
        this.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createBevelBorder(BevelBorder.LOWERED),
                "Contracts"
            )
        );
    }

    private void addContractPanel(final Contract contract, final javax.swing.JComponent contractPanel) {
        this.tabbedPane.addTab(contract.m_localSymbol, contractPanel);
    }
    
    /**
     * Enable the "Get Contracts" button, MUST only be called from within the
     * Swing event thread.
     */
    protected void enableGetContracts() {
        this.contractButton.setEnabled(true);
    }

    private class ContractAction extends Object implements ActionListener, ContractDetailsConsumer, ContractManagerConsumer {
        private     ContractFactory contractFactory;

        private     ContractAction() {
            this.contractFactory = new ContractFactory(ContractsPanel.this.mainFrame.getConnectionManager());
        }

        public  void    actionPerformed(final ActionEvent event) {
            final ContractManager contractManager;
            final String baseCurrency = ContractsPanel.this.baseCurrencyField.getText();
            final Connection dbConnection;
            final String purchaseCurrency = ContractsPanel.this.purchaseCurrencyField.getText();

            try {
                try {
                    dbConnection = ContractsPanel.this.configuration.getDBConnection();
                } catch(DatabaseUnavailableException e) {
                    // Modal dialog time?
                    System.err.println("Unable to contact SQL database: "
                        + e.getCause());
                    return;
                }

                this.contractFactory.getForexContractDetails(dbConnection,
                    baseCurrency, purchaseCurrency, this);
            } catch(SQLException sqlE) {
                // Convert the exception into something the wrapped runnable can access
                final Exception e = sqlE;
                
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        JOptionPane.showMessageDialog(ContractsPanel.this.mainFrame,
                            "Unable to retrieve contract for currency pair \""
                                + baseCurrency + "/"
                                + purchaseCurrency + " due to: "
                                + e.toString(),
                            "Database error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                });
                return;
            }
        }

        public void contractDetailsEnd() {
        }

        public void contractDetailsReady(final ContractDetails contractDetails) {
            final Contract contract = contractDetails.m_summary;
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    final ConnectionManager connectionManager = ContractsPanel.this.mainFrame.getConnectionManager();

                    try {
                        connectionManager.getContractManager(ContractAction.this, contractDetails, ContractsPanel.this.mainFrame.getStrategyFactory());
                    } catch(DatabaseUnavailableException e) {
                        System.err.println("Unable to construct contract manager for "
                            + contract.m_localSymbol + " due to database being unavailable: " + e);
                        return;
                    } catch(StrategyAlreadyExistsException e) {
                        javax.swing.SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                JOptionPane.showMessageDialog(ContractsPanel.this.mainFrame,
                                    "A strategy is already running for currency pair "
                                        + contract.m_localSymbol + ".",
                                    "Strategy error",
                                    JOptionPane.ERROR_MESSAGE);
                            }
                        });
                        return;
                    } catch(SQLException e) {
                        System.err.println("Unable to construct contract manager for "
                            + contract.m_localSymbol + " due to: " + e);
                        return;
                    }

                    return;
                }
            });
        }
        
        public void contractManagerReady(final ContractManager contractManager) {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    final org.lostics.foxquant.model.Strategy strategy = contractManager.getStrategy();
                
                    // contractManager.addPositionListener(this);
                    ContractsPanel.this.addContractPanel(contractManager.getContract(), strategy.getSwingComponent());
                }
            });
        }
    }
}
