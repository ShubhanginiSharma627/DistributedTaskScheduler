package com.taskscheduler.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.taskscheduler.util.LoggingContext;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet filter for managing logging context and correlation IDs in HTTP requests.
 * Automatically sets correlation IDs for incoming requests and cleans up context.
 */
@Component
@Order(1)
public class LoggingFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
      
        String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = LoggingContext.generateCorrelationId();
        }
        
      
        LoggingContext.setCorrelationId(correlationId);
        
       
        httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);
        
      
        long startTime = System.currentTimeMillis();
        String method = httpRequest.getMethod();
        String uri = httpRequest.getRequestURI();
        String queryString = httpRequest.getQueryString();
        String fullUrl = queryString != null ? uri + "?" + queryString : uri;
        
        logger.info("HTTP_REQUEST_START - {} {}", method, fullUrl);
        
        try {
           
            chain.doFilter(request, response);
            
            long duration = System.currentTimeMillis() - startTime;
            int status = httpResponse.getStatus();
            
            if (status >= 400) {
                logger.warn("HTTP_REQUEST_COMPLETE - {} {} - Status: {}, Duration: {}ms", 
                        method, fullUrl, status, duration);
            } else {
                logger.info("HTTP_REQUEST_COMPLETE - {} {} - Status: {}, Duration: {}ms", 
                        method, fullUrl, status, duration);
            }
            
        } catch (Exception e) {
           
            long duration = System.currentTimeMillis() - startTime;
            logger.error("HTTP_REQUEST_ERROR - {} {} - Duration: {}ms, Error: {}", 
                    method, fullUrl, duration, e.getMessage(), e);
            throw e;
            
        } finally {
          
            LoggingContext.clearAll();
        }
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("LoggingFilter initialized");
    }
    
    @Override
    public void destroy() {
        logger.info("LoggingFilter destroyed");
    }
}