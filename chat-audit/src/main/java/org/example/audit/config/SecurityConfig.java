package org.example.audit.config;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain managementChain(HttpSecurity http,
                                               @Value("${management.server.port}") int managementPort) throws Exception {
        return http
                .securityMatcher(request -> request.getLocalPort() == managementPort)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new JwtRoleConverter());

        return http
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/livez", "/readyz", "/api/audit").permitAll()
                        .requestMatchers("/internal/diagnostics/**").hasRole("OPERATOR")
                        .requestMatchers("/internal/**").hasRole("SERVICE")
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
                .build();
    }
}
