package jjcet.PragatiX.modules.profile.controller;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.modules.profile.dto.ProfileResponse;
import jjcet.PragatiX.modules.profile.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile() {
        ProfileResponse response = profileService.getMyProfile();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
