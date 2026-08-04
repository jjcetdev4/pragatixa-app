package com.pragatix.infrastructure.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        if (request.getQueryString() != null) {
            uri += "?" + request.getQueryString();
        }

        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }

        log.info("REQUEST | Method: {} | URI: {} | Client IP: {}", method, uri, clientIp);

        try {
            filterChain.doFilter(request, response);

            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();

            HttpStatus httpStatus = HttpStatus.resolve(status);
            String statusText = httpStatus != null ? httpStatus.getReasonPhrase() : "";

            if (status >= 500) {
                // Attempt to retrieve exception if handled by ControllerAdvice
                Exception ex = (Exception) request.getAttribute("jakarta.servlet.error.exception");
                if (ex == null) {
                    ex = (Exception) request
                            .getAttribute("org.springframework.boot.web.servlet.error.DefaultErrorAttributes.ERROR");
                }

                String exceptionName = (ex != null) ? ex.getClass().getSimpleName() : "UnknownException";
                log.error("ERROR RESPONSE | Method: {} | URI: {} | Status: {} | Exception: {}", method, uri, status,
                        exceptionName);
            } else {
                log.info("RESPONSE | Method: {} | URI: {} | Status: {} {} | Time: {} ms", method, uri, status,
                        statusText, duration);
            }

        } catch (Exception ex) {
            log.error("ERROR | Method: {} | URI: {} | Status: 500 | Exception: {}", method, uri,
                    ex.getClass().getSimpleName(), ex);
            throw ex;
        }
    }
}
