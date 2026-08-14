package com.teslamate.query.security;

import com.teslamate.query.config.QueryProperties;
import com.teslamate.query.dto.ErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Component
public class ApiKeyAuthFilter implements WebFilter, Ordered {

    private final QueryProperties properties;
    private final JsonMapper jsonMapper;
    private final Set<String> keys;

    public ApiKeyAuthFilter(QueryProperties properties, JsonMapper jsonMapper) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        this.keys = new HashSet<>(properties.resolvedApiKeys());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!properties.isAuthEnabled()) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getPath().value();
        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        String key = exchange.getRequest().getHeaders().getFirst("X-API-Key");
        if (key == null || key.isBlank()) {
            key = exchange.getRequest().getQueryParams().getFirst("api_key");
        }

        if (key == null || !keys.contains(key)) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            ErrorResponse body = new ErrorResponse(
                    "UNAUTHORIZED",
                    "Missing or invalid API key. Provide X-API-Key header.",
                    Instant.now(),
                    path
            );
            DataBuffer buffer = response.bufferFactory().wrap(jsonMapper.writeValueAsBytes(body));
            return response.writeWith(Mono.just(buffer));
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private boolean isPublic(String path) {
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/error")
                || path.equals("/api/v1/health");
    }
}
