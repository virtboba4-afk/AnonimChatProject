package org.example.rest.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class RequestIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = RequestIds.normalize(Collections.list(request.getHeaders(RequestIds.HEADER)));
        String previous = MDC.get(RequestIds.MDC_KEY);
        MDC.put(RequestIds.MDC_KEY, requestId);
        response.setHeader(RequestIds.HEADER, requestId);
        try {
            chain.doFilter(request, response);
            log.info("Request completed: {} {} -> {}", request.getMethod(), request.getRequestURI(), response.getStatus());
        } finally {
            if (previous == null) {
                MDC.remove(RequestIds.MDC_KEY);
            } else {
                MDC.put(RequestIds.MDC_KEY, previous);
            }
        }
    }
}
