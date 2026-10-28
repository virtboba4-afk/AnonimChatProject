package org.example.gateway.controller;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/bff")
public class BffController {

    // Только сведения о пользователе — сами токены браузеру не отдаём
    @GetMapping("/me")
    public Mono<Map<String, Object>> me(@AuthenticationPrincipal OidcUser user) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("authenticated", user != null);
        if (user != null) {
            body.put("username", user.getPreferredUsername());
            body.put("subject", user.getSubject());
            body.put("authorities", user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
        }
        return Mono.just(body);
    }

    @GetMapping("/csrf")
    public Mono<Map<String, String>> csrf(ServerWebExchange exchange) {
        Mono<CsrfToken> token = exchange.getAttribute(CsrfToken.class.getName());
        if (token == null) {
            return Mono.empty();
        }
        return token.map(t -> Map.of(
                "headerName", t.getHeaderName(),
                "parameterName", t.getParameterName(),
                "token", t.getToken()));
    }
}
