package org.example.rest.config;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {

    private static final String REGISTRATION_ID = "chat-audit";
    private static final String SERVICE_PRINCIPAL = "chat-rest";

    @Bean
    AuthorizedClientServiceOAuth2AuthorizedClientManager serviceClientManager(
            ClientRegistrationRepository registrations,
            OAuth2AuthorizedClientService clients) {
        return new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, clients);
    }

    @Bean
    RestClient auditRestClient(RestClient.Builder builder,
                               AuthorizedClientServiceOAuth2AuthorizedClientManager manager,
                               @Value("${chat.audit.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(500);
        factory.setReadTimeout(2000);

        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId(REGISTRATION_ID)
                .principal(SERVICE_PRINCIPAL)
                .build();

        return builder.baseUrl(baseUrl)
                .requestFactory(factory)
                .requestInterceptor((request, body, execution) -> {
                    String requestId = MDC.get(RequestIds.MDC_KEY);
                    if (requestId != null) {
                        request.getHeaders().set(RequestIds.HEADER, requestId);
                    }
                    return execution.execute(request, body);
                })
                .requestInterceptor((request, body, execution) -> {
                    OAuth2AuthorizedClient client = manager.authorize(authorizeRequest);
                    request.getHeaders().set(HttpHeaders.AUTHORIZATION,
                            "Bearer " + client.getAccessToken().getTokenValue());
                    return execution.execute(request, body);
                })
                .build();
    }
}
