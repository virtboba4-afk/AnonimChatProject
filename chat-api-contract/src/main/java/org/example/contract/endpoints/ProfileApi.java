package org.example.contract.endpoints;

import org.example.contract.dto.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Profiles", description = "Управление профилями для чат-рулетки")
@RequestMapping(value = "/api/profiles", produces = APPLICATION_JSON_VALUE)
public interface ProfileApi {

    @GetMapping("/{id}")
    EntityModel<ProfileResponse> getProfileById(@PathVariable Long id);

    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    ResponseEntity<EntityModel<ProfileResponse>> createProfile(@Valid @RequestBody ProfileRequest request);

    @PutMapping(value = "/{id}", consumes = APPLICATION_JSON_VALUE)
    EntityModel<ProfileResponse> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProfileRequest request);

    @PatchMapping(value = "/{id}", consumes = APPLICATION_JSON_VALUE)
    EntityModel<ProfileResponse> updateProfilePartially(
            @PathVariable Long id,
            @Valid @RequestBody PatchProfileRequest request);

    @GetMapping
    PagedResponse<EntityModel<ProfileResponse>> getAllProfiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size);
}