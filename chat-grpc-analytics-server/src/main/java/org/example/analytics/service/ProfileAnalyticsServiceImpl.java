package org.example.analytics.service;

import io.grpc.stub.StreamObserver;
import org.example.grpc.analytics.AnalyzeProfileRequest;
import org.example.grpc.analytics.AnalyzeProfileResponse;
import org.example.grpc.analytics.ProfileAnalyticsServiceGrpc;
import org.springframework.stereotype.Service;

@Service
public class ProfileAnalyticsServiceImpl extends ProfileAnalyticsServiceGrpc.ProfileAnalyticsServiceImplBase {

    @Override
    public void analyzeProfile(AnalyzeProfileRequest request, StreamObserver<AnalyzeProfileResponse> responseObserver) {

        long profileId = request.getProfileId();
        int age = request.getAge();
        String lang = request.getPreferredLanguage();

        double baseScore = 100.0;
        String category = (age >= 18) ? "ADULT" : "TEEN";

        if ("ADULT".equals(category)) {
            baseScore += 50.0;
        }
        if ("EN".equalsIgnoreCase(lang)) {
            baseScore += 20.0;
        }

        AnalyzeProfileResponse response = AnalyzeProfileResponse.newBuilder()
                .setProfileId(profileId)
                .setMatchingScore(baseScore)
                .setCategoryLevel(category)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        System.out.println(" [gRPC SERVER] Рассчитан рейтинг " + baseScore + " для профиля ID: " + profileId);
    }
}