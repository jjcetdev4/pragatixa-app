package jjcet.PragatiX.infrastructure.logging;

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

        // Only log request details at DEBUG to avoid flooding logs with every
        // request/response (see Railway 500 logs/sec rate limit).
        log.debug("REQUEST | Method: {} | URI: {} | Client IP: {}", method, uri, clientIp);

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
                log.warn("ERROR RESPONSE | Method: {} | URI: {} | Status: {} | Exception: {}", method, uri, status,
                        exceptionName);
            } else if (status >= 400) {
                // Client errors are logged at WARN so they remain visible without the
                // volume of a log line for every successful request.
                log.warn("CLIENT ERROR RESPONSE | Method: {} | URI: {} | Status: {} {} | Time: {} ms", method, uri,
                        status, statusText, duration);
            } else {
                // Successful responses are only logged at DEBUG.
                log.debug("RESPONSE | Method: {} | URI: {} | Status: {} {} | Time: {} ms", method, uri, status,
                        statusText, duration);
            }

        } catch (Exception ex) {
            log.warn("ERROR | Method: {} | URI: {} | Status: 500 | Exception: {}", method, uri,
                    ex.getClass().getSimpleName(), ex);
            throw ex;
        }
    }
}
