package com.hireconnect.interview.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RescheduleInterviewRequest {

    @NotNull(message = "New scheduled date/time is required")
    @Future(message = "New interview time must be in the future")
    private LocalDateTime newScheduledAt;

    private String rescheduleReason;
    private String meetLink;
    private String location;
}
