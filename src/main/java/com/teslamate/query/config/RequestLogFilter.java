package com.teslamate.query.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class RequestLogFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long started = System.nanoTime();
        return chain.filter(exchange).doFinally(signal -> {
            String path = exchange.getRequest().getURI().getRawPath();
            if (path == null || skip(path)) {
                return;
            }
            String query = exchange.getRequest().getURI().getRawQuery();
            String url = (query == null || query.isBlank()) ? path : path + "?" + query;
            HttpStatusCode status = exchange.getResponse().getStatusCode();
            int code = status == null ? 0 : status.value();
            log.info("{} {} {} {}ms",
                    exchange.getRequest().getMethod(),
                    url,
                    code,
                    (System.nanoTime() - started) / 1_000_000);
        });
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private static boolean skip(String path) {
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/api/v1/health");
    }
}
