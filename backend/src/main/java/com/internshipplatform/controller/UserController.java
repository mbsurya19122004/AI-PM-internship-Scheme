package com.internshipplatform.controller;

import com.internshipplatform.dto.ApiResponse;
import com.internshipplatform.service.ProfilePictureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final ProfilePictureService profilePictureService;

    @PostMapping("/me/profile-picture")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadProfilePicture(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) throws IOException {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }
        String filename = profilePictureService.uploadProfilePicture(userDetails.getUsername(), file);
        return ResponseEntity.ok(ApiResponse.success("Profile picture uploaded successfully",
                Map.of("profilePictureUrl", "/api/users/me/profile-picture")));
    }

    @GetMapping("/me/profile-picture")
    public ResponseEntity<byte[]> getProfilePicture(
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        byte[] imageBytes = profilePictureService.getProfilePicture(userDetails.getUsername());
        String contentType = profilePictureService.getProfilePictureContentType(userDetails.getUsername());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(imageBytes);
    }

    @DeleteMapping("/me/profile-picture")
    public ResponseEntity<ApiResponse<Void>> deleteProfilePicture(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }
        profilePictureService.deleteProfilePicture(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Profile picture deleted successfully"));
    }
}
