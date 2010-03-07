package org.lostics.foxquant.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
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
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;

import com.ib.client.Contract;

import org.lostics.foxquant.ib.ContractFactory;
import org.lostics.foxquant.ib.ConnectionManager;
import org.lostics.foxquant.iqfeed.IQFeedException;
import org.lostics.foxquant.model.ContractManager;
import org.lostics.foxquant.model.ErrorListener;
import org.lostics.foxquant.model.StrategyFactory;
import org.lostics.foxquant.strategy.CatchingDaggersFactory;
import org.lostics.foxquant.Configuration;
import org.lostics.foxquant.database.DatabaseUnavailableException;

public class MainFrame extends JFrame implements WindowListener {
    /** Width of the error and contracts panel. */
    protected static final int PANEL_WIDTH = 800;

    private final   Configuration   configuration;
    private final   ConnectionManager connectionManager;
    private final   StrategyFactory strategyFactory = new CatchingDaggersFactory();

    private final  JButton   connectButton = new JButton("Connect");
    private final  JButton   disconnectButton = new JButton("Disconnect");
    private final  JButton   emergencyStopButton = new JButton("EMERGENCY STOP");
    private final  JTextArea errorTextArea = new JTextArea();
    
    private final   ContractsPanel contractsPanel;

    private         MainFrame(final Configuration setConfiguration)
        throws DatabaseUnavailableException, IQFeedException, SQLException {
        super("Forex Quant");

        this.configuration = setConfiguration;
        this.connectionManager = this.configuration.getConnectionManager();
        
        this.connectButton.addActionListener(this.new ConnectAction());
        this.disconnectButton.addActionListener(this.new DisconnectAction());
        this.disconnectButton.setEnabled(false);
        this.emergencyStopButton.addActionListener(this.new EmergencyStopAction());
        
        this.errorTextArea.setEditable(false);
        this.errorTextArea.setLineWrap(true);
        this.errorTextArea.setWrapStyleWord(true);
        
        this.contractsPanel = new ContractsPanel(setConfiguration, this);
    }

    private JMenuBar createMenuBar() {
        final JMenuBar menuBar = new JMenuBar();
        final JMenu fileMenu = new JMenu("File");
        
        fileMenu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(fileMenu);
        
        final JMenuItem connectMenuItem = new JMenuItem("Connect", KeyEvent.VK_C);
        connectMenuItem.getAccessibleContext().setAccessibleDescription(
            "Connect to Trader Workstation");
                
        final JMenuItem disconnectMenuItem = new JMenuItem("Disconnect", KeyEvent.VK_D);
        disconnectMenuItem.getAccessibleContext().setAccessibleDescription(
            "Disconnect from Trader Workstation");
        disconnectMenuItem.setEnabled(false);
        
        final JMenuItem quitMenuItem = new JMenuItem("Quit", KeyEvent.VK_Q);
        disconnectMenuItem.getAccessibleContext().setAccessibleDescription(
            "Quit the FoxQuant application");
                
        fileMenu.add(connectMenuItem);
        fileMenu.add(disconnectMenuItem);
        
        fileMenu.addSeparator();
        
        fileMenu.add(quitMenuItem);

        return menuBar;
    }

    public static void createAndShowUI(final Configuration setConfiguration)
        throws DatabaseUnavailableException, IQFeedException, SQLException {
        // XXX: Should handle runtime exceptions by exiting here.
        final MainFrame frame = new MainFrame(setConfiguration);
        frame.createAndShowUI();
    }

