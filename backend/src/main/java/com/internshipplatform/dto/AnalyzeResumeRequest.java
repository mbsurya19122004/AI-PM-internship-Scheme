package com.internshipplatform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyzeResumeRequest {

    @NotBlank(message = "Resume text is required")
    private String resumeText;
}
