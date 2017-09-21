package com.sst.burpextender.proxyhistory.webui;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

public class CustomFlywayDataSource implements DataSource {
    private PrintWriter pw;
    private int loginTimeout = 0;
    private Connection conn;

    public CustomFlywayDataSource(PrintWriter pw, Connection conn) {
        this.pw = pw;
        this.conn = conn;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return pw;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        this.pw = out;
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        this.loginTimeout = seconds;
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return this.loginTimeout;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return this.conn;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return this.conn;
    }

}
