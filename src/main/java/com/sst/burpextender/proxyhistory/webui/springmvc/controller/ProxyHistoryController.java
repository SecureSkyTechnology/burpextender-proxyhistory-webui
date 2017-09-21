package com.sst.burpextender.proxyhistory.webui.springmvc.controller;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import com.sst.burpextender.proxyhistory.webui.AppContext;
import com.sst.burpextender.proxyhistory.webui.springmvc.model.ProxyHistory;

@Controller
public class ProxyHistoryController {

    @Autowired
    ServletContext servletContext;

    @Autowired
    HttpServletRequest servletRequest;

    @Autowired
    HttpServletResponse servletResponse;

    @GetMapping("/")
    public String list(@RequestAttribute Connection dbconn, Model model) throws SQLException {
        String dbname = (String) servletContext.getAttribute("dbname");
        model.addAttribute("dbname", dbname);
        model.addAttribute("proxyHistories", ProxyHistory.getList(dbconn));
        return "index";
    }

    @GetMapping("/proxy-history/{logContext}/{messageRef}")
    public String detail(@RequestAttribute Connection dbconn, Model model, @PathVariable String logContext,
            @PathVariable int messageRef) throws SQLException {
        model.addAttribute("logContext", logContext);
        model.addAttribute("messageRef", messageRef);
        ProxyHistory proxyHistory = ProxyHistory.getDetail(dbconn, logContext, messageRef);
        if (Objects.isNull(proxyHistory)) {
            servletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return "proxy-history-not-found";
        }
        model.addAttribute("proxyHistory", ProxyHistory.getDetail(dbconn, logContext, messageRef));
        model.addAttribute("charsetNames", AppContext.getAvailableCharsetNames());
        return "proxy-history";
    }

    @PostMapping("/proxy-history/{logContext}/{messageRef}")
    public String updateCharset(@RequestAttribute Connection dbconn, Model model, @PathVariable String logContext,
            @PathVariable int messageRef, @RequestParam String requestCharset, @RequestParam String responseCharset)
            throws SQLException {
        ProxyHistory.updateCharset(dbconn, logContext, messageRef, requestCharset, responseCharset);
        // TODO affected row check
        return "redirect:/proxy-history/{logContext}/{messageRef}";
    }
}
