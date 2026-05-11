package com.hireconnect.job.controller;

import com.hireconnect.job.dto.request.JobRequest;
import com.hireconnect.job.dto.response.JobResponse;
import com.hireconnect.job.enums.JobStatus;
import com.hireconnect.job.enums.JobType;
import com.hireconnect.job.service.JobService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobControllerTest {

    @Mock private JobService jobService;
    @InjectMocks private JobController controller;

    private final JobResponse job = JobResponse.builder()
            .jobId(10L)
            .title("Developer")
            .jobType(JobType.FULL_TIME)
            .status(JobStatus.ACTIVE)
            .postedBy(7L)
            .build();

    @Test
    void publicBrowseSearchAndAdminListDelegateWithPaging() {
        when(jobService.getAllActiveJobs(any())).thenReturn(new PageImpl<>(List.of(job)));
        when(jobService.searchJobs(eq("dev"), eq("Remote"), isNull(), eq(JobType.FULL_TIME), eq(2), eq(100.0), eq(200.0), any()))
                .thenReturn(new PageImpl<>(List.of(job)));
        when(jobService.getAllJobs(any(), eq(JobStatus.ACTIVE))).thenReturn(new PageImpl<>(List.of(job)));

        assertThat(controller.getAllActiveJobs(0, 20, "postedAt", "desc").getBody().getData().getContent()).containsExactly(job);
        assertThat(controller.searchJobs("dev", "Remote", null, JobType.FULL_TIME, 2, 100.0, 200.0, 0, 10).getBody().getData().getContent()).containsExactly(job);
        assertThat(controller.getAllJobsForAdmin(0, 50, "createdAt", "asc", JobStatus.ACTIVE).getBody().getData().getContent()).containsExactly(job);
    }

    @Test
    void recruiterEndpointsUseAuthenticationName() {
        var auth = new TestingAuthenticationToken("7", "n/a", "ROLE_RECRUITER");
        JobRequest request = new JobRequest();
        when(jobService.createJob(7L, null, request)).thenReturn(job);
        when(jobService.getJobsByRecruiter(eq(7L), any())).thenReturn(new PageImpl<>(List.of(job)));
        when(jobService.getJobsByRecruiterAndStatus(eq(7L), eq(JobStatus.ACTIVE), any())).thenReturn(new PageImpl<>(List.of(job)));
        when(jobService.getAllJobsByRecruiter(7L)).thenReturn(List.of(job));
        when(jobService.updateJob(10L, 7L, request)).thenReturn(job);

        assertThat(controller.createJob(request, auth).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.getJobsByRecruiter(auth, 0, 10, null).getBody().getData().getContent()).containsExactly(job);
        assertThat(controller.getJobsByRecruiter(auth, 0, 10, JobStatus.ACTIVE).getBody().getData().getContent()).containsExactly(job);
        assertThat(controller.getAllJobsByRecruiter(7L).getBody().getData()).containsExactly(job);
        assertThat(controller.updateJob(10L, request, auth).getBody().getData()).isEqualTo(job);
    }

    @Test
    void statusAndDeleteEndpointsUseAdminBypassOrRecruiterOwnership() {
        var admin = new TestingAuthenticationToken("99", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        var recruiter = new TestingAuthenticationToken("7", "n/a", List.of(new SimpleGrantedAuthority("ROLE_RECRUITER")));
        when(jobService.updateJobStatusAsAdmin(10L, JobStatus.PAUSED)).thenReturn(job);
        when(jobService.updateJobStatusAsAdmin(10L, JobStatus.CLOSED)).thenReturn(job);
        when(jobService.updateJobStatus(10L, 7L, JobStatus.PAUSED)).thenReturn(job);
        when(jobService.updateJobStatus(10L, 7L, JobStatus.CLOSED)).thenReturn(job);

        assertThat(controller.updateJobStatus(10L, JobStatus.PAUSED, admin).getBody().getData()).isEqualTo(job);
        assertThat(controller.updateJobStatus(10L, JobStatus.PAUSED, recruiter).getBody().getData()).isEqualTo(job);
        assertThat(controller.closeJob(10L, admin).getBody().getMessage()).isEqualTo("Job closed successfully");
        assertThat(controller.closeJob(10L, recruiter).getBody().getMessage()).isEqualTo("Job closed successfully");
        assertThat(controller.pauseJob(10L, admin).getBody().getMessage()).isEqualTo("Job paused successfully");
        assertThat(controller.pauseJob(10L, recruiter).getBody().getMessage()).isEqualTo("Job paused successfully");

        controller.deleteJob(10L, admin);
        controller.deleteJob(10L, recruiter);

        verify(jobService).deleteJobAsAdmin(10L);
        verify(jobService).deleteJob(10L, 7L);
    }

    @Test
    void statusEndpointsRejectMissingAuthentication() {
        assertThatThrownBy(() -> controller.updateJobStatus(10L, JobStatus.PAUSED, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Authentication is required");
        assertThatThrownBy(() -> controller.closeJob(10L, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Authentication is required");
        assertThatThrownBy(() -> controller.pauseJob(10L, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Authentication is required");
    }

    @Test
    void detailAndInternalEndpointsDelegate() {
        when(jobService.getJobById(10L, 1L, "CANDIDATE")).thenReturn(job);
        when(jobService.jobExists(10L)).thenReturn(true);
        when(jobService.getRecruiterIdByJob(10L)).thenReturn(7L);
        when(jobService.countJobsByRecruiter(7L)).thenReturn(3L);

        assertThat(controller.getJobById(10L, 1L, "CANDIDATE").getBody().getData()).isEqualTo(job);
        assertThat(controller.jobExists(10L).getBody().getData()).isTrue();
        assertThat(controller.getRecruiterByJob(10L).getBody().getData()).isEqualTo(7L);
        assertThat(controller.countJobsByRecruiter(7L).getBody().getData()).isEqualTo(3L);
    }
}
