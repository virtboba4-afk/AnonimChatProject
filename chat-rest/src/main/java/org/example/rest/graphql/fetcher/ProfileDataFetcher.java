package org.example.rest.graphql.fetcher;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import org.example.contract.dto.ProfileRequest;
import org.example.contract.dto.ProfileResponse;
import org.example.contract.dto.UpdateProfileRequest;
import org.example.rest.graphql.types.ProfileInputGql;
import org.example.rest.service.ProfileService;

import java.util.List;

@DgsComponent
public class ProfileDataFetcher {

    private final ProfileService profileService;

    public ProfileDataFetcher(ProfileService profileService) {
        this.profileService = profileService;
    }

    @DgsQuery
    public ProfileResponse profile(@InputArgument String id) {
        return profileService.findById(Long.parseLong(id));
    }

    @DgsQuery
    public List<ProfileResponse> allProfiles(@InputArgument Integer page, @InputArgument Integer size) {
        return profileService.findAll(page != null ? page : 0, size != null ? size : 20).content();
    }

    @DgsMutation
    public ProfileResponse createProfile(@InputArgument ProfileInputGql input) {
        ProfileRequest request = new ProfileRequest(input.nickname(), input.age(), input.preferredLanguage());
        return profileService.create(request);
    }

    @DgsMutation
    public ProfileResponse updateProfile(@InputArgument String id, @InputArgument ProfileInputGql input) {
        UpdateProfileRequest request = new UpdateProfileRequest(input.nickname(), input.age(), input.preferredLanguage());
        return profileService.update(Long.parseLong(id), request);
    }
}