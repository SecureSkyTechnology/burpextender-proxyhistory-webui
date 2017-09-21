package com.sst.burpextender.proxyhistory.webui.swingui;

import java.awt.EventQueue;
import java.time.Clock;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import com.google.common.base.Throwables;
import com.sst.burpextender.proxyhistory.webui.AppContext;

public class GUIMain {

    /** 
     * See Effective-Java 2nd, Item 4 / Checkstyle:HideUtilityClassConstructor
     */
    private GUIMain() {
        throw new AssertionError("Java Rationale: Utility class prohibits Instantiation.");
    }

    /**
     * Launch the application.
     * 
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        AppContext appContext = new AppContext(Clock.systemDefaultZone());

        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    MainWindow window = new MainWindow(appContext);
                    window.show();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(
                        null,
                        Throwables.getStackTraceAsString(e),
                        "error:" + e.getMessage(),
                        JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        });
    }

}
