package com.sst.burpextender.proxyhistory.webui;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Objects;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.DispatcherServlet;

public class WebUIApp {
    private static final Logger LOG = LoggerFactory.getLogger(WebUIApp.class);

    private Server server;

    public WebUIApp() throws IOException {
        server = new Server();

        AnnotationConfigWebApplicationContext springContext = new AnnotationConfigWebApplicationContext();
        springContext.setClassLoader(this.getClass().getClassLoader());
        springContext.setConfigLocation("com.sst.burpextender.proxyhistory.webui.springmvc.config");

        DispatcherServlet dispatcherServlet = new DispatcherServlet(springContext);
        ServletHolder springServletHolder = new ServletHolder("mvc-dispatcher", dispatcherServlet);

        // ref: https://github.com/bkielczewski/example-spring-mvc-jetty
        // ref: https://github.com/fernandospr/spring-jetty-example
        ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        contextHandler.setErrorHandler(null);
        contextHandler.setContextPath("/");
        contextHandler.addServlet(springServletHolder, "/*");
        contextHandler.addEventListener(new ContextLoaderListener(springContext));
        contextHandler.setResourceBase(new ClassPathResource("/webui", WebUIApp.class).getURI().toString());

        /* NOTE: Burp Extender において、 burp.BurpExtender については jar をロードしたclass loader内で実行される。
         * 一方、Tabなど独自のUIを追加した場合は、UIから呼ばれるコードはBurp自身のclass loader内で実行される。
         * つまり、Webアプリのstart/stopをBurp上に追加したUIからそのまま = Swingのスレッド内から呼び出す場合、
         * その一連の過程で使われるclass loaderはburp自身のものであり、
         * 「Burp Extender の jar 内のclassを見つけることができない。」
         * # 実際に見てみると、Thread.currentThread().getContextClassLoader() が sun.misc.Launcher$AppClassLoader となる.
         * 
         * その結果として、特に何の手当もしなかった場合、以下のような様々な、予期せぬclass loading関連の例外に遭遇する。
         * - Spring で setConfigLocation() で指定したパッケージがスキャンされない。
         * - Spring で @ComponentScan(basePackages = xxx) で指定したパッケージがスキャンされない。
         * ==> 結果として Spring MVC の初期化時に "No annotated classes found for specified class/package" のログが出力される。
         * - Spring + Thymeleaf 内部でのSpring EL式の生成で class not found 例外発生
         * 
         * そこで、Jetty側のレベルでclass loaderをカスタマイズする。
         * 本クラスのインスタンスをロードした class loader であれば、Burp Extender の jar をロードしているので、それを使うようにする。
         * これにより、Spring の annotation class のスキャンや、Spring MVC 内部で発生する様々な class loading が本来の形で動作するようになる。
         */
        contextHandler.setClassLoader(this.getClass().getClassLoader());

        CharacterEncodingFilter utf8Filter = new CharacterEncodingFilter();
        utf8Filter.setEncoding("UTF-8");
        utf8Filter.setForceEncoding(true);
        FilterHolder filterHolder = new FilterHolder(utf8Filter);
        EnumSet<DispatcherType> allDispatcher = EnumSet.of(
            DispatcherType.ASYNC,
            DispatcherType.ERROR,
            DispatcherType.FORWARD,
            DispatcherType.INCLUDE,
            DispatcherType.REQUEST);
        contextHandler.addFilter(filterHolder, "/*", allDispatcher);

        server.setHandler(contextHandler);
    }

    public void start(int listenPort, String dbname) throws Exception {
        if (Objects.nonNull(server) && server.isRunning()) {
            LOG.info("ineternal webui already running at port [" + listenPort + "].");
            throw new Exception("already running at port[" + listenPort + "]");
        }
        // remove old connectors
        Connector[] oldConnectors = server.getConnectors();
        if (Objects.nonNull(oldConnectors)) {
            for (Connector oldc : oldConnectors) {
                server.removeConnector(oldc);
            }
        }
        // add new connector
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(listenPort);
        server.setConnectors(new Connector[] { connector });
        // set dbname
        ServletContextHandler contextHandler = (ServletContextHandler) server.getHandler();
        contextHandler.setAttribute("dbname", dbname);
        server.start();
        LOG.info("internal webui server started with listening port [" + listenPort + "].");
    }

    public void stop() throws Exception {
        if (Objects.isNull(server)) {
            LOG.info("internal webui server is not initialized.");
            return;
        }
        if (!server.isStarted()) {
            LOG.info("internal webui server is not started.");
            return;
        }
        server.stop();
        LOG.info("internal webui server stopped.");
    }
}
