package com.sst.burpextender.proxyhistory.webui;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import com.google.common.util.concurrent.Uninterruptibles;
import com.jgoodies.common.base.Strings;
import com.sst.burpextender.proxyhistory.webui.springmvc.model.ProxyHistory;

import burp.IHttpService;

public class AppContextTest {

    String getTestDbName() {
        LocalDateTime ldNow0 =
            Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneOffset.systemDefault()).toLocalDateTime();
        DateTimeFormatter dtf0 = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return ldNow0.format(dtf0);
    }

    String testDbName = "";

    Connection dbconn = null;

    AppContext appContext = null;

    @Before
    public void prepareTestDatabase() throws Exception {
        testDbName = getTestDbName();
        DataStore.migrateOrCreateIfNotExists(testDbName);
        dbconn = DataStore.getConnection(testDbName);
    }

    @After
    public void cleanUp() throws Exception {
        appContext.stopLogging();
        DataStore.closeConnection(dbconn);
        try {
            MoreFiles
                .deleteRecursively(Paths.get(DataStore.getDbDir(testDbName)), RecursiveDeleteOption.ALLOW_INSECURE);
        } catch (IOException e) {
            // DONT make test failure.
            System.err.println("test db clean-up failure, remove manually later :P");
            e.printStackTrace();
        }
    }

    @Test
    public void testTypicalUseCase() throws Exception {
        final String clockAt = "2017-09-20T01:02:03.456Z";
        Instant i0 = Instant.parse(clockAt);
        ZoneOffset zo0 = ZoneOffset.UTC;
        Clock c0 = Clock.fixed(i0, zo0);
        appContext = new AppContext(c0);

        LocalDateTime ldNow0 = Instant.ofEpochMilli(appContext.now()).atZone(zo0).toLocalDateTime();
        DateTimeFormatter dtf0 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        assertEquals("2017-09-20T01:02:03.456", ldNow0.format(dtf0));
        assertEquals("20170920_010203", appContext.getLogContext());

        List<Pattern> targetHostNameRegexps =
            Arrays.asList(Pattern.compile("^localhost$"), Pattern.compile(".*\\.test\\.localhost"));
        List<String> exlcudeFilenameExtensions = Arrays.asList("js", "css");

        assertFalse(appContext.isLoggigActive());
        appContext.startLogging(testDbName, targetHostNameRegexps, exlcudeFilenameExtensions);
        assertTrue(appContext.isLoggigActive());

        assertEquals("^.*$", GUIConfig.convertWildcardToRegexp("*"));
        Pattern pat0 = Pattern.compile(GUIConfig.convertWildcardToRegexp("*"));
        assertTrue(pat0.matcher("a").matches());
        assertEquals("^aaa.*bbb\\.ccc$", GUIConfig.convertWildcardToRegexp("aaa*bbb.ccc"));
        Pattern pat1 = Pattern.compile(GUIConfig.convertWildcardToRegexp("aaa*bbb.ccc"));
        assertTrue(pat1.matcher("aaabbb.ccc").matches());
        assertTrue(pat1.matcher("aaaXbbb.ccc").matches());
        assertTrue(pat1.matcher("aaaXYZbbb.ccc").matches());

        assertTrue(appContext.isLoggingTarget("localhost", new URL("http://localhost/")));
        assertTrue(appContext.isLoggingTarget("localhost", new URL("http://localhost/aaa/")));
        assertTrue(appContext.isLoggingTarget("localhost", new URL("http://localhost/aaa/index.html")));
        assertTrue(appContext.isLoggingTarget("aaa.test.localhost", new URL("http://localhost/css/")));
        assertFalse(appContext.isLoggingTarget("xxx.localhost", new URL("http://localhost/")));
        assertFalse(appContext.isLoggingTarget("Xlocalhost", new URL("http://localhost/")));
        assertFalse(appContext.isLoggingTarget("localhostY", new URL("http://localhost/")));
        assertFalse(appContext.isLoggingTarget("localhost", new URL("http://localhost/test.js?aaa=bbb")));
        assertFalse(appContext.isLoggingTarget("localhost", new URL("http://localhost/test.test.css?aaa=bbb")));

        IHttpService testHttpService = new IHttpService() {
            @Override
            public String getHost() {
                return "localhost";
            }

            @Override
            public int getPort() {
                return 80;
            }

            @Override
            public String getProtocol() {
                return "http";
            }
        };
        final String testLogCtx = appContext.getLogContext();
        final int mref0 = 99;
        int affectedRows =
            ProxyHistory.insert(
                dbconn,
                testLogCtx,
                mref0,
                testHttpService,
                "clientIp",
                "http://localhost/",
                "GET",
                new byte[] { 0x1, 0x2 });
        assertEquals(1, affectedRows);

        affectedRows = ProxyHistory.update(dbconn, testLogCtx, mref0, new byte[] { 0x3, 0x4 }, 200);
        assertEquals(1, affectedRows);

        affectedRows =
            ProxyHistory.updateCharset(
                dbconn,
                testLogCtx,
                mref0,
                StandardCharsets.ISO_8859_1.name(),
                StandardCharsets.ISO_8859_1.name());
        assertEquals(1, affectedRows);

        ProxyHistory phr = ProxyHistory.getDetail(dbconn, testLogCtx, mref0);
        assertEquals(testLogCtx, phr.getLogContext());
        assertEquals(mref0, phr.getMessageRef());
        assertEquals(testHttpService.getProtocol(), phr.getProtocol());
        assertEquals(testHttpService.getHost(), phr.getHost());
        assertEquals(testHttpService.getPort(), phr.getPort());
        assertEquals("clientIp", phr.getClientIp());
        assertEquals("http://localhost/", phr.getUrl());
        assertEquals("GET", phr.getRequestMethod());
        assertEquals(new String(new byte[] { 0x1, 0x2 }, StandardCharsets.ISO_8859_1), phr.getRequestString());
        assertEquals(StandardCharsets.ISO_8859_1.name(), phr.getRequestCharset());
        assertEquals(StandardCharsets.ISO_8859_1, phr.getRequestCharsetInstance());
        assertEquals(200, phr.getResponseStatusCode());
        assertEquals(new String(new byte[] { 0x3, 0x4 }, StandardCharsets.ISO_8859_1), phr.getResponseString());
        assertEquals(StandardCharsets.ISO_8859_1.name(), phr.getResponseCharset());
        assertEquals(StandardCharsets.ISO_8859_1, phr.getResponseCharsetInstance());
        assertTrue(Objects.nonNull(phr.getSendAt()));
        assertTrue(Objects.nonNull(phr.getReceivedAt()));

        Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        final int mref1 = 1;
        ProxyHistory.insert(
            dbconn,
            testLogCtx,
            mref1,
            testHttpService,
            "clientIp1",
            "http://localhost/1",
            "POST",
            new byte[] { 0x5, 0x6 });
        ProxyHistory.update(dbconn, testLogCtx, mref1, new byte[] { 0x7, 0x8 }, 404);

        List<ProxyHistory> proxyHistories = ProxyHistory.getList(dbconn);
        assertEquals(2, proxyHistories.size());
        phr = proxyHistories.get(0);
        assertEquals(testLogCtx, phr.getLogContext());
        assertEquals(mref1, phr.getMessageRef());
        assertEquals(testHttpService.getProtocol(), phr.getProtocol());
        assertEquals(testHttpService.getHost(), phr.getHost());
        assertEquals(testHttpService.getPort(), phr.getPort());
        assertEquals("clientIp1", phr.getClientIp());
        assertEquals("http://localhost/1", phr.getUrl());
        assertEquals("POST", phr.getRequestMethod());
        assertTrue(Strings.isEmpty(phr.getRequestString()));
        assertEquals(StandardCharsets.UTF_8.name(), phr.getRequestCharset());
        assertEquals(StandardCharsets.UTF_8, phr.getRequestCharsetInstance());
        assertEquals(404, phr.getResponseStatusCode());
        assertTrue(Strings.isEmpty(phr.getResponseString()));
        assertEquals(StandardCharsets.UTF_8.name(), phr.getResponseCharset());
        assertEquals(StandardCharsets.UTF_8, phr.getResponseCharsetInstance());
        assertTrue(Objects.nonNull(phr.getSendAt()));
        assertTrue(Objects.nonNull(phr.getReceivedAt()));
        phr = proxyHistories.get(1);
        assertEquals(testLogCtx, phr.getLogContext());
        assertEquals(mref0, phr.getMessageRef());
        assertEquals(testHttpService.getProtocol(), phr.getProtocol());
        assertEquals(testHttpService.getHost(), phr.getHost());
        assertEquals(testHttpService.getPort(), phr.getPort());
        assertEquals("clientIp", phr.getClientIp());
        assertEquals("http://localhost/", phr.getUrl());
        assertEquals("GET", phr.getRequestMethod());
        assertTrue(Strings.isEmpty(phr.getRequestString()));
        assertEquals(StandardCharsets.ISO_8859_1.name(), phr.getRequestCharset());
        assertEquals(StandardCharsets.ISO_8859_1, phr.getRequestCharsetInstance());
        assertEquals(200, phr.getResponseStatusCode());
        assertTrue(Strings.isEmpty(phr.getResponseString()));
        assertEquals(StandardCharsets.ISO_8859_1.name(), phr.getResponseCharset());
        assertEquals(StandardCharsets.ISO_8859_1, phr.getResponseCharsetInstance());
        assertTrue(Objects.nonNull(phr.getSendAt()));
        assertTrue(Objects.nonNull(phr.getReceivedAt()));

        appContext.stopLogging();
        assertFalse(appContext.isLoggigActive());
    }
}
