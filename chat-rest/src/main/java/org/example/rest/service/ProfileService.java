package org.example.rest.service;

import org.example.contract.dto.*;
import org.example.rest.entity.ProfileEntity;
import org.example.rest.event.ProfileEventPublisher;
import org.example.contract.exception.ResourceNotFoundException;
import org.example.rest.repository.ProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final ProfileEventPublisher eventPublisher;

    public ProfileService(ProfileRepository profileRepository, ProfileEventPublisher eventPublisher) {
        this.profileRepository = profileRepository;
        this.eventPublisher = eventPublisher;
    }


    @Transactional(readOnly = true)
    public ProfileResponse findById(UUID id) {
        return toDto(findEntityById(id));
    }


    @Transactional
    public ProfileResponse create(ProfileRequest request) {

        if (profileRepository.existsByNickname(request.nickname())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Профиль с таким никнеймом уже существует");
        }

        ProfileEntity entity = new ProfileEntity();
        entity.setId(UUID.randomUUID());
        entity.setNickname(request.nickname());
        entity.setAge(request.age());
        entity.setPreferredLanguage(request.preferredLanguage());
        entity.setMatchingScore(100.0);
        entity.setCanSearch(true);

        ProfileEntity saved = profileRepository.save(entity);
        ProfileResponse profile = toDto(saved);

        eventPublisher.publishProfileCreated(profile);
        return profile;
    }

    @Transactional
    public ProfileResponse update(UUID id, UpdateProfileRequest request) {
        ProfileEntity entity = findEntityById(id);

        entity.setNickname(request.nickname());
        entity.setAge(request.age());
        entity.setPreferredLanguage(request.preferredLanguage());

        ProfileEntity saved = profileRepository.save(entity);
        return toDto(saved);
    }

    @Transactional
    public ProfileResponse patch(UUID id, PatchProfileRequest request) {
        ProfileEntity entity = findEntityById(id);

        if (request.nickname() != null) entity.setNickname(request.nickname());
        if (request.age() != null) entity.setAge(request.age());
        if (request.preferredLanguage() != null) entity.setPreferredLanguage(request.preferredLanguage());

        ProfileEntity saved = profileRepository.save(entity);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProfileResponse> findAll(int page, int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("id"));
        Page<ProfileEntity> entityPage = profileRepository.findAll(pageRequest);

        List<ProfileResponse> content = entityPage.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return new PagedResponse<>(
                content,
                page,
                size,
                (int) entityPage.getTotalElements(),
                entityPage.getTotalPages(),
                entityPage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public void startSearch(UUID id) {
        ProfileResponse profile = findById(id);
        eventPublisher.publishSearchStarted(profile);
        System.out.println("Пользователь " + profile.getNickname() + " начал поиск собеседника!");
    }

    @Transactional
    public void blockUser(UUID profileId) {
        ProfileEntity entity = findEntityById(profileId);
        entity.setCanSearch(false);
        profileRepository.save(entity);
        System.out.println(" Профиль ID " + profileId + " заблокирован. Поиск запрещен.");
    }


    private ProfileEntity findEntityById(UUID id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile", id));
    }


    private ProfileResponse toDto(ProfileEntity entity) {
        return ProfileResponse.builder()
                .id(entity.getId())
                .nickname(entity.getNickname())
                .age(entity.getAge())
                .preferredLanguage(entity.getPreferredLanguage())
                .matchingScore(entity.getMatchingScore())
                .canSearch(entity.isCanSearch())
                .build();
    }
}