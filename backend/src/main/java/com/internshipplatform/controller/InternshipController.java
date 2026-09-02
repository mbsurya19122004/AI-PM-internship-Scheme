package com.internshipplatform.controller;

import com.internshipplatform.dto.ApiResponse;
import com.internshipplatform.dto.InternshipDto;
import com.internshipplatform.service.InternshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internships")
@RequiredArgsConstructor
public class InternshipController {

    private final InternshipService internshipService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<InternshipDto.Response>>> getAllInternships() {
        List<InternshipDto.Response> internships = internshipService.getAllInternships();
        return ResponseEntity.ok(ApiResponse.success("Internships retrieved", internships));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InternshipDto.Response>> getInternshipById(@PathVariable Long id) {
        InternshipDto.Response internship = internshipService.getInternshipById(id);
        return ResponseEntity.ok(ApiResponse.success("Internship retrieved", internship));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InternshipDto.Response>> createInternship(
            @Valid @RequestBody InternshipDto.Request request) {
        InternshipDto.Response internship = internshipService.createInternship(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Internship created", internship));
    }
}
