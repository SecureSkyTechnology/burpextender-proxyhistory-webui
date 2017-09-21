package com.sst.burpextender.proxyhistory.webui;

import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataStore {
    /* NOTE: h2dbを使うと、デフォルトでVM終了時にh2dbの接続が残ってたらcloseするshutdown hookが起動する。
     * しかし、burp拡張のclassloader中でh2dbがロードされる性質上、VM終了時のshutdown hookからは
     * shutdown hookの実行に必要なclass loadingに失敗し、以下のstacktraceがコンソールに出力される。
     * 
     * java.lang.NoClassDefFoundError: org/h2/mvstore/MVStore$2
     *         at org.h2.mvstore.MVStore.compactGetOldChunks(MVStore.java:1868)
     *         at org.h2.mvstore.MVStore.compact(MVStore.java:1805)
     *         at org.h2.mvstore.MVStore.writeInBackground(MVStore.java:2512)
     *         at org.h2.mvstore.MVStore$BackgroundWriterThread.run(MVStore.java:2720)
     * Caused by: java.lang.ClassNotFoundException: org.h2.mvstore.MVStore$2
     *         at java.net.URLClassLoader.findClass(URLClassLoader.java:381)
     *         at java.lang.ClassLoader.loadClass(ClassLoader.java:424)
     *         at java.lang.ClassLoader.loadClass(ClassLoader.java:357)
     *         ... 4 more
     * 
     * そのため、";DB_CLOSE_ON_EXIT=FALSE"を全てのJDBC URLに付与することで、shutdown hookを無効化した（つもり）。
     * これの簡易的なチェックとしては、burp拡張をロードしてロギング開始した状態でburpを終了させ、その後再起動してみる。
     * java -jar 実行したコンソールで、まずburp終了時に上記stacktraceが表示されず、
     * かつ、再起動時のh2dbのdbチェックで以下の例外が表示されなければOK.
     * 
     * org.h2.jdbc.JdbcSQLException: データベースが使用中です: null. 可能な解決策: 他の接続を全て閉じる; サーバモードを使う
     * Database may be already in use: null. Possible solutions: close all other connection(s); use the server mode [90020-196]
     * 
     */

    private static final Logger LOG = LoggerFactory.getLogger(DataStore.class);

    public static String getDbDir(String dbname) {
        return GUIConfig.CONFIG_DOT_DIR + "/h2db_" + dbname;
    }

    public static String getDbPath(String dbname) {
        return getDbDir(dbname) + "/datastore";
    }

    public static List<String> getDbNames() {
        List<String> dbNames = new ArrayList<>();
        File root = new File(GUIConfig.CONFIG_DOT_DIR);
        for (File f : root.listFiles()) {
            if (!f.isDirectory()) {
                continue;
            }
            if (f.getName().indexOf("h2db_") != 0) {
                continue;
            }
            // TODO もう少しファイル名のチェックを厳しくしないと、下のJDBC URL生成でインジェクションが発生する。
            // ";" とかunix系で普通に入れられるし・・・。
            // (インジェクションどうこうというよりは、ユーザが無邪気に含めた記号が原因でエラーになるのがダサい)
            String jdbcurl = "jdbc:h2:file:" + f.getAbsolutePath() + "/datastore;IFEXISTS=TRUE;DB_CLOSE_ON_EXIT=FALSE";
            boolean isH2db = false;
            Connection conn = null;
            try {
                conn = DriverManager.getConnection(jdbcurl, "sa", "");
                conn.close();
                isH2db = true;
            } catch (SQLException e) {
                LOG.error("db open error", e);
            } finally {
                DbUtils.closeQuietly(conn);
            }
            if (!isH2db) {
                continue;
            }
            String dbname = f.getName().replace("h2db_", "");
            LOG.info("dbname detected: {}", dbname);
            dbNames.add(dbname);
        }
        Collections.sort(dbNames);
        return dbNames;
    }

    public static boolean migrateOrCreateIfNotExists(String dbname) {
        String dbpath = getDbPath(dbname);
        String jdbcurl = "jdbc:h2:file:" + dbpath + ";DB_CLOSE_ON_EXIT=FALSE";
        LOG.info("new jdbcurl = {}", jdbcurl);
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(jdbcurl, "sa", "");

            DataSource ds = new CustomFlywayDataSource(new PrintWriter(System.out), conn);
            Flyway flyway = new Flyway();
            flyway.setDataSource(ds);
            // explicitly set h2db driver loaded (= including this jar file) class loader
            flyway.setClassLoader(conn.getClass().getClassLoader());
            flyway.migrate();
            LOG.info("db[{}] migration success", dbname);

            // may be already closed -> reconnect.
            DbUtils.closeQuietly(conn);
            conn = DriverManager.getConnection(jdbcurl, "sa", "");

            QueryRunner r = new QueryRunner();
            Long cnt = r.query(conn, "select count(*) from PROXY_HISTORY", new ScalarHandler<Long>());
            LOG.info("db[{}] open/creation success(select count(*) from logtable returns {})", dbname, cnt);

            return true;
        } catch (SQLException e) {
            LOG.error("db[" + dbname + "] open/migrate/creation error", e);
            return false;
        } finally {
            DbUtils.closeQuietly(conn);
        }
    }

    public static Connection getConnection(String dbname) throws SQLException {
        String dbpath = getDbPath(dbname);
        String jdbcurl = "jdbc:h2:file:" + dbpath + ";DB_CLOSE_ON_EXIT=FALSE";
        LOG.info("connect jdbcurl = {}", jdbcurl);
        Connection conn = DriverManager.getConnection(jdbcurl, "sa", "");

        DataSource ds = new CustomFlywayDataSource(new PrintWriter(System.out), conn);
        Flyway flyway = new Flyway();
        flyway.setDataSource(ds);
        // explicitly set h2db driver loaded (= including this jar file) class loader
        flyway.setClassLoader(conn.getClass().getClassLoader());
        flyway.migrate();
        LOG.info("db[{}] migration success", dbname);

        // may be already closed -> reconnect.
        DbUtils.closeQuietly(conn);
        conn = DriverManager.getConnection(jdbcurl, "sa", "");

        return conn;
    }

    public static void closeConnection(Connection conn) {
        DbUtils.closeQuietly(conn);
    }
}
