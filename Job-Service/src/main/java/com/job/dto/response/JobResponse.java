package com.job.dto.response;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class JobResponse {

    private Long jobId;
    private String title;
    private String category;
    private String location;
    private double salaryMin;
    private double salaryMax;
    private List<String> skills;
    private int experienceRequired;
    private String status;
    private LocalDate postedAt;
}