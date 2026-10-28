package org.example.gateway.config;

import io.netty.channel.ConnectTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * Сетевые сбои upstream-сервиса переводим в 502/504 вместо 500,
 * чтобы отличать недоступность backend от ошибки самого шлюза.
 */
@Component
@Order(-2)
public class UpstreamErrorHandler implements WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(UpstreamErrorHandler.class);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = resolve(ex);
        if (status == null || exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        log.warn("Upstream failure on {} {}: {}", exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(), ex.toString());

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"status\":" + status.value() + ",\"error\":\"" + status.getReasonPhrase() + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    private HttpStatus resolve(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof ConnectTimeoutException || t instanceof TimeoutException) {
                return HttpStatus.GATEWAY_TIMEOUT;
            }
            if (t instanceof ConnectException) {
                return HttpStatus.BAD_GATEWAY;
            }
        }
        return null;
    }
}
