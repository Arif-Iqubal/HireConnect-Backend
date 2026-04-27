package com.hireconnect.notification.service;

import com.hireconnect.notification.event.NotificationEventListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventListener Tests")
class NotificationEventListenerTest {

    @Mock private NotificationService notificationService;

    @InjectMocks private NotificationEventListener eventListener;

    private Map<String, Object> buildEvent(String eventType) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("applicationId", 100L);
        event.put("candidateId", 1L);
        event.put("candidateEmail", "alice@example.com");
        event.put("candidateName", "Alice Smith");
        event.put("jobId", 10L);
        event.put("jobTitle", "Software Engineer");
        event.put("companyName", "TechCorp");
        event.put("recruiterId", 2L);
        event.put("interviewId", 200L);
        event.put("scheduledAt", "2026-06-01T10:00:00");
        event.put("mode", "ONLINE");
        event.put("status", "SCHEDULED");
        return event;
    }

    @Test
    @DisplayName("APPLICATION_SUBMITTED should notify both candidate and recruiter")
    void shouldNotifyBothOnApplicationSubmitted() {
        eventListener.handleNotificationEvent(buildEvent("APPLICATION_SUBMITTED"));

        // Candidate notification
        verify(notificationService).sendNotification(
                eq(1L), eq("alice@example.com"),
                eq("APPLICATION_SUBMITTED"),
                anyString(), anyString(),
                eq(100L), eq("APPLICATION"), anyString());

        // Recruiter notification
        verify(notificationService).sendNotification(
                eq(2L), isNull(),
                eq("NEW_APPLICATION"),
                anyString(), anyString(),
                eq(100L), eq("APPLICATION"), anyString());
    }

    @Test
    @DisplayName("APPLICATION_STATUS_CHANGED should notify candidate")
    void shouldNotifyCandidateOnStatusChange() {
        Map<String, Object> event = buildEvent("APPLICATION_STATUS_CHANGED");
        event.put("oldStatus", "APPLIED");
        event.put("newStatus", "SHORTLISTED");

        eventListener.handleNotificationEvent(event);

        verify(notificationService).sendNotification(
                eq(1L), eq("alice@example.com"),
                eq("APPLICATION_STATUS_CHANGED"),
                contains("Shortlisted"),
                contains("shortlisted"),
                eq(100L), eq("APPLICATION"), anyString());
    }

    @Test
    @DisplayName("INTERVIEW_SCHEDULED should notify candidate")
    void shouldNotifyCandidateOnInterviewScheduled() {
        eventListener.handleNotificationEvent(buildEvent("INTERVIEW_SCHEDULED"));

        verify(notificationService).sendNotification(
                eq(1L), eq("alice@example.com"),
                eq("INTERVIEW_SCHEDULED"),
                anyString(), anyString(),
                eq(200L), eq("INTERVIEW"), anyString());
    }

    @Test
    @DisplayName("INTERVIEW_CONFIRMED should notify recruiter")
    void shouldNotifyRecruiterOnInterviewConfirmed() {
        eventListener.handleNotificationEvent(buildEvent("INTERVIEW_CONFIRMED"));

        verify(notificationService).sendNotification(
                eq(2L), isNull(),
                eq("INTERVIEW_CONFIRMED"),
                anyString(), anyString(),
                eq(200L), eq("INTERVIEW"), anyString());
    }

    @Test
    @DisplayName("INTERVIEW_RESCHEDULED should notify both candidate and recruiter")
    void shouldNotifyBothOnInterviewRescheduled() {
        eventListener.handleNotificationEvent(buildEvent("INTERVIEW_RESCHEDULED"));

        // Candidate
        verify(notificationService).sendNotification(
                eq(1L), eq("alice@example.com"),
                eq("INTERVIEW_RESCHEDULED"),
                anyString(), anyString(),
                eq(200L), eq("INTERVIEW"), anyString());

        // Recruiter
        verify(notificationService).sendNotification(
                eq(2L), isNull(),
                eq("INTERVIEW_RESCHEDULED"),
                anyString(), anyString(),
                eq(200L), eq("INTERVIEW"), anyString());
    }

    @Test
    @DisplayName("INTERVIEW_CANCELLED should notify both candidate and recruiter")
    void shouldNotifyBothOnInterviewCancelled() {
        eventListener.handleNotificationEvent(buildEvent("INTERVIEW_CANCELLED"));

        verify(notificationService, times(2)).sendNotification(
                anyLong(), any(), eq("INTERVIEW_CANCELLED"),
                anyString(), anyString(), anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("Unknown event type should be silently ignored")
    void shouldIgnoreUnknownEventType() {
        eventListener.handleNotificationEvent(buildEvent("UNKNOWN_EVENT_TYPE"));

        verify(notificationService, never()).sendNotification(
                anyLong(), any(), anyString(), anyString(), anyString(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Null event fields should be handled gracefully without exception")
    void shouldHandleNullEventFieldsGracefully() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "APPLICATION_SUBMITTED");
        event.put("candidateId", 1L);
        // Missing candidateEmail, recruiterId etc.

        // Should not throw even with partial data
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                eventListener.handleNotificationEvent(event));
    }
}
