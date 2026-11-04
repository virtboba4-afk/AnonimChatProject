package org.example.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.WebSessionServerOAuth2AuthorizedClientRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.csrf.WebSessionServerCsrfTokenRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult;

import java.net.InetSocketAddress;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    // Токены авторизованного клиента храним в WebSession (а она — в Redis), а не в памяти инстанса
    @Bean
    ServerOAuth2AuthorizedClientRepository authorizedClientsInWebSession() {
        return new WebSessionServerOAuth2AuthorizedClientRepository();
    }

    @Bean
    @Order(1)
    SecurityWebFilterChain managementChain(ServerHttpSecurity http,
                                           @Value("${management.server.port}") int managementPort) {
        return http
                .securityMatcher(exchange -> {
                    InetSocketAddress local = exchange.getRequest().getLocalAddress();
                    return local != null && local.getPort() == managementPort
                            ? MatchResult.match()
                            : MatchResult.notMatch();
                })
                .authorizeExchange(auth -> auth.anyExchange().permitAll())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }

    @Bean
    @Order(2)
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                  ReactiveClientRegistrationRepository clients) {
        OidcClientInitiatedServerLogoutSuccessHandler logoutSuccess =
                new OidcClientInitiatedServerLogoutSuccessHandler(clients);
        logoutSuccess.setPostLogoutRedirectUri("{baseUrl}/");

        return http
                .authorizeExchange(auth -> auth
                        .pathMatchers("/", "/index.html", "/favicon.ico", "/bff/csrf", "/bff/me",
                                "/oauth2/**", "/login/**", "/livez", "/readyz").permitAll()
                        .anyExchange().authenticated())
                .oauth2Login(Customizer.withDefaults())
                .oauth2Client(Customizer.withDefaults())
                // Браузер ходит в шлюз с cookie SESSION, поэтому CSRF обязателен
                .csrf(csrf -> csrf.csrfTokenRepository(new WebSessionServerCsrfTokenRepository()))
                .logout(logout -> logout.logoutSuccessHandler(logoutSuccess))
                .build();
    }
}
