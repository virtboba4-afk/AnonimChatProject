package org.example.rest.endpoints;

import org.example.contract.endpoints.ProfileApi;
import org.example.rest.assemblers.ProfileModelAssembler;
import org.example.contract.dto.*;
import org.example.rest.service.ProfileService;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ProfileController implements ProfileApi {

    private final ProfileService profileService;
    private final ProfileModelAssembler assembler;

    public ProfileController(ProfileService profileService, ProfileModelAssembler assembler) {
        this.profileService = profileService;
        this.assembler = assembler;
    }

    @Override
    public EntityModel<ProfileResponse> getProfileById(Long id) {
        return assembler.toModel(profileService.findById(id));
    }

    @Override
    public ResponseEntity<EntityModel<ProfileResponse>> createProfile(ProfileRequest request) {
        ProfileResponse created = profileService.create(request);
        EntityModel<ProfileResponse> model = assembler.toModel(created);
        return ResponseEntity
                .created(model.getRequiredLink("self").toUri())
                .body(model);
    }

    @Override
    public EntityModel<ProfileResponse> updateProfile(Long id, UpdateProfileRequest request) {
        return assembler.toModel(profileService.update(id, request));
    }

    @Override
    public EntityModel<ProfileResponse> updateProfilePartially(Long id, PatchProfileRequest request) {
        return assembler.toModel(profileService.patch(id, request));
    }

    @Override
    public PagedResponse<EntityModel<ProfileResponse>> getAllProfiles(int page, int size) {
        PagedResponse<ProfileResponse> paged = profileService.findAll(page, size);

        List<EntityModel<ProfileResponse>> modelContent = paged.content().stream()
                .map(assembler::toModel)
                .collect(Collectors.toList());

        return new PagedResponse<>(
                modelContent,
                paged.pageNumber(),
                paged.pageSize(),
                paged.totalElements(),
                paged.totalPages(),
                paged.last()
        );
    }
}
