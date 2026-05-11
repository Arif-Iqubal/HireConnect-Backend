package com.hireconnect.analytics.event;

import com.hireconnect.analytics.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AnalyticsEventListenerTest {

    @Mock private AnalyticsService analyticsService;
    @InjectMocks private AnalyticsEventListener listener;

    @Test
    void handlesApplicationSubmittedEvents() {
        listener.handleAnalyticsEvent(Map.of(
                "eventType", "APPLICATION_SUBMITTED",
                "applicationId", "100",
                "jobId", 10,
                "recruiterId", 2L,
                "candidateId", 1L));

        verify(analyticsService).recordApplicationEvent(
                100L, 10L, 2L, 1L, "APPLICATION_SUBMITTED", null, "APPLIED");
    }

    @Test
    void handlesApplicationStatusChangedEvents() {
        listener.handleAnalyticsEvent(Map.of(
                "eventType", "APPLICATION_STATUS_CHANGED",
                "applicationId", 100L,
                "jobId", 10L,
                "recruiterId", 2L,
                "candidateId", 1L,
                "oldStatus", "APPLIED",
                "newStatus", "SHORTLISTED"));

        verify(analyticsService).recordApplicationEvent(
                100L, 10L, 2L, 1L, "APPLICATION_STATUS_CHANGED", "APPLIED", "SHORTLISTED");
    }

    @Test
    void handlesJobViewedEventsAndMalformedIdsAsNulls() {
        listener.handleAnalyticsEvent(Map.of(
                "eventType", "JOB_VIEWED",
                "jobId", "bad-id",
                "recruiterId", 2,
                "viewerId", 1L,
                "viewerRole", "CANDIDATE"));

        verify(analyticsService).recordJobView(null, 2L, 1L, "CANDIDATE");
    }

    @Test
    void ignoresUnknownAndBadEvents() {
        listener.handleAnalyticsEvent(Map.of("eventType", "UNKNOWN"));
        verifyNoInteractions(analyticsService);

        listener.handleAnalyticsEvent(Map.of("eventType", "JOB_VIEWED", "viewerRole", 12));
        verify(analyticsService, never()).recordJobView(null, null, null, null);
    }
}
