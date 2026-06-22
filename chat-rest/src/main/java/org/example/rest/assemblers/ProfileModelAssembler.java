package org.example.rest.assemblers;

import org.example.rest.controllers.ProfileController;
import org.example.contract.dto.ProfileResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class ProfileModelAssembler implements RepresentationModelAssembler<ProfileResponse, EntityModel<ProfileResponse>> {

    @Override
    public EntityModel<ProfileResponse> toModel(ProfileResponse profile) {

        return EntityModel.of(profile,
                linkTo(methodOn(ProfileController.class).getProfileById(profile.getId())).withSelfRel()
        );
    }
}