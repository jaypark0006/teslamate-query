package com.teslamate.query.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teslamate.query.config.QueryProperties;
import com.teslamate.query.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final QueryProperties properties;
    private final ObjectMapper objectMapper;
    private final Set<String> keys;

    public ApiKeyAuthFilter(QueryProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.keys = new HashSet<>(properties.resolvedApiKeys());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.isAuthEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (isPublic(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = request.getHeader("X-API-Key");
        if (key == null || key.isBlank()) {
            key = request.getParameter("api_key");
        }

        if (key == null || !keys.contains(key)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponse body = new ErrorResponse(
                    "UNAUTHORIZED",
                    "Missing or invalid API key. Provide X-API-Key header.",
                    Instant.now(),
                    path
            );
            objectMapper.writeValue(response.getOutputStream(), body);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublic(String path) {
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/error")
                || path.equals("/api/v1/health");
    }
}
