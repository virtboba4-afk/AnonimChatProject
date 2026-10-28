package org.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.WebSessionServerOAuth2AuthorizedClientRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.csrf.WebSessionServerCsrfTokenRepository;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    // Токены авторизованного клиента храним в WebSession (а она — в Redis), а не в памяти инстанса
    @Bean
    ServerOAuth2AuthorizedClientRepository authorizedClientsInWebSession() {
        return new WebSessionServerOAuth2AuthorizedClientRepository();
    }

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                  ReactiveClientRegistrationRepository clients) {
        OidcClientInitiatedServerLogoutSuccessHandler logoutSuccess =
                new OidcClientInitiatedServerLogoutSuccessHandler(clients);
        logoutSuccess.setPostLogoutRedirectUri("{baseUrl}/");

        return http
                .authorizeExchange(auth -> auth
                        .pathMatchers("/", "/index.html", "/favicon.ico", "/bff/csrf", "/bff/me",
                                "/oauth2/**", "/login/**", "/actuator/health").permitAll()
                        .anyExchange().authenticated())
                .oauth2Login(Customizer.withDefaults())
                .oauth2Client(Customizer.withDefaults())
                // Браузер ходит в шлюз с cookie SESSION, поэтому CSRF обязателен
                .csrf(csrf -> csrf.csrfTokenRepository(new WebSessionServerCsrfTokenRepository()))
                .logout(logout -> logout.logoutSuccessHandler(logoutSuccess))
                .build();
    }
}
