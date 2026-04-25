package com.job.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long jobId;

    @Column(nullable = false)
    @NotBlank(message = "Title cannot be empty")
    private String title;

    private String category;

    private String type;

    @Column(nullable = false)
    @NotBlank(message = "Location is required")
    private String location;

    @PositiveOrZero(message = "Minimum salary must be >= 0")
    private double salaryMin;

    @PositiveOrZero(message = "Maximum salary must be >= 0")
    private double salaryMax;

    @Column(length = 2000)
    @NotBlank(message = "Description is required")
    private String description;

    @ElementCollection
    private List<String> skills;

    @Min(value = 0, message = "Experience cannot be negative")
    private int experienceRequired;

    @NotNull(message = "postedBy is required")
    private Long postedBy;

    private String status;

    private LocalDate postedAt;
}