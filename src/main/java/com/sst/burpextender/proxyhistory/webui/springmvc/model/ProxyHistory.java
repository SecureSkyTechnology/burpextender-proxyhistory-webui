package com.sst.burpextender.proxyhistory.webui.springmvc.model;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.AbstractListHandler;

import com.jgoodies.common.base.Strings;

import burp.IHttpService;
import lombok.Data;

@Data
public class ProxyHistory {
    String logContext;
    int messageRef;
    String protocol;
    String host;
    int port;
    String clientIp;
    String url;
    String requestMethod;
    byte[] requestBytes;
    String requestString = "";
    String requestCharset;
    Charset requestCharsetInstance;
    short responseStatusCode;
    byte[] responseBytes;
    String responseString = "";;
    String responseCharset;
    Charset responseCharsetInstance;
    Timestamp sendAt;
    Timestamp receivedAt;

    public static Charset getCharset(String charsetName) {
        if (Strings.isBlank(charsetName)) {
            return StandardCharsets.ISO_8859_1;
        }
        try {
            Charset charset = Charset.forName(charsetName);
            return charset;
        } catch (Exception ignore) {
            return StandardCharsets.ISO_8859_1;
        }
    }

    public static ProxyHistory create(ResultSet rs, boolean needsHttpBytes) throws SQLException {
        ProxyHistory r = new ProxyHistory();
        r.logContext = rs.getString("log_context");
        r.messageRef = rs.getInt("message_ref");
        r.protocol = rs.getString("protocol");
        r.host = rs.getString("host");
        r.port = rs.getInt("port");
        r.clientIp = rs.getString("client_ip");
        r.url = rs.getString("url");
        r.requestMethod = rs.getString("request_method");
        r.requestCharset = rs.getString("request_charset");
        r.requestCharsetInstance = getCharset(r.requestCharset);
        r.responseStatusCode = rs.getShort("response_status_code");
        r.responseCharset = rs.getString("response_charset");
        r.responseCharsetInstance = getCharset(r.responseCharset);
        r.sendAt = rs.getTimestamp("send_at");
        r.receivedAt = rs.getTimestamp("received_at");
        if (needsHttpBytes) {
            r.requestBytes = rs.getBytes("request_bytes");
            r.requestString = new String(r.requestBytes, r.requestCharsetInstance);
            r.responseBytes = rs.getBytes("response_bytes");
            r.responseString = new String(r.responseBytes, r.responseCharsetInstance);
        }
        return r;
    }

    public static ResultSetHandler<List<ProxyHistory>> createListResultSetHandler() {
        return new AbstractListHandler<ProxyHistory>() {
            @Override
            protected ProxyHistory handleRow(ResultSet rs) throws SQLException {
                return ProxyHistory.create(rs, false);
            }
        };
    }

    public static ResultSetHandler<ProxyHistory> createDetailResultSetHandler() {
        return new ResultSetHandler<ProxyHistory>() {
            @Override
            public ProxyHistory handle(ResultSet rs) throws SQLException {
                if (!rs.next()) {
                    return null;
                }
                return ProxyHistory.create(rs, true);
            }
        };
    }

    public static List<ProxyHistory> getList(Connection dbconn) throws SQLException {
        String sql =
            "select log_context, message_ref, protocol, host, port, client_ip, url, request_method, request_charset, response_status_code, response_charset, send_at, received_at from proxy_history order by send_at desc";
        QueryRunner runner = new QueryRunner();
        return runner.query(dbconn, sql, createListResultSetHandler());
    }

    public static ProxyHistory getDetail(Connection dbconn, final String logContext, final int messageRef)
            throws SQLException {
        String sql =
            "select log_context, message_ref, protocol, host, port, client_ip, url, request_method, request_bytes, request_charset, response_status_code, response_bytes, response_charset, send_at, received_at from proxy_history where log_context = ? and message_ref = ? order by send_at desc";
        QueryRunner runner = new QueryRunner();
        return runner.query(dbconn, sql, createDetailResultSetHandler(), logContext, messageRef);
    }

    public static int updateCharset(Connection dbconn, final String logContext, final int messageRef,
            final String requestCharset, final String responseCharset) throws SQLException {
        String sql =
            "update proxy_history set request_charset = ?, response_charset = ? where log_context = ? and message_ref = ?";
        QueryRunner runner = new QueryRunner();
        return runner.update(
            dbconn,
            sql,
            getCharset(requestCharset).name(),
            getCharset(responseCharset).name(),
            logContext,
            messageRef);
    }

    public static int insert(Connection dbconn, final String logContext, final int messageRef,
            final IHttpService httpService, final String clientIp, final String requestUrl, final String method,
            final byte[] reqdata) throws SQLException {
        QueryRunner runner = new QueryRunner();
        String sql =
            "insert into proxy_history(log_context, message_ref, protocol, host, port, client_ip, url, request_method, request_bytes, send_at)"
                + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, now())";
        return runner.update(
            dbconn,
            sql,
            logContext,
            messageRef,
            httpService.getProtocol(),
            httpService.getHost(),
            httpService.getPort(),
            clientIp,
            requestUrl,
            method,
            reqdata);
    }

    public static int update(Connection dbconn, final String logContext, final int messageRef, final byte[] resdata,
            final int statusCode) throws SQLException {
        QueryRunner runner = new QueryRunner();
        String sql =
            "update proxy_history set response_bytes = ?, response_status_code = ?, received_at = now() "
                + " where log_context = ? and message_ref = ?";
        return runner.update(dbconn, sql, resdata, statusCode, logContext, messageRef);
    }
}
