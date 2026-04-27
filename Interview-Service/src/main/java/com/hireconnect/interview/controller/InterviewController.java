package com.hireconnect.interview.controller;

import com.hireconnect.interview.dto.request.RescheduleInterviewRequest;
import com.hireconnect.interview.dto.request.ScheduleInterviewRequest;
import com.hireconnect.interview.dto.response.ApiResponse;
import com.hireconnect.interview.dto.response.InterviewResponse;
import com.hireconnect.interview.enums.InterviewStatus;
import com.hireconnect.interview.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
@Slf4j
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<InterviewResponse>> scheduleInterview(
            @Valid @RequestBody ScheduleInterviewRequest request,
            @RequestHeader("X-User-Id") Long recruiterId) {
        InterviewResponse response = interviewService.scheduleInterview(recruiterId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Interview scheduled successfully", response));
    }

    @GetMapping("/{interviewId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<InterviewResponse>> getInterview(@PathVariable Long interviewId) {
        return ResponseEntity.ok(ApiResponse.success(interviewService.getInterviewById(interviewId)));
    }

    @PatchMapping("/{interviewId}/confirm")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<InterviewResponse>> confirmInterview(
            @PathVariable Long interviewId,
            @RequestHeader("X-User-Id") Long candidateId) {
        InterviewResponse response = interviewService.confirmInterview(interviewId, candidateId);
        return ResponseEntity.ok(ApiResponse.success("Interview confirmed successfully", response));
    }

    @PatchMapping("/{interviewId}/reschedule")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<InterviewResponse>> rescheduleInterview(
            @PathVariable Long interviewId,
            @Valid @RequestBody RescheduleInterviewRequest request,
            @RequestHeader("X-User-Id") Long requesterId) {
        InterviewResponse response = interviewService.rescheduleInterview(interviewId, request, requesterId);
        return ResponseEntity.ok(ApiResponse.success("Interview rescheduled successfully", response));
    }

    @PatchMapping("/{interviewId}/cancel")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<InterviewResponse>> cancelInterview(
            @PathVariable Long interviewId,
            @RequestParam(required = false) String reason,
            @RequestHeader("X-User-Id") Long requesterId) {
        InterviewResponse response = interviewService.cancelInterview(interviewId, requesterId, reason);
        return ResponseEntity.ok(ApiResponse.success("Interview cancelled successfully", response));
    }

    @PatchMapping("/{interviewId}/complete")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<InterviewResponse>> completeInterview(
            @PathVariable Long interviewId,
            @RequestHeader("X-User-Id") Long recruiterId) {
        InterviewResponse response = interviewService.completeInterview(interviewId, recruiterId);
        return ResponseEntity.ok(ApiResponse.success("Interview marked as completed", response));
    }

    @GetMapping("/application/{applicationId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<InterviewResponse>>> getByApplication(@PathVariable Long applicationId) {
        return ResponseEntity.ok(ApiResponse.success(interviewService.getInterviewsByApplication(applicationId)));
    }

    @GetMapping("/candidate/{candidateId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<InterviewResponse>>> getByCandidate(
            @PathVariable Long candidateId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("scheduledAt").descending());
        return ResponseEntity.ok(ApiResponse.success(interviewService.getInterviewsByCandidate(candidateId, pageable)));
    }

    @GetMapping("/candidate/{candidateId}/upcoming")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<InterviewResponse>>> getUpcomingByCandidate(@PathVariable Long candidateId) {
        return ResponseEntity.ok(ApiResponse.success(interviewService.getUpcomingInterviewsByCandidate(candidateId)));
    }

    @GetMapping("/recruiter/{recruiterId}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<InterviewResponse>>> getByRecruiter(
            @PathVariable Long recruiterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) InterviewStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("scheduledAt").descending());
        Page<InterviewResponse> result = status != null
                ? interviewService.getInterviewsByRecruiterAndStatus(recruiterId, status, pageable)
                : interviewService.getInterviewsByRecruiter(recruiterId, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/recruiter/{recruiterId}/upcoming")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<InterviewResponse>>> getUpcomingByRecruiter(@PathVariable Long recruiterId) {
        return ResponseEntity.ok(ApiResponse.success(interviewService.getUpcomingInterviewsByRecruiter(recruiterId)));
    }
}
