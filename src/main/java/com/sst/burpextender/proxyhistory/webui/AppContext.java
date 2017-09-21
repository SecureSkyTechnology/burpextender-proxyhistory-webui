package com.sst.burpextender.proxyhistory.webui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.Setter;

public class AppContext {
    private static List<String> availableCharsetNames;

    static {
        Map<String, Charset> acs = Charset.availableCharsets();
        List<String> l = new ArrayList<>(acs.size());
        for (Map.Entry<String, Charset> e : acs.entrySet()) {
            l.add(e.getValue().name());
        }
        Collections.sort(l);
        // for usability adjustment in Japan locale
        int sz = l.size();
        List<String> prefs =
            Arrays.asList("UTF-8", "Shift_JIS", "windows-31j", "EUC-JP", "ISO-2022-JP", "ISO-8859-1", "US-ASCII");
        l.removeAll(prefs);
        List<String> l2 = new ArrayList<>(sz);
        l2.addAll(prefs);
        l2.addAll(l);
        availableCharsetNames = Collections.unmodifiableList(l2);
    }

    @Getter
    @Setter
    private GUIConfig config;
    @Getter
    private final WebUIApp webUIApp;
    @Setter
    private PrintWriter consoleOutputWriter = new PrintWriter(System.out, true);
    @Setter
    private PrintWriter consoleErrorWriter = new PrintWriter(System.err, true);
    @Getter
    private Connection connection;
    @Getter
    private final String logContext;

    private final Clock clock;
    private volatile boolean loggingActive = false;
    private List<Pattern> targetHostNameRegexps = Collections.emptyList();
    private List<String> excludeFilenameExtensions = Collections.emptyList();

    public AppContext(Clock clock) throws IOException {
        this.config = loadDefaultConfig();
        this.webUIApp = new WebUIApp();
        this.clock = clock;
        LocalDateTime ldt0 = LocalDateTime.now(clock);
        DateTimeFormatter dtf0 = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        this.logContext = ldt0.format(dtf0);
    }

    public GUIConfig loadDefaultConfig() throws IOException {
        File d = new File(GUIConfig.CONFIG_DOT_DIR);
        if (d.exists()) {
            if (!d.isDirectory()) {
                this.consoleErrorWriter.println(GUIConfig.CONFIG_DOT_DIR + " was exists, but not directory.");
                throw new FileNotFoundException(GUIConfig.DEFAULT_CONFIG_FILE.getAbsolutePath());
            }
        } else {
            d.mkdirs();
        }
        if (!GUIConfig.DEFAULT_CONFIG_FILE.exists()) {
            GUIConfig defc = new GUIConfig();
            defc.save(GUIConfig.DEFAULT_CONFIG_FILE);
        }
        DataStore.migrateOrCreateIfNotExists(GUIConfig.DEFAULT_DB_NAME);
        this.config = GUIConfig.load(GUIConfig.DEFAULT_CONFIG_FILE);
        return this.config;
    }

    public static List<String> getAvailableCharsetNames() {
        return AppContext.availableCharsetNames;
    }

    public long now() {
        return this.clock.millis();
    }

    public void printOut(String s) {
        this.consoleOutputWriter.println(s);
    }

    public void printErr(String s) {
        this.consoleErrorWriter.println(s);
    }

    public synchronized void startLogging(String dbname, List<Pattern> targetHostNameRegexps,
            List<String> exlcudeFilenameExtensions) throws SQLException {
        this.loggingActive = true;
        this.targetHostNameRegexps = targetHostNameRegexps;
        this.excludeFilenameExtensions = exlcudeFilenameExtensions;
        this.connection = DataStore.getConnection(dbname);
    }

    public boolean isLoggigActive() {
        return this.loggingActive;
    }

    public synchronized void stopLogging() {
        this.loggingActive = false;
        DataStore.closeConnection(connection);
    }

    // TODO make thread safety, Akka could be save us from hell :)
    public boolean isLoggingTarget(String host, URL url) {
        boolean matched = false;
        for (Pattern p : targetHostNameRegexps) {
            if (p.matcher(host).find()) {
                matched = true;
                break;
            }
        }
        if (!matched) {
            return false;
        }
        String path = url.getPath();
        String[] dotseps = path.split("\\.");
        if (dotseps.length < 2) {
            // no filename extension like ".../"
            return true;
        }
        String lastSep = dotseps[dotseps.length - 1];
        for (String ext : excludeFilenameExtensions) {
            if (ext.equals(lastSep)) {
                return false;
            }
        }
        return true;
    }
}
