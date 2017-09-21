package burp;

import java.awt.Component;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;

import javax.swing.SwingUtilities;

import com.google.common.base.Throwables;
import com.sst.burpextender.proxyhistory.webui.AppContext;
import com.sst.burpextender.proxyhistory.webui.springmvc.model.ProxyHistory;
import com.sst.burpextender.proxyhistory.webui.swingui.DemoPanel;

public class BurpExtender implements IBurpExtender, IExtensionStateListener, ITab, IProxyListener {

    private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;
    private DemoPanel demoPanel;
    private AppContext appContext;

    @Override
    public void processProxyMessage(boolean messageIsRequest, IInterceptedProxyMessage message) {
        if (!appContext.isLoggigActive()) {
            return;
        }
        final IHttpService httpService = message.getMessageInfo().getHttpService();
        final byte[] reqdata = message.getMessageInfo().getRequest();
        final IRequestInfo ri = helpers.analyzeRequest(message.getMessageInfo().getHttpService(), reqdata);
        final URL url = ri.getUrl();
        if (!appContext.isLoggingTarget(httpService.getHost(), url)) {
            return;
        }
        final int messageRef = message.getMessageReference();
        final String clientIp = message.getClientIpAddress().getHostAddress();
        final StringBuilder sburl = new StringBuilder(httpService.getProtocol());
        sburl.append("://");
        sburl.append(httpService.getHost());
        if (("http".equals(httpService.getProtocol()) && 80 != httpService.getPort())
            || ("https").equals(httpService.getProtocol()) && 443 != httpService.getPort()) {
            // URL#toString()/toExternalForm()ではhttp-80/https-443でもポート番号が含まれてしまうので、独自に除去している。
            sburl.append(":");
            sburl.append(httpService.getPort());
        }
        sburl.append(ri.getUrl().getFile());
        final String requestUrl = sburl.toString();
        final String method = ri.getMethod();
        Connection conn = appContext.getConnection();
        if (messageIsRequest) {
            synchronized (conn) {
                // TODO make thread safety, Akka could be save us from hell :)
                try {
                    ProxyHistory.insert(
                        appContext.getConnection(),
                        appContext.getLogContext(),
                        messageRef,
                        httpService,
                        clientIp,
                        requestUrl,
                        method,
                        reqdata);
                } catch (SQLException e) {
                    callbacks.printError(Throwables.getStackTraceAsString(e));
                }
            }
        } else {
            final byte[] resdata = message.getMessageInfo().getResponse();
            final IResponseInfo resi = helpers.analyzeResponse(resdata);
            final short statusCode = resi.getStatusCode();
            synchronized (conn) {
                // TODO make thread safety, Akka could be save us from hell :)
                try {
                    final int affectedRows =
                        ProxyHistory.update(
                            appContext.getConnection(),
                            appContext.getLogContext(),
                            messageRef,
                            resdata,
                            statusCode);
                    if (1 != affectedRows) {
                        callbacks.printError(
                            "response log unknown result : affected row = "
                                + affectedRows
                                + ", logContext="
                                + appContext.getLogContext()
                                + ", messageRef="
                                + messageRef);
                    }
                } catch (SQLException e) {
                    callbacks.printError(Throwables.getStackTraceAsString(e));
                }
            }
        }
    }

    @Override
    public String getTabCaption() {
        return "ProxyHistoryWebUI";
    }

    @Override
    public Component getUiComponent() {
        return demoPanel;
    }

    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        this.callbacks = callbacks;
        this.helpers = callbacks.getHelpers();
        callbacks.setExtensionName("ProxyHistoryWebUI");

        try {
            // ONLY THIS PATTERN WORK FINE. ?? ClassLoader.loadClass() NOT WORK... X(
            Class.forName("org.h2.Driver");

            appContext = new AppContext(Clock.systemDefaultZone());
            appContext.setConsoleOutputWriter(new PrintWriter(callbacks.getStdout(), true));
            appContext.setConsoleErrorWriter(new PrintWriter(callbacks.getStderr(), true));

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    demoPanel = new DemoPanel(appContext);
                    callbacks.customizeUiComponent(demoPanel);
                    callbacks.addSuiteTab(BurpExtender.this);

                    // register ourselves as an HTTP listener
                    callbacks.registerProxyListener(BurpExtender.this);
                }
            });
        } catch (Exception e) {
            callbacks.printError(Throwables.getStackTraceAsString(e));
            e.printStackTrace();
        }
    }

    @Override
    public void extensionUnloaded() {
        this.callbacks.removeSuiteTab(this);
        this.callbacks.removeExtensionStateListener(this);
        this.appContext.stopLogging();
        try {
            this.appContext.getWebUIApp().stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