    private void createAndShowUI()
        throws DatabaseUnavailableException, SQLException {
        final Container pane;
        final JPanel controlPanel = this.createControlPanel();
        final JPanel errorPanel = this.createErrorPanel();
        final SpringLayout layout = new SpringLayout();

        //Create and set up the window.
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.addWindowListener(this);

        pane = this.getContentPane();
        pane.setLayout(layout);
        
        pane.add(controlPanel);
        pane.add(contractsPanel);
        pane.add(errorPanel);
        
        layout.putConstraint(SpringLayout.WEST, controlPanel, 5, SpringLayout.WEST, pane);
        layout.putConstraint(SpringLayout.NORTH, controlPanel, 5, SpringLayout.NORTH, pane);
        
        layout.putConstraint(SpringLayout.WEST, contractsPanel, 5, SpringLayout.WEST, pane);
        layout.putConstraint(SpringLayout.NORTH, contractsPanel, 5, SpringLayout.SOUTH, controlPanel);
        layout.putConstraint(SpringLayout.EAST, contractsPanel, 0, SpringLayout.EAST, errorPanel);
        
        layout.putConstraint(SpringLayout.WEST, errorPanel, 5, SpringLayout.WEST, pane);
        layout.putConstraint(SpringLayout.NORTH, errorPanel, 5, SpringLayout.SOUTH, contractsPanel);
        
        layout.putConstraint(SpringLayout.EAST, pane, 5, SpringLayout.EAST, errorPanel);
        layout.putConstraint(SpringLayout.SOUTH, pane, 5, SpringLayout.SOUTH, errorPanel);

        this.connectionManager.addErrorListener(this.new ErrorHandler());

        this.setJMenuBar(createMenuBar());

        //Display the window.
        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }
    
    /**
     * Generates the panel with the contract details in.
     */
    private JPanel createContractPanel() {
        final JPanel panel = new JPanel();
        final SpringLayout layout = new SpringLayout();
        
        return panel;
    }
    
