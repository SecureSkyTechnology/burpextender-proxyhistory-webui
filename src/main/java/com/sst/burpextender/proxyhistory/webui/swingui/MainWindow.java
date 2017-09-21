package com.sst.burpextender.proxyhistory.webui.swingui;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import com.sst.burpextender.proxyhistory.webui.AppContext;

public class MainWindow {

    private JFrame frame;
    private final AppContext appContext;

    /**
     * デフォルトで生成された public static void main() の中で呼ばれていた
     * setVisible(true)を外部から可能とするために手作業で追加したpublicメソッド。
     */
    public void show() {
        frame.setVisible(true);
    }

    /**
     * Create the application.
     */
    public MainWindow(AppContext appContext) {
        this.appContext = appContext;
        initialize();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        frame = new JFrame();
        frame.setTitle("ProxyHistoryWebUI standalone mode");
        frame.setBounds(100, 100, 600, 450);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout(0, 0));
        frame.getContentPane().add(new DemoPanel(appContext), BorderLayout.CENTER);
    }

}
