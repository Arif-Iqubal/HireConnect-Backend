package com.hireconnect.interview.controller;

import com.hireconnect.interview.dto.request.RescheduleInterviewRequest;
import com.hireconnect.interview.dto.request.ScheduleInterviewRequest;
import com.hireconnect.interview.dto.response.InterviewResponse;
import com.hireconnect.interview.enums.InterviewStatus;
import com.hireconnect.interview.service.InterviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewControllerTest {

    @Mock private InterviewService interviewService;
    @InjectMocks private InterviewController controller;

    private final InterviewResponse interview = InterviewResponse.builder()
            .interviewId(1L)
            .candidateId(2L)
            .recruiterId(7L)
            .status(InterviewStatus.SCHEDULED)
            .build();

    @Test
    void candidateAndRecruiterMutationEndpointsDelegate() {
        ScheduleInterviewRequest schedule = new ScheduleInterviewRequest();
        RescheduleInterviewRequest reschedule = new RescheduleInterviewRequest();
        when(interviewService.scheduleInterview(7L, schedule)).thenReturn(interview);
        when(interviewService.confirmInterview(1L, 2L)).thenReturn(interview);
        when(interviewService.rescheduleInterview(1L, reschedule, 7L)).thenReturn(interview);
        when(interviewService.rejectReschedule(1L, 7L, "No")).thenReturn(interview);
        when(interviewService.cancelInterview(1L, 2L, "Busy")).thenReturn(interview);
        when(interviewService.completeInterview(1L, 7L)).thenReturn(interview);

        assertThat(controller.scheduleInterview(schedule, 7L).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.confirmInterview(1L, 2L).getBody().getMessage()).isEqualTo("Interview confirmed successfully");
        assertThat(controller.rescheduleInterview(1L, reschedule, 7L).getBody().getData()).isEqualTo(interview);
        assertThat(controller.rejectReschedule(1L, "No", 7L).getBody().getData()).isEqualTo(interview);
        assertThat(controller.cancelInterview(1L, "Busy", 2L).getBody().getData()).isEqualTo(interview);
        assertThat(controller.completeInterview(1L, 7L).getBody().getMessage()).isEqualTo("Interview marked as completed");
    }

    @Test
    void queryEndpointsDelegateWithPagingAndStatus() {
        when(interviewService.getInterviewById(1L)).thenReturn(interview);
        when(interviewService.getInterviewsByApplication(10L)).thenReturn(List.of(interview));
        when(interviewService.getInterviewsByCandidate(eq(2L), any())).thenReturn(new PageImpl<>(List.of(interview)));
        when(interviewService.getUpcomingInterviewsByCandidate(2L)).thenReturn(List.of(interview));
        when(interviewService.getInterviewsByRecruiter(eq(7L), any())).thenReturn(new PageImpl<>(List.of(interview)));
        when(interviewService.getInterviewsByRecruiterAndStatus(eq(7L), eq(InterviewStatus.SCHEDULED), any()))
                .thenReturn(new PageImpl<>(List.of(interview)));
        when(interviewService.getUpcomingInterviewsByRecruiter(7L)).thenReturn(List.of(interview));

        assertThat(controller.getInterview(1L).getBody().getData()).isEqualTo(interview);
        assertThat(controller.getByApplication(10L).getBody().getData()).containsExactly(interview);
        assertThat(controller.getByCandidate(2L, 0, 10).getBody().getData().getContent()).containsExactly(interview);
        assertThat(controller.getUpcomingByCandidate(2L).getBody().getData()).containsExactly(interview);
        assertThat(controller.getByRecruiter(7L, 0, 10, null).getBody().getData().getContent()).containsExactly(interview);
        assertThat(controller.getByRecruiter(7L, 0, 10, InterviewStatus.SCHEDULED).getBody().getData().getContent()).containsExactly(interview);
        assertThat(controller.getUpcomingByRecruiter(7L).getBody().getData()).containsExactly(interview);
    }

    @Test
    void myAndRequestRescheduleResolveUserIdFromHeaderOrAuthentication() {
        var auth = new TestingAuthenticationToken("2", "n/a");
        when(interviewService.getInterviewsByCandidate(eq(2L), any())).thenReturn(new PageImpl<>(List.of(interview)));
        when(interviewService.requestReschedule(eq(1L), eq(2L), any())).thenReturn(interview);

        assertThat(controller.getMyInterviews(null, auth).getBody().getData()).containsExactly(interview);
        assertThat(controller.getMyInterviews(2L, auth).getBody().getData()).containsExactly(interview);
        assertThat(controller.requestReschedule(1L, null, LocalDateTime.now().plusDays(1).toString(), "Later", null, null, auth)
                .getBody().getData()).isEqualTo(interview);
    }

    @Test
    void requestRescheduleRejectsInvalidIdentityAndDate() {
        assertThatThrownBy(() -> controller.getMyInterviews(null, new TestingAuthenticationToken("abc", "n/a")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Candidate identity is invalid");
        assertThatThrownBy(() -> controller.requestReschedule(1L, null, "not-a-date", null, null, 2L, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Invalid date/time format");
    }
}
