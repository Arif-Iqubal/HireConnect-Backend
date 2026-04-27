package com.hireconnect.interview.service.impl;

import com.hireconnect.interview.dto.request.RescheduleInterviewRequest;
import com.hireconnect.interview.dto.request.ScheduleInterviewRequest;
import com.hireconnect.interview.dto.response.InterviewResponse;
import com.hireconnect.interview.entity.Interview;
import com.hireconnect.interview.enums.InterviewStatus;
import com.hireconnect.interview.exception.ResourceNotFoundException;
import com.hireconnect.interview.mapper.InterviewMapper;
import com.hireconnect.interview.repository.InterviewRepository;
import com.hireconnect.interview.service.InterviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRepository interviewRepository;
    private final InterviewMapper interviewMapper;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key.notification}")
    private String notificationRoutingKey;

    @Override
    public InterviewResponse scheduleInterview(Long recruiterId, ScheduleInterviewRequest request) {
        log.info("Recruiter {} scheduling interview for application {}", recruiterId, request.getApplicationId());

        Interview interview = Interview.builder()
                .applicationId(request.getApplicationId())
                .candidateId(request.getCandidateId())
                .recruiterId(recruiterId)
                .jobId(request.getJobId())
                .jobTitle(request.getJobTitle())
                .candidateName(request.getCandidateName())
                .candidateEmail(request.getCandidateEmail())
                .companyName(request.getCompanyName())
                .scheduledAt(request.getScheduledAt())
                .mode(request.getMode())
                .durationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 60)
                .meetLink(request.getMeetLink())
                .location(request.getLocation())
                .notes(request.getNotes())
                .interviewerName(request.getInterviewerName())
                .roundNumber(request.getRoundNumber() != null ? request.getRoundNumber() : 1)
                .status(InterviewStatus.SCHEDULED)
                .build();

        Interview saved = interviewRepository.save(interview);
        log.info("Interview {} scheduled for candidate {} at {}", saved.getInterviewId(),
                request.getCandidateId(), request.getScheduledAt());

        publishInterviewEvent("INTERVIEW_SCHEDULED", saved);
        return interviewMapper.toResponse(saved);
    }

    @Override
    public InterviewResponse confirmInterview(Long interviewId, Long candidateId) {
        Interview interview = findById(interviewId);

        if (!interview.getCandidateId().equals(candidateId)) {
            throw new AccessDeniedException("Only the candidate can confirm this interview");
        }

        if (interview.getStatus() != InterviewStatus.SCHEDULED &&
                interview.getStatus() != InterviewStatus.RESCHEDULED) {
            throw new IllegalStateException("Interview cannot be confirmed in status: " + interview.getStatus());
        }

        interview.setStatus(InterviewStatus.CONFIRMED);
        Interview updated = interviewRepository.save(interview);

        publishInterviewEvent("INTERVIEW_CONFIRMED", updated);
        return interviewMapper.toResponse(updated);
    }

    @Override
    public InterviewResponse rescheduleInterview(Long interviewId, RescheduleInterviewRequest request, Long requesterId) {
        Interview interview = findById(interviewId);

        boolean isCandidate = interview.getCandidateId().equals(requesterId);
        boolean isRecruiter = interview.getRecruiterId().equals(requesterId);

        if (!isCandidate && !isRecruiter) {
            throw new AccessDeniedException("You are not authorized to reschedule this interview");
        }

        if (interview.getStatus() == InterviewStatus.CANCELLED ||
                interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new IllegalStateException("Cannot reschedule interview with status: " + interview.getStatus());
        }

        interview.setScheduledAt(request.getNewScheduledAt());
        interview.setStatus(InterviewStatus.RESCHEDULED);
        interview.setRescheduleReason(request.getRescheduleReason());
        if (request.getMeetLink() != null) interview.setMeetLink(request.getMeetLink());
        if (request.getLocation() != null) interview.setLocation(request.getLocation());

        Interview updated = interviewRepository.save(interview);
        publishInterviewEvent("INTERVIEW_RESCHEDULED", updated);
        return interviewMapper.toResponse(updated);
    }

    @Override
    public InterviewResponse cancelInterview(Long interviewId, Long requesterId, String reason) {
        Interview interview = findById(interviewId);

        boolean isCandidate = interview.getCandidateId().equals(requesterId);
        boolean isRecruiter = interview.getRecruiterId().equals(requesterId);

        if (!isCandidate && !isRecruiter) {
            throw new AccessDeniedException("You are not authorized to cancel this interview");
        }

        if (interview.getStatus() == InterviewStatus.COMPLETED ||
                interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new IllegalStateException("Interview is already " + interview.getStatus());
        }

        interview.setStatus(InterviewStatus.CANCELLED);
        interview.setCancellationReason(reason);

        Interview updated = interviewRepository.save(interview);
        publishInterviewEvent("INTERVIEW_CANCELLED", updated);
        return interviewMapper.toResponse(updated);
    }

    @Override
    public InterviewResponse completeInterview(Long interviewId, Long recruiterId) {
        Interview interview = findById(interviewId);

        if (!interview.getRecruiterId().equals(recruiterId)) {
            throw new AccessDeniedException("Only the recruiter can mark interview as completed");
        }

        if (interview.getStatus() != InterviewStatus.CONFIRMED &&
                interview.getStatus() != InterviewStatus.SCHEDULED) {
            throw new IllegalStateException("Interview cannot be completed in status: " + interview.getStatus());
        }

        interview.setStatus(InterviewStatus.COMPLETED);
        Interview updated = interviewRepository.save(interview);
        return interviewMapper.toResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewResponse getInterviewById(Long interviewId) {
        return interviewMapper.toResponse(findById(interviewId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewResponse> getInterviewsByApplication(Long applicationId) {
        return interviewRepository.findByApplicationId(applicationId)
                .stream().map(interviewMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InterviewResponse> getInterviewsByCandidate(Long candidateId, Pageable pageable) {
        return interviewRepository.findByCandidateId(candidateId, pageable)
                .map(interviewMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewResponse> getUpcomingInterviewsByCandidate(Long candidateId) {
        return interviewRepository.findUpcomingByCandidate(candidateId, LocalDateTime.now())
                .stream().map(interviewMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InterviewResponse> getInterviewsByRecruiter(Long recruiterId, Pageable pageable) {
        return interviewRepository.findByRecruiterId(recruiterId, pageable)
                .map(interviewMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewResponse> getUpcomingInterviewsByRecruiter(Long recruiterId) {
        return interviewRepository.findUpcomingByRecruiter(recruiterId, LocalDateTime.now())
                .stream().map(interviewMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InterviewResponse> getInterviewsByRecruiterAndStatus(Long recruiterId, InterviewStatus status, Pageable pageable) {
        return interviewRepository.findByRecruiterIdAndStatus(recruiterId, status, pageable)
                .map(interviewMapper::toResponse);
    }

    private Interview findById(Long interviewId) {
        return interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", interviewId));
    }

    private void publishInterviewEvent(String eventType, Interview interview) {
        try {
            Map<String, Object> event = Map.of(
                "eventType", eventType,
                "interviewId", interview.getInterviewId(),
                "candidateId", interview.getCandidateId(),
                "candidateEmail", interview.getCandidateEmail() != null ? interview.getCandidateEmail() : "",
                "candidateName", interview.getCandidateName() != null ? interview.getCandidateName() : "",
                "jobTitle", interview.getJobTitle() != null ? interview.getJobTitle() : "",
                "companyName", interview.getCompanyName() != null ? interview.getCompanyName() : "",
                "scheduledAt", interview.getScheduledAt().toString(),
                "mode", interview.getMode().name(),
                "status", interview.getStatus().name()
            );
            rabbitTemplate.convertAndSend(exchange, notificationRoutingKey, event);
        } catch (Exception e) {
            log.error("Failed to publish interview event {}: {}", eventType, e.getMessage());
        }
    }
}
