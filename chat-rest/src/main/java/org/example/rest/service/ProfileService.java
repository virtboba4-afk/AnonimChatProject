package org.example.rest.service;

import org.example.contract.dto.*;
import org.example.rest.event.ProfileEventPublisher;
import org.example.contract.exception.ResourceNotFoundException;
import org.example.rest.storage.InMemoryStorage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    private final InMemoryStorage storage;
    private final ProfileEventPublisher eventPublisher;

    public ProfileService(InMemoryStorage storage,ProfileEventPublisher eventPublisher) {
        this.storage = storage;
        this.eventPublisher = eventPublisher;
    }

    public ProfileResponse findById(Long id) {
        if (!storage.profiles.containsKey(id)) {
            throw new ResourceNotFoundException("Profile", id);
        }
        return storage.profiles.get(id);
    }

    public ProfileResponse create(ProfileRequest request) {
        long id = storage.profileSequence.incrementAndGet();
        ProfileResponse profile = ProfileResponse.builder()
                .id(id)
                .nickname(request.nickname())
                .age(request.age())
                .preferredLanguage(request.preferredLanguage())
                .matchingScore(100.0)
                .build();
        storage.profiles.put(id, profile);
        eventPublisher.publishProfileCreated(profile);
        return profile;
    }

    public ProfileResponse update(Long id, UpdateProfileRequest request) {
        findById(id);
        ProfileResponse updated = ProfileResponse.builder()
                .id(id)
                .nickname(request.nickname())
                .age(request.age())
                .preferredLanguage(request.preferredLanguage())
                .matchingScore(storage.profiles.get(id).getMatchingScore())
                .build();
        storage.profiles.put(id, updated);
        return updated;
    }

    public ProfileResponse patch(Long id, PatchProfileRequest request) {
        ProfileResponse existing = findById(id);
        ProfileResponse updated = ProfileResponse.builder()
                .id(id)
                .nickname(request.nickname() != null ? request.nickname() : existing.getNickname())
                .age(request.age() != null ? request.age() : existing.getAge())
                .preferredLanguage(request.preferredLanguage() != null ? request.preferredLanguage() : existing.getPreferredLanguage())
                .matchingScore(existing.getMatchingScore())
                .build();
        storage.profiles.put(id, updated);
        return updated;
    }

    public PagedResponse<ProfileResponse> findAll(int page, int size) {
        List<ProfileResponse> all = storage.profiles.values().stream()
                .sorted((p1, p2) -> p1.getId().compareTo(p2.getId()))
                .collect(Collectors.toList());

        int totalElements = all.size();
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 1;
        int from = page * size;
        int to = Math.min(from + size, totalElements);

        List<ProfileResponse> content = (from >= totalElements) ? List.of() : all.subList(from, to);
        return new PagedResponse<>(content, page, size, totalElements, totalPages, page >= totalPages - 1);
    }

    public void startSearch(Long id) {
        ProfileResponse profile = findById(id);
        eventPublisher.publishSearchStarted(profile);
        System.out.println("Пользователь " + profile.getNickname() + " начал поиск собеседника!");
    }
}
