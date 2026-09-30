package org.example.contract.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.util.UUID;

@Getter
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Relation(collectionRelation = "profiles", itemRelation = "profile")
public class ProfileResponse extends RepresentationModel<ProfileResponse> {
    private final UUID id;
    private final String nickname;
    private final Integer age;
    private final String preferredLanguage;
    private final Double matchingScore;
    private final boolean canSearch;
}