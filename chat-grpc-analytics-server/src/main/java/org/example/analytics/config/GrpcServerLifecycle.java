package org.example.analytics.config;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.example.analytics.service.ProfileAnalyticsServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GrpcServerLifecycle implements CommandLineRunner {

    private Server server;
    private final ProfileAnalyticsServiceImpl analyticsService;
    private final int port;

    public GrpcServerLifecycle(ProfileAnalyticsServiceImpl analyticsService,
                               @Value("${grpc.server.port:9090}") int port) {
        this.analyticsService = analyticsService;
        this.port = port;
    }

    @PostConstruct
    public void start() throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(analyticsService)
                .build()
                .start();
        System.out.println(" [gRPC SERVER] Запущен и слушает порт: " + port);
    }


    @Override
    public void run(String... args) throws Exception {
        if (server != null) {
            server.awaitTermination();
        }
    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            System.out.println(" [gRPC SERVER] Выключается...");
            server.shutdown();
        }
    }
}