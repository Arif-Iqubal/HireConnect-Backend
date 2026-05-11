package com.hireconnect.analytics.controller;

import com.hireconnect.analytics.dto.response.ApiResponse;
import com.hireconnect.analytics.dto.response.PlatformAnalyticsResponse;
import com.hireconnect.analytics.dto.response.RecruiterAnalyticsResponse;
import com.hireconnect.analytics.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock private AnalyticsService analyticsService;
    @Mock private Authentication authentication;
    @InjectMocks private AnalyticsController controller;

    @Test
    void getRecruiterStatsUsesAuthenticatedUserId() {
        RecruiterAnalyticsResponse stats = RecruiterAnalyticsResponse.builder()
                .recruiterId(7L)
                .totalJobViews(20L)
                .build();
        when(authentication.getName()).thenReturn("7");
        when(analyticsService.getRecruiterStats(7L)).thenReturn(stats);

        ResponseEntity<ApiResponse<RecruiterAnalyticsResponse>> response = controller.getRecruiterStats(authentication);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).isEqualTo(stats);
        verify(analyticsService).getRecruiterStats(7L);
    }

    @Test
    void getPlatformStatsReturnsWrappedResponse() {
        PlatformAnalyticsResponse stats = PlatformAnalyticsResponse.builder()
                .totalJobViews(100L)
                .totalApplicationEvents(25L)
                .build();
        when(analyticsService.getPlatformStats()).thenReturn(stats);

        ResponseEntity<ApiResponse<PlatformAnalyticsResponse>> response = controller.getPlatformStats();

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo(stats);
        verify(analyticsService).getPlatformStats();
    }

    @Test
    void jobMetricEndpointsDelegateToService() {
        when(analyticsService.getJobViewCount(10L)).thenReturn(50L);
        when(analyticsService.getApplicationCountByJob(10L)).thenReturn(5L);
        when(analyticsService.getViewToApplyRatio(10L)).thenReturn(0.1);

        assertThat(controller.getJobViewCount(10L).getBody().getData()).isEqualTo(50L);
        assertThat(controller.getApplicationCount(10L).getBody().getData()).isEqualTo(5L);
        assertThat(controller.getViewToApplyRatio(10L).getBody().getData()).isEqualTo(0.1);

        verify(analyticsService).getJobViewCount(10L);
        verify(analyticsService).getApplicationCountByJob(10L);
        verify(analyticsService).getViewToApplyRatio(10L);
    }

    @Test
    void recordJobViewDelegatesAndReturnsSuccessMessage() {
        ResponseEntity<ApiResponse<Void>> response = controller.recordJobView(10L, 2L, 3L, "CANDIDATE");

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("View recorded");
        verify(analyticsService).recordJobView(10L, 2L, 3L, "CANDIDATE");
    }
}
