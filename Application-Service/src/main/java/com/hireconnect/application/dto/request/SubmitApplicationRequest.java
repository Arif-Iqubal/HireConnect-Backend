package com.hireconnect.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class SubmitApplicationRequest {

    @NotNull(message = "Job ID is required")
    @Positive(message = "Job ID must be positive")
    private Long jobId;

    @NotNull(message = "Recruiter ID is required")
    @Positive(message = "Recruiter ID must be positive")
    private Long recruiterId;

    @NotBlank(message = "Job title is required")
    private String jobTitle;

    @NotBlank(message = "Company name is required")
    private String companyName;

    private String coverLetter;

    @NotBlank(message = "Resume URL is required")
    private String resumeUrl;
}
