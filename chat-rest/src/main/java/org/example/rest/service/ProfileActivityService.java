package org.example.rest.service;

import org.example.rest.client.ActivityEntry;
import org.example.rest.client.AuditClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProfileActivityService {

    private final ProfileService profileService;
    private final AuditClient auditClient;

    public ProfileActivityService(ProfileService profileService, AuditClient auditClient) {
        this.profileService = profileService;
        this.auditClient = auditClient;
    }

    public List<ActivityEntry> activityOf(UUID profileId) {
        profileService.findById(profileId);
        return auditClient.activityOf(profileId);
    }
}
