package com.hireconnect.application.dto.request;

import com.hireconnect.application.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusRequest {

    @NotNull(message = "Status is required")
    private ApplicationStatus status;

    private String rejectionReason;
}
