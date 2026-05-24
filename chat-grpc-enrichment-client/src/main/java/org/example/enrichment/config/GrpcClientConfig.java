package org.example.enrichment.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.example.grpc.analytics.ProfileAnalyticsServiceGrpc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PreDestroy;

@Configuration
public class GrpcClientConfig {

    private ManagedChannel channel;

    @Bean
    public ManagedChannel managedChannel(@Value("${grpc.server.host}") String host,
                                         @Value("${grpc.server.port}") int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        return channel;
    }

    @Bean
    public ProfileAnalyticsServiceGrpc.ProfileAnalyticsServiceBlockingStub blockingStub(ManagedChannel channel) {
        return ProfileAnalyticsServiceGrpc.newBlockingStub(channel);
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null) {
            channel.shutdown();
        }
    }
}