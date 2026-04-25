package com.job.dto.request;


import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

import com.job.exception.ValidSalary;

@Data
@ValidSalary
public class JobRequest {

    @NotBlank(message = "Job title is required")
    private String title;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Job type is required")
    private String type;

    @NotBlank(message = "Location is required")
    private String location;

    @PositiveOrZero(message = "Minimum salary must be >= 0")
    private double salaryMin;

    @Positive(message = "Maximum salary must be > 0")
    private double salaryMax;

    @NotBlank(message = "Description is required")
    private String description;

    private List<@NotBlank(message = "Skill cannot be empty") String> skills;

    @Min(value = 0, message = "Experience cannot be negative")
    private int experienceRequired;

    @NotNull(message = "Recruiter ID is required")
    private Long postedBy;
}