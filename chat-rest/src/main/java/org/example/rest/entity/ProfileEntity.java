package org.example.rest.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "profiles")
@Getter
@Setter
public class ProfileEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String nickname;

    @Column(nullable = false)
    private Integer age;

    @Column(name = "preferred_language", nullable = false, length = 10)
    private String preferredLanguage;

    @Column(name = "matching_score", nullable = false)
    private Double matchingScore = 100.0;

    @Column(name = "can_search", nullable = false)
    private boolean canSearch = true;

    @Version
    private Long version;

    public ProfileEntity() {}
}