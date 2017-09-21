package com.sst.burpextender.proxyhistory.webui.springmvc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.sst.burpextender.proxyhistory.webui.springmvc.interceptors.JdbcConnectionInterceptor;

@Configuration
@EnableWebMvc
@Import(ThymeleafConfig.class)
public class WebMvcConfig extends WebMvcConfigurerAdapter {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/staticfiles/**").addResourceLocations("/staticfiles/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JdbcConnectionInterceptor());
    }
}
