package com.sst.burpextender.proxyhistory.webui.swingui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.eclipse.wb.swing.FocusTraversalOnArray;

import com.google.common.base.Throwables;
import com.jgoodies.common.base.Strings;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import com.sst.burpextender.proxyhistory.webui.AppContext;
import com.sst.burpextender.proxyhistory.webui.DataStore;
import com.sst.burpextender.proxyhistory.webui.GUIConfig;

public class DemoPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final AppContext appContext;
    private JSpinner spnWebUIPort;
    private JTextField txtNewDbName;
    private JComboBox<String> cmbSelectDb;
    private JTextArea txtaTargetHost;
    private JTextArea txtaExcludeFilenameExtension;
    private JButton btnStartWebUi;
    private JButton btnStopWebUi;

    /**
     * Create the panel.
     */
    public DemoPanel(AppContext appContext) {
        this.appContext = appContext;

        setLayout(new BorderLayout(0, 0));

        JPanel panelSettings = new JPanel();
        add(panelSettings, BorderLayout.NORTH);
        panelSettings.setLayout(
            new FormLayout(
                new ColumnSpec[] {
                    FormSpecs.RELATED_GAP_COLSPEC,
                    ColumnSpec.decode("100dlu"),
                    FormSpecs.RELATED_GAP_COLSPEC,
                    ColumnSpec.decode("100dlu"),
                    FormSpecs.RELATED_GAP_COLSPEC,
                    ColumnSpec.decode("100dlu"),
                    FormSpecs.RELATED_GAP_COLSPEC,
                    ColumnSpec.decode("default:grow"), },
                new RowSpec[] {
                    FormSpecs.RELATED_GAP_ROWSPEC,
                    FormSpecs.DEFAULT_ROWSPEC,
                    FormSpecs.RELATED_GAP_ROWSPEC,
                    FormSpecs.DEFAULT_ROWSPEC,
                    FormSpecs.RELATED_GAP_ROWSPEC,
                    RowSpec.decode("top:60dlu"),
                    FormSpecs.RELATED_GAP_ROWSPEC,
                    RowSpec.decode("top:60dlu"),
                    FormSpecs.RELATED_GAP_ROWSPEC,
                    FormSpecs.DEFAULT_ROWSPEC,
                    FormSpecs.RELATED_GAP_ROWSPEC,
                    FormSpecs.DEFAULT_ROWSPEC,
                    FormSpecs.RELATED_GAP_ROWSPEC,
                    FormSpecs.DEFAULT_ROWSPEC,
                    FormSpecs.RELATED_GAP_ROWSPEC,
                    FormSpecs.DEFAULT_ROWSPEC,
                    FormSpecs.RELATED_GAP_ROWSPEC,
                    FormSpecs.DEFAULT_ROWSPEC,
                    FormSpecs.RELATED_GAP_ROWSPEC,
                    RowSpec.decode("default:grow"), }));

        btnStartWebUi = new JButton("Start Web UI & Logging");
        btnStartWebUi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                saveSettings();

                List<Pattern> targetHostNameRegexps = new ArrayList<>();
                for (String s : txtaTargetHost.getText().split("\n")) {
                    s = s.trim();
                    if (Strings.isEmpty(s)) {
                        continue;
                    }
                    String p = GUIConfig.convertWildcardToRegexp(s);
                    if (!GUIConfig.isValidRegexpPattern(p)) {
                        continue;
                    }
                    Pattern regexp = Pattern.compile(p);
                    targetHostNameRegexps.add(regexp);
                }

                List<String> excludeFilenameExtensions = new ArrayList<>();
                for (String s : txtaExcludeFilenameExtension.getText().split("\n")) {
                    s = s.trim();
                    if (Strings.isEmpty(s)) {
                        continue;
                    }
                    excludeFilenameExtensions.add(s);
                }

                try {
                    final String dbname = cmbSelectDb.getSelectedItem().toString();
                    DemoPanel.this.appContext.startLogging(dbname, targetHostNameRegexps, excludeFilenameExtensions);

                    final int portNum = Integer.parseInt(spnWebUIPort.getValue().toString());
                    DemoPanel.this.appContext.getWebUIApp().start(portNum, cmbSelectDb.getSelectedItem().toString());
                    JOptionPane
                        .showMessageDialog(DemoPanel.this.getWindowFrame(), "Web UI & Logging Started Successfully.");

                    btnStartWebUi.setEnabled(false);
                    btnStopWebUi.setEnabled(true);

                    Desktop desktop = Desktop.getDesktop();
                    try {
                        desktop.browse(new URI("http://localhost:" + portNum + "/"));
                    } catch (IOException ex) {
                        DemoPanel.this.appContext.printErr(Throwables.getStackTraceAsString(ex));
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                        DemoPanel.this.getWindowFrame(),
                        ex.getMessage(),
                        "Web UI or Logging Start Failure.",
                        JOptionPane.WARNING_MESSAGE);
                    DemoPanel.this.appContext.printErr(Throwables.getStackTraceAsString(ex));
                }
            }
        });
        panelSettings.add(btnStartWebUi, "2, 2, 3, 1");

        btnStopWebUi = new JButton("Stop Web UI & Logging");
        btnStopWebUi.setEnabled(false);
        btnStopWebUi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    DemoPanel.this.appContext.stopLogging();
                    DemoPanel.this.appContext.getWebUIApp().stop();
                    JOptionPane
                        .showMessageDialog(DemoPanel.this.getWindowFrame(), "Web UI & Logging Stopped Successfully.");

                    btnStartWebUi.setEnabled(true);
                    btnStopWebUi.setEnabled(false);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                        DemoPanel.this.getWindowFrame(),
                        ex.getMessage(),
                        "Web UI or Logging Stop Failure.",
                        JOptionPane.WARNING_MESSAGE);
                    DemoPanel.this.appContext.printErr(Throwables.getStackTraceAsString(ex));
                }
            }
        });
        panelSettings.add(btnStopWebUi, "2, 4, 3, 1");

        JLabel lblTargetHost = new JLabel("target host (*:wildcard):");
        lblTargetHost.setHorizontalAlignment(SwingConstants.RIGHT);
        panelSettings.add(lblTargetHost, "2, 6");

        JScrollPane scrollPaneTargetHost = new JScrollPane();
        panelSettings.add(scrollPaneTargetHost, "4, 6, 3, 1, fill, fill");

        txtaTargetHost = new JTextArea();
        txtaTargetHost.setToolTipText(
            "explicitly set logging target hostname one per line. (* = wildcard, ex: *.localhost => abc.localhost, abc.def.localhost, ... will be logged.)");
        txtaTargetHost.setFont(new Font("MS UI Gothic", Font.PLAIN, 12));
        scrollPaneTargetHost.setViewportView(txtaTargetHost);

        JLabel lblExcludeFilenameExtension = new JLabel("exclude filename extension:");
        panelSettings.add(lblExcludeFilenameExtension, "2, 8, right, default");

        JScrollPane scrollPaneExcludeFilenameExtension = new JScrollPane();
        panelSettings.add(scrollPaneExcludeFilenameExtension, "4, 8, 3, 1, fill, fill");

        txtaExcludeFilenameExtension = new JTextArea();
        txtaExcludeFilenameExtension.setFont(new Font("MS UI Gothic", Font.PLAIN, 12));
        scrollPaneExcludeFilenameExtension.setViewportView(txtaExcludeFilenameExtension);

        JSeparator separator = new JSeparator();
        panelSettings.add(separator, "2, 10, 5, 1");

        JLabel lblAdvancedSetting = new JLabel("Advanced Setting:");
        panelSettings.add(lblAdvancedSetting, "2, 12");

        JLabel lblInternalWebUIPort = new JLabel("Web UI Internal Listen Port:");
        lblInternalWebUIPort.setHorizontalAlignment(SwingConstants.RIGHT);
        panelSettings.add(lblInternalWebUIPort, "2, 14");

        spnWebUIPort = new JSpinner();
        spnWebUIPort
            .setModel(new SpinnerNumberModel(new Integer(GUIConfig.WEBUI_PORT_DEFAULT), null, null, new Integer(1)));
        panelSettings.add(spnWebUIPort, "4, 14");

        JLabel lblNewDbName = new JLabel("New DB Name:");
        panelSettings.add(lblNewDbName, "2, 16, right, default");

        txtNewDbName = new JTextField();
        panelSettings.add(txtNewDbName, "4, 16, fill, default");
        txtNewDbName.setColumns(10);

        JButton btnCreateDb = new JButton("Create DB");
        btnCreateDb.setToolTipText("create h2db directory into your \"" + GUIConfig.CONFIG_DOT_DIR + "/\"");
        btnCreateDb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String newDbName = txtNewDbName.getText().trim();
                if (Strings.isEmpty(newDbName)) {
                    JOptionPane.showMessageDialog(DemoPanel.this.getWindowFrame(), "Enter new db name.");
                    return;
                }
                for (int i = 0; i < cmbSelectDb.getItemCount(); i++) {
                    final String existedDbName = cmbSelectDb.getItemAt(i);
                    if (newDbName.equals(existedDbName)) {
                        JOptionPane.showMessageDialog(
                            DemoPanel.this.getWindowFrame(),
                            "db name[" + newDbName + "] already exists. Enter other db name.");
                        return;
                    }
                }
                /* TODO 将来的にはもう少しDB名として使える文字種を限定する。
                 * 基本的には[-_0-9a-zA-Z]で十分なはず。
                 * ただしデフォルトはユーザ名を使うので、ユーザ名に日本語が含まれている可能性は高い。
                 * 日本語許すなら、記号などのpunctationは除外するとか。
                 * ";" とかunix系ではファイル/ディレクトリ名として普通に使えるが、そうされると
                 * JDBC URL生成するところで(結果としてインジェクション相当になり)不具合となる。
                 * (ローカルアプリなので、インジェクションどうこうというよりは、
                 *  ユーザが無邪気に含めた記号が原因でエラーになるのがダサい)
                 */
                if (!DataStore.migrateOrCreateIfNotExists(newDbName)) {
                    JOptionPane.showMessageDialog(
                        DemoPanel.this.getWindowFrame(),
                        "DB Creation Failure.",
                        "check burp extender's error outputs.",
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                JOptionPane.showMessageDialog(DemoPanel.this.getWindowFrame(), "DB Created Sucessfuly.");
                cmbSelectDb.insertItemAt(newDbName, 0);
                cmbSelectDb.setSelectedIndex(0);
            }
        });
        panelSettings.add(btnCreateDb, "6, 16");

        JLabel lblSelectDb = new JLabel("Select DB:");
        panelSettings.add(lblSelectDb, "2, 18, right, default");

        cmbSelectDb = new JComboBox<>();
        cmbSelectDb.setToolTipText("select \"h2db_xxxx\" dbname in your \"" + GUIConfig.CONFIG_DOT_DIR + "/\".");
        panelSettings.add(cmbSelectDb, "4, 18, fill, default");
        panelSettings.setFocusTraversalPolicy(
            new FocusTraversalOnArray(
                new Component[] {
                    btnStopWebUi,
                    lblTargetHost,
                    btnStartWebUi,
                    scrollPaneTargetHost,
                    txtaTargetHost,
                    lblExcludeFilenameExtension,
                    scrollPaneExcludeFilenameExtension,
                    txtaExcludeFilenameExtension,
                    separator,
                    lblAdvancedSetting,
                    lblInternalWebUIPort,
                    spnWebUIPort,
                    lblNewDbName,
                    txtNewDbName,
                    btnCreateDb,
                    lblSelectDb,
                    cmbSelectDb }));
        setFocusTraversalPolicy(
            new FocusTraversalOnArray(
                new Component[] {
                    btnStartWebUi,
                    btnStopWebUi,
                    lblTargetHost,
                    txtaTargetHost,
                    scrollPaneTargetHost,
                    lblExcludeFilenameExtension,
                    scrollPaneExcludeFilenameExtension,
                    spnWebUIPort,
                    txtNewDbName,
                    btnCreateDb,
                    cmbSelectDb,
                    txtaExcludeFilenameExtension,
                    panelSettings,
                    separator,
                    lblAdvancedSetting,
                    lblInternalWebUIPort,
                    lblNewDbName,
                    lblSelectDb }));

        loadSettings();
    }

    /**
     * updating some Swing/AWT component. you MUST call from event dispatcher thread (or use EventQueue.invokeLater())
     */
    public void loadSettings() {
        try {
            GUIConfig gc = this.appContext.loadDefaultConfig();
            spnWebUIPort.setValue(gc.getWebuiPort());
            for (String dbname : gc.getDbNames()) {
                cmbSelectDb.insertItemAt(dbname, 0);
            }
            cmbSelectDb.setSelectedIndex(0);
            txtaTargetHost.setText(String.join("\n", gc.getTargetHostNames()));
            txtaExcludeFilenameExtension.setText(String.join("\n", gc.getExcludeFilenameExtensions()));
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                this.getWindowFrame(),
                ex.getMessage(),
                "Load Settings Failure.",
                JOptionPane.WARNING_MESSAGE);
            this.appContext.printErr(Throwables.getStackTraceAsString(ex));
        }
    }

    public void saveSettings() {
        GUIConfig gc = this.appContext.getConfig();
        gc.setWebuiPort(Integer.parseInt(spnWebUIPort.getValue().toString()));

        gc.setTargetHostNames(new ArrayList<>());
        String[] targetHostNames = txtaTargetHost.getText().split("\n");
        for (String s : targetHostNames) {
            s = s.trim();
            if (Strings.isEmpty(s)) {
                continue;
            }
            String p = GUIConfig.convertWildcardToRegexp(s);
            if (!GUIConfig.isValidRegexpPattern(p)) {
                continue;
            }
            gc.getTargetHostNames().add(s);
        }

        gc.setExcludeFilenameExtensions(new ArrayList<>());
        String[] excludeFilenameExtensions = txtaExcludeFilenameExtension.getText().split("\n");
        for (String s : excludeFilenameExtensions) {
            s = s.trim();
            if (Strings.isEmpty(s)) {
                continue;
            }
            gc.getExcludeFilenameExtensions().add(s);
        }
        try {
            gc.save(GUIConfig.DEFAULT_CONFIG_FILE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                this.getWindowFrame(),
                Throwables.getStackTraceAsString(ex),
                "Save Settings Failure.",
                JOptionPane.WARNING_MESSAGE);
            this.appContext.printErr(Throwables.getStackTraceAsString(ex));
        }
    }

    public JFrame getWindowFrame() {
        return (JFrame) SwingUtilities.getWindowAncestor(this);
    }
}
