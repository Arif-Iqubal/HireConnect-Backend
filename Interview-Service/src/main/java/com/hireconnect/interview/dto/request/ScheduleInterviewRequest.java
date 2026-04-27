package com.hireconnect.interview.dto.request;

import com.hireconnect.interview.enums.InterviewMode;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduleInterviewRequest {

    @NotNull(message = "Application ID is required")
    @Positive(message = "Application ID must be positive")
    private Long applicationId;

    @NotNull(message = "Candidate ID is required")
    private Long candidateId;

    @NotNull(message = "Job ID is required")
    private Long jobId;

    private String jobTitle;
    private String candidateName;
    private String candidateEmail;
    private String companyName;

    @NotNull(message = "Scheduled date/time is required")
    @Future(message = "Interview must be scheduled in the future")
    private LocalDateTime scheduledAt;

    @NotNull(message = "Interview mode is required")
    private InterviewMode mode;

    private Integer durationMinutes = 60;
    private String meetLink;
    private String location;
    private String notes;
    private String interviewerName;
    private Integer roundNumber = 1;
}
