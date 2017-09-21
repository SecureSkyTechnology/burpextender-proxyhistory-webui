package com.sst.burpextender.proxyhistory.webui.springmvc.interceptors;

import java.sql.Connection;
import java.util.Objects;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.jgoodies.common.base.Strings;
import com.sst.burpextender.proxyhistory.webui.DataStore;

public class JdbcConnectionInterceptor extends HandlerInterceptorAdapter {
    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        ServletWebRequest wr = new ServletWebRequest(req);
        ServletContext sctx = req.getServletContext();
        String dbname = (String) sctx.getAttribute("dbname");
        Connection dbconn = (Connection) wr.getAttribute("dbconn", WebRequest.SCOPE_REQUEST);
        if (Objects.isNull(dbconn) && !Strings.isEmpty(dbname)) {
            dbconn = DataStore.getConnection(dbname);
            wr.setAttribute("dbconn", dbconn, WebRequest.SCOPE_REQUEST);
        }
        return super.preHandle(req, res, handler);
    }

    @Override
    public void postHandle(HttpServletRequest req, HttpServletResponse res, Object handler, ModelAndView mav)
            throws Exception {
        // postHandle()はcontroller内で例外が発生しても呼ばれるので安心。
        // ただしこの後にviewが呼ばれるので、viewの中からはDB接続使えないようにしておく、という意図はある。
        ServletWebRequest wr = new ServletWebRequest(req);
        Connection dbconn = (Connection) wr.getAttribute("dbconn", WebRequest.SCOPE_REQUEST);
        DataStore.closeConnection(dbconn);
    }

}
