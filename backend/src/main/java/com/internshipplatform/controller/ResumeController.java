package com.internshipplatform.controller;

import com.internshipplatform.dto.ApiResponse;
import com.internshipplatform.dto.ResumeResponse;
import com.internshipplatform.service.ResumeService;
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
import java.util.List;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<ResumeResponse>> uploadResume(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description) throws IOException {
        ResumeResponse response = resumeService.uploadResume(userDetails.getUsername(), file, description);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Resume uploaded successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ResumeResponse>>> getUserResumes(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ResumeResponse> resumes = resumeService.getUserResumes(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Resumes retrieved", resumes));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<ResumeResponse>> getActiveResume(
            @AuthenticationPrincipal UserDetails userDetails) {
        ResumeResponse resume = resumeService.getActiveResume(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Active resume retrieved", resume));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResumeResponse>> getResumeById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        ResumeResponse resume = resumeService.getResumeById(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("Resume retrieved", resume));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadResume(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        ResumeResponse resume = resumeService.getResumeById(userDetails.getUsername(), id);
        byte[] fileData = resumeService.downloadResume(userDetails.getUsername(), id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resume.getOriginalFileName() + "\"")
                .contentType(MediaType.parseMediaType(resume.getContentType()))
                .contentLength(resume.getFileSize())
                .body(fileData);
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<ResumeResponse>> activateResume(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        ResumeResponse response = resumeService.activateResume(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("Resume activated", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteResume(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        resumeService.deleteResume(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("Resume deleted successfully"));
    }
}