    /**
     * Generates the panel with the control buttons (connect, disconnect, etc.)
     */
    private JPanel createControlPanel() {
        final JPanel panel = new JPanel();
        final SpringLayout layout = new SpringLayout();
        
        panel.setLayout(layout);
        panel.add(this.connectButton);
        panel.add(this.disconnectButton);
        panel.add(this.emergencyStopButton);
        
        layout.putConstraint(SpringLayout.WEST, this.connectButton, 3, SpringLayout.WEST, panel);
        layout.putConstraint(SpringLayout.NORTH, this.connectButton, 3, SpringLayout.NORTH, panel);
        layout.putConstraint(SpringLayout.SOUTH, panel, 3, SpringLayout.SOUTH, this.connectButton);
        
        layout.putConstraint(SpringLayout.WEST, this.disconnectButton, 3, SpringLayout.EAST, this.connectButton);
        layout.putConstraint(SpringLayout.NORTH, this.disconnectButton, 3, SpringLayout.NORTH, panel);
        layout.putConstraint(SpringLayout.SOUTH, panel, 3, SpringLayout.SOUTH, this.disconnectButton);
        
        layout.putConstraint(SpringLayout.WEST, this.emergencyStopButton, 3, SpringLayout.EAST, this.disconnectButton);
        layout.putConstraint(SpringLayout.NORTH, this.emergencyStopButton, 3, SpringLayout.NORTH, panel);
        layout.putConstraint(SpringLayout.SOUTH, panel, 3, SpringLayout.SOUTH, this.emergencyStopButton);
        layout.putConstraint(SpringLayout.EAST, panel, 3, SpringLayout.EAST, this.emergencyStopButton);

        panel.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createBevelBorder(BevelBorder.LOWERED),
                "Connection"
            )
        );
        
        return panel;
    }
    
    private JPanel createErrorPanel() {
        final JScrollPane scrollPanel = new JScrollPane(this.errorTextArea);
        final JPanel panel = new JPanel();
        final SpringLayout layout = new SpringLayout();
        
        scrollPanel.setPreferredSize(new Dimension(PANEL_WIDTH, 200));
        panel.setLayout(layout);
        panel.add(scrollPanel);
        layout.putConstraint(SpringLayout.WEST, scrollPanel, 3, SpringLayout.WEST, panel);
        layout.putConstraint(SpringLayout.NORTH, scrollPanel, 3, SpringLayout.NORTH, panel);
        layout.putConstraint(SpringLayout.EAST, panel, 3, SpringLayout.EAST, scrollPanel);
        layout.putConstraint(SpringLayout.SOUTH, panel, 3, SpringLayout.SOUTH, scrollPanel);
        panel.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createBevelBorder(BevelBorder.LOWERED),
                "Error Log"
            )
        );
        
        return panel;
    }

    protected Configuration getConfiguration() {
        return this.configuration;
    }

    /**
     * Returns the connection manager.
     *
     */
    protected ConnectionManager getConnectionManager() {
        return this.connectionManager;
    }

    protected StrategyFactory getStrategyFactory() {
        return this.strategyFactory;
    }

    public void windowActivated(final WindowEvent event) {
    }

    public void windowClosed(final WindowEvent event) {
        System.exit(0);
    }

    public void windowClosing(final WindowEvent event) {
        try {
            this.connectionManager.close();
        } catch(SQLException sqlException) {
            System.err.println(sqlException.toString());
        }
    }

    public void windowDeactivated(final WindowEvent event) {
    }

    public void windowDeiconified(final WindowEvent event) {
    }

    public void windowIconified(final WindowEvent event) {
    }

    public void windowOpened(final WindowEvent event) {
    }

    private class ConnectAction extends Object implements ActionListener {
        private     ConnectAction() {
        }

        public  void    actionPerformed(final ActionEvent e) {
            final Thread connectThread = new Thread(new Runnable() {
                public void run() {
                    final Connection dbConnection;

                    try {
                        dbConnection = MainFrame.this.configuration.getDBConnection();
                    } catch(DatabaseUnavailableException e) {
                        javax.swing.SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                JOptionPane.showMessageDialog(MainFrame.this,
                                    "Unable to connect to MySQL database, so unable to construct connection manager for TWS.",
                                    "Database error",
                                    JOptionPane.ERROR_MESSAGE);
                            }
                        });
                        return;
                    }

                    try {
                        try {
                            MainFrame.this.connectionManager.connect(dbConnection);
                        } finally {
                            dbConnection.close();
                        }
                    } catch(Exception e) {
                        final String message = e.toString();
                        
                        javax.swing.SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                JOptionPane.showMessageDialog(MainFrame.this,
                                    "ConnectionManager.connect() threw Exception: "
                                        + message,
                                    "Database error",
                                    JOptionPane.ERROR_MESSAGE);
                            }
                        });
                        e.printStackTrace();
                        return;
                    }
                    
                    javax.swing.SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            // I wish we had a way of checking the connection worked.
                            MainFrame.this.contractsPanel.enableGetContracts();
                            MainFrame.this.disconnectButton.setEnabled(true);
                        }
                    });
                }
            });

            connectThread.start();
        }
    }

    private class DisconnectAction extends Object implements ActionListener {
        private     DisconnectAction() {
        }

        public  void    actionPerformed(final ActionEvent e) {
            MainFrame.this.connectionManager.disconnect();
        }
    }

    private class EmergencyStopAction extends Object implements ActionListener {
        private     EmergencyStopAction() {
        }

        public  void    actionPerformed(final ActionEvent e) {
            MainFrame.this.connectionManager.emergencyStop();
        }
    }

    private class ErrorHandler extends Object implements ErrorListener {
        private     ErrorHandler() {
        }

        public void error(final int id, final int errorCode, final String errorString) {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    MainFrame.this.errorTextArea.append("Error #"
                        + id + ", "
                        + errorCode + ": "
                        + errorString + "\n");
                }
            });
        }

        public void error(final String errorString) {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    MainFrame.this.errorTextArea.append(errorString + "\n");
                }
            });
        }

        public void error(final Exception cause) {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    MainFrame.this.errorTextArea.append("Caught exception (see stderr for stacktrace): "
                        + cause.toString() + "\n");
                    cause.printStackTrace(System.err);
                }
            });
        }
    }
}
