package org.example.gateway.config;

import io.micrometer.context.ContextRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class RequestIdWebFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestIdWebFilter.class);

    static {
        ContextRegistry.getInstance().registerThreadLocalAccessor(
                RequestIds.MDC_KEY,
                () -> MDC.get(RequestIds.MDC_KEY),
                value -> MDC.put(RequestIds.MDC_KEY, value),
                () -> MDC.remove(RequestIds.MDC_KEY));
    }

    private final int managementPort;

    public RequestIdWebFilter(@Value("${management.server.port}") int managementPort) {
        this.managementPort = managementPort;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest original = exchange.getRequest();
        InetSocketAddress local = original.getLocalAddress();
        if (local != null && local.getPort() == managementPort) {
            return chain.filter(exchange);
        }
        String requestId = RequestIds.normalize(original.getHeaders().get(RequestIds.HEADER));

        ServerHttpRequest request = original.mutate()
                .headers(headers -> headers.set(RequestIds.HEADER, requestId))
                .build();
        ServerWebExchange correlated = exchange.mutate().request(request).build();

        correlated.getResponse().beforeCommit(() -> {
            correlated.getResponse().getHeaders().set(RequestIds.HEADER, requestId);
            return Mono.empty();
        });

        return chain.filter(correlated)
                .doOnTerminate(() -> log.info("Request completed: {} {} -> {}",
                        original.getMethod(), original.getPath(), correlated.getResponse().getStatusCode()))
                .contextWrite(context -> context.put(RequestIds.MDC_KEY, requestId));
    }
}
