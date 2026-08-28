package com.internshipplatform.service;

import com.internshipplatform.dto.ResumeResponse;
import com.internshipplatform.entity.Resume;
import com.internshipplatform.entity.User;
import com.internshipplatform.repository.ResumeRepository;
import com.internshipplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_RESUMES_PER_USER = 10;
    private static final List<String> ALLOWED_TYPES = List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;

    @Transactional
    public ResumeResponse uploadResume(String email, MultipartFile file, String description) throws IOException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate file
        validateFile(file);

        // Check resume limit
        long resumeCount = resumeRepository.countByUserId(user.getId());
        if (resumeCount >= MAX_RESUMES_PER_USER) {
            throw new IllegalArgumentException("Maximum of " + MAX_RESUMES_PER_USER + " resumes per user exceeded");
        }

        Resume resume = Resume.builder()
                .fileName(generateFileName(user.getId(), file.getOriginalFilename()))
                .originalFileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .fileData(file.getBytes())
                .description(description)
                .active(resumeCount == 0) // First resume is automatically active
                .user(user)
                .build();

        resume = resumeRepository.save(resume);
        log.info("Resume uploaded: {} for user: {}", resume.getOriginalFileName(), email);

        return mapToResumeResponse(resume);
    }

    @Transactional(readOnly = true)
    public List<ResumeResponse> getUserResumes(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return resumeRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToResumeResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResumeResponse getResumeById(String email, Long resumeId) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, getUserId(email))
                .orElseThrow(() -> new RuntimeException("Resume not found"));
        return mapToResumeResponse(resume);
    }

    @Transactional
    public void deleteResume(String email, Long resumeId) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, getUserId(email))
                .orElseThrow(() -> new RuntimeException("Resume not found"));
        resumeRepository.delete(resume);
        log.info("Resume deleted: {} for user: {}", resumeId, email);
    }

    @Transactional
    public ResumeResponse activateResume(String email, Long resumeId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Resume resume = resumeRepository.findByIdAndUserId(resumeId, user.getId())
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        // Deactivate all other resumes for this user
        resumeRepository.deactivateAllByUserId(user.getId());

        // Activate the selected resume
        resumeRepository.activateResume(resumeId, user.getId());

        resume.setActive(true);
        log.info("Resume activated: {} for user: {}", resumeId, email);

        return mapToResumeResponse(resume);
    }

    @Transactional(readOnly = true)
    public byte[] downloadResume(String email, Long resumeId) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, getUserId(email))
                .orElseThrow(() -> new RuntimeException("Resume not found"));
        return resume.getFileData();
    }

    @Transactional(readOnly = true)
    public ResumeResponse getActiveResume(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return resumeRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .filter(Resume::isActive)
                .findFirst()
                .map(this::mapToResumeResponse)
                .orElseThrow(() -> new RuntimeException("No active resume found"));
    }

    private Long getUserId(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 10MB");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Only PDF and Word documents are allowed");
        }
    }

    private String generateFileName(Long userId, String originalFileName) {
        return "resume_" + userId + "_" + System.currentTimeMillis() + "_" + originalFileName;
    }

    private ResumeResponse mapToResumeResponse(Resume resume) {
        return ResumeResponse.builder()
                .id(resume.getId())
                .fileName(resume.getFileName())
                .originalFileName(resume.getOriginalFileName())
                .contentType(resume.getContentType())
                .fileSize(resume.getFileSize())
                .description(resume.getDescription())
                .active(resume.isActive())
                .createdAt(resume.getCreatedAt())
                .updatedAt(resume.getUpdatedAt())
                .build();
    }
}
