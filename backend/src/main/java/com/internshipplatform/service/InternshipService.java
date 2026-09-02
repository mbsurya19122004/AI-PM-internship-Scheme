package com.internshipplatform.service;

import com.internshipplatform.dto.InternshipDto;
import com.internshipplatform.entity.Internship;
import com.internshipplatform.repository.InternshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternshipService {

    private final InternshipRepository internshipRepository;

    @Transactional(readOnly = true)
    public List<InternshipDto.Response> getAllInternships() {
        return internshipRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InternshipDto.Response getInternshipById(Long id) {
        Internship internship = internshipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Internship not found with id: " + id));
        return mapToResponse(internship);
    }

    @Transactional
    public InternshipDto.Response createInternship(InternshipDto.Request request) {
        Internship internship = Internship.builder()
                .title(request.getTitle().trim())
                .company(request.getCompany().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .applicationLink(request.getApplicationLink().trim())
                .build();

        internship = internshipRepository.save(internship);
        log.info("Internship created: {} at {}", internship.getTitle(), internship.getCompany());

        return mapToResponse(internship);
    }

    private InternshipDto.Response mapToResponse(Internship internship) {
        return InternshipDto.Response.builder()
                .id(internship.getId())
                .title(internship.getTitle())
                .company(internship.getCompany())
                .description(internship.getDescription())
                .applicationLink(internship.getApplicationLink())
                .createdAt(internship.getCreatedAt())
                .updatedAt(internship.getUpdatedAt())
                .build();
    }
}
