package com.hireconnect.job.service;

import com.hireconnect.job.client.ProfileServiceClient;
import com.hireconnect.job.client.SubscriptionServiceClient;
import com.hireconnect.job.dto.request.JobRequest;
import com.hireconnect.job.dto.response.ApiResponse;
import com.hireconnect.job.dto.response.CandidateNotificationRecipientResponse;
import com.hireconnect.job.dto.response.JobResponse;
import com.hireconnect.job.entity.Job;
import com.hireconnect.job.enums.JobStatus;
import com.hireconnect.job.enums.JobType;
import com.hireconnect.job.exception.ResourceNotFoundException;
import com.hireconnect.job.repository.JobRepository;
import com.hireconnect.job.service.impl.JobServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobServiceImpl Tests")
class JobServiceImplTest {

    @Mock private JobRepository jobRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private ProfileServiceClient profileServiceClient;
    @Mock private SubscriptionServiceClient subscriptionServiceClient;

    @InjectMocks private JobServiceImpl jobService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jobService, "exchange", "hireconnect.exchange");
        ReflectionTestUtils.setField(jobService, "analyticsRoutingKey", "analytics.routing.key");
        ReflectionTestUtils.setField(jobService, "notificationRoutingKey", "notification.routing.key");
    }

    private Job buildJob(Long id, Long recruiterId, JobStatus status) {
        return Job.builder()
                .jobId(id)
                .title("Backend Developer")
                .category("Engineering")
                .jobType(JobType.FULL_TIME)
                .location("Bangalore")
                .salaryMin(80000.0)
                .salaryMax(120000.0)
                .description("We are looking for a skilled backend developer with 3+ years of experience.")
                .skills(List.of("Java", "Spring Boot", "MySQL"))
                .experienceRequired(3)
                .vacancies(2)
                .postedBy(recruiterId)
                .companyName("TechCorp")
                .status(status)
                .expiresAt(LocalDate.now().plusDays(30))
                .viewCount(0L)
                .isRemote(false)
                .build();
    }

    private JobRequest buildRequest() {
        JobRequest r = new JobRequest();
        r.setTitle("Backend Developer");
        r.setCategory("Engineering");
        r.setJobType(JobType.FULL_TIME);
        r.setLocation("Bangalore");
        r.setSalaryMin(80000.0);
        r.setSalaryMax(120000.0);
        r.setDescription("We are looking for a skilled backend developer with 3+ years of experience.");
        r.setSkills(List.of("Java", "Spring Boot", "MySQL"));
        r.setExperienceRequired(3);
        r.setVacancies(2);
        r.setCompanyName("TechCorp");
        r.setIsRemote(false);
        r.setExpiresAt(LocalDate.now().plusDays(30));
        return r;
    }

    @Nested
    @DisplayName("createJob()")
    class CreateJobTests {

        @Test
        @DisplayName("should create job successfully")
        void shouldCreateJobSuccessfully() {
            Job saved = buildJob(1L, 2L, JobStatus.ACTIVE);
            when(subscriptionServiceClient.getMaxJobPosts(2L)).thenReturn(ApiResponse.success(3));
            when(jobRepository.countByPostedByAndStatus(2L, JobStatus.ACTIVE)).thenReturn(0L);
            when(jobRepository.save(any(Job.class))).thenReturn(saved);

            JobResponse result = jobService.createJob(2L, "TechCorp", buildRequest());

            assertThat(result).isNotNull();
            assertThat(result.getJobId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("Backend Developer");
            assertThat(result.getStatus()).isEqualTo(JobStatus.ACTIVE);
            assertThat(result.getPostedBy()).isEqualTo(2L);
            verify(jobRepository).save(any(Job.class));
        }

        @Test
        @DisplayName("should create draft with defaults without publish validation")
        void shouldCreateDraftWithDefaults() {
            JobRequest request = new JobRequest();
            request.setStatus(JobStatus.DRAFT);

            Job saved = buildJob(3L, 2L, JobStatus.DRAFT);
            saved.setTitle("Untitled Draft");
            saved.setCategory("Uncategorized");
            saved.setLocation("Not specified");
            saved.setSalaryMin(0.0);
            saved.setSalaryMax(0.0);
            saved.setDescription("Draft job description pending.");
            saved.setSkills(List.of());
            saved.setVacancies(1);
            saved.setCompanyName("Fallback Corp");
            when(jobRepository.save(any(Job.class))).thenReturn(saved);

            JobResponse result = jobService.createJob(2L, "Fallback Corp", request);

            assertThat(result.getStatus()).isEqualTo(JobStatus.DRAFT);
            assertThat(result.getTitle()).isEqualTo("Untitled Draft");
            verify(subscriptionServiceClient, never()).getMaxJobPosts(anyLong());
            verify(rabbitTemplate, never()).convertAndSend(eq("hireconnect.exchange"), eq("notification.routing.key"), any(Object.class));
        }

        @Test
        @DisplayName("should publish job alerts for valid recipients")
        void shouldPublishJobAlertsForValidRecipients() {
            Job saved = buildJob(1L, 2L, JobStatus.ACTIVE);
            CandidateNotificationRecipientResponse valid = new CandidateNotificationRecipientResponse();
            valid.setUserId(5L);
            valid.setEmail("candidate@example.com");
            valid.setFullName("Candidate One");
            CandidateNotificationRecipientResponse missingEmail = new CandidateNotificationRecipientResponse();
            missingEmail.setUserId(6L);

            when(subscriptionServiceClient.getMaxJobPosts(2L)).thenReturn(ApiResponse.success(3));
            when(jobRepository.countByPostedByAndStatus(2L, JobStatus.ACTIVE)).thenReturn(0L);
            when(profileServiceClient.getCandidateNotificationRecipients())
                    .thenReturn(ApiResponse.success(List.of(valid, missingEmail)));
            when(jobRepository.save(any(Job.class))).thenReturn(saved);

            jobService.createJob(2L, "TechCorp", buildRequest());

            verify(rabbitTemplate).convertAndSend(eq("hireconnect.exchange"), eq("notification.routing.key"), any(Object.class));
        }

        @Test
        @DisplayName("should reject active job when subscription limit is reached")
        void shouldRejectWhenSubscriptionLimitReached() {
            when(subscriptionServiceClient.getMaxJobPosts(2L)).thenReturn(ApiResponse.success(1));
            when(jobRepository.countByPostedByAndStatus(2L, JobStatus.ACTIVE)).thenReturn(1L);

            assertThatThrownBy(() -> jobService.createJob(2L, "TechCorp", buildRequest()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Job post limit reached");
            verify(jobRepository, never()).save(any());
        }

        @Test
        @DisplayName("should reject incomplete active job")
        void shouldRejectIncompleteActiveJob() {
            JobRequest request = buildRequest();
            request.setTitle(" ");

            assertThatThrownBy(() -> jobService.createJob(2L, "TechCorp", request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("complete all required job fields");
        }
    }

    @Nested
    @DisplayName("getJobById()")
    class GetJobByIdTests {

        @Test
        @DisplayName("should return job and increment view count")
        void shouldReturnJobAndIncrementViewCount() {
            Job job = buildJob(1L, 2L, JobStatus.ACTIVE);
            when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

            JobResponse result = jobService.getJobById(1L, 99L, "CANDIDATE");

            assertThat(result.getJobId()).isEqualTo(1L);
            verify(jobRepository).incrementViewCount(1L);
            verify(rabbitTemplate).convertAndSend(eq("hireconnect.exchange"), eq("analytics.routing.key"), any(Object.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for non-existent job")
        void shouldThrowForNonExistentJob() {
            when(jobRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> jobService.getJobById(999L, null, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("list and search methods")
    class ListAndSearchTests {

        @Test
        @DisplayName("should delegate list methods to repository")
        void shouldDelegateListMethods() {
            Job job = buildJob(1L, 2L, JobStatus.ACTIVE);
            PageRequest page = PageRequest.of(0, 10);
            when(jobRepository.findByStatus(JobStatus.ACTIVE, page)).thenReturn(new PageImpl<>(List.of(job)));
            when(jobRepository.findAll(page)).thenReturn(new PageImpl<>(List.of(job)));
            when(jobRepository.findByPostedBy(2L, page)).thenReturn(new PageImpl<>(List.of(job)));
            when(jobRepository.findByPostedByAndStatus(2L, JobStatus.ACTIVE, page)).thenReturn(new PageImpl<>(List.of(job)));
            when(jobRepository.findByPostedByOrderByCreatedAtDesc(2L)).thenReturn(List.of(job));

            assertThat(jobService.getAllActiveJobs(page).getContent()).hasSize(1);
            assertThat(jobService.getAllJobs(page, null).getContent()).hasSize(1);
            assertThat(jobService.getAllJobs(page, JobStatus.ACTIVE).getContent()).hasSize(1);
            assertThat(jobService.getJobsByRecruiter(2L, page).getContent()).hasSize(1);
            assertThat(jobService.getJobsByRecruiterAndStatus(2L, JobStatus.ACTIVE, page).getContent()).hasSize(1);
            assertThat(jobService.getAllJobsByRecruiter(2L)).hasSize(1);
        }

        @Test
        @DisplayName("should delegate search filters to repository")
        void shouldDelegateSearch() {
            Job job = buildJob(1L, 2L, JobStatus.ACTIVE);
            PageRequest page = PageRequest.of(0, 10);
            when(jobRepository.searchJobs("java", "Remote", "Engineering", JobType.FULL_TIME, 3, 100.0, 200.0, page))
                    .thenReturn(new PageImpl<>(List.of(job)));

            assertThat(jobService.searchJobs("java", "Remote", "Engineering", JobType.FULL_TIME, 3, 100.0, 200.0, page)
                    .getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("updateJob()")
    class UpdateJobTests {

        @Test
        @DisplayName("should update job successfully when recruiter owns it")
        void shouldUpdateJobSuccessfully() {
            Job job = buildJob(1L, 2L, JobStatus.ACTIVE);
            Job updated = buildJob(1L, 2L, JobStatus.ACTIVE);
            updated.setTitle("Senior Backend Developer");

            when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
            when(jobRepository.save(any(Job.class))).thenReturn(updated);

            JobRequest request = buildRequest();
            request.setTitle("Senior Backend Developer");

            JobResponse result = jobService.updateJob(1L, 2L, request);
            assertThat(result.getTitle()).isEqualTo("Senior Backend Developer");
        }

        @Test
        @DisplayName("should throw AccessDeniedException when another recruiter tries to update")
        void shouldThrowWhenNotOwner() {
            Job job = buildJob(1L, 2L, JobStatus.ACTIVE);
            when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

            assertThatThrownBy(() -> jobService.updateJob(1L, 99L, buildRequest()))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("should activate draft after validating current job and subscription limit")
        void shouldActivateDraftJob() {
            Job job = buildJob(1L, 2L, JobStatus.DRAFT);
            Job active = buildJob(1L, 2L, JobStatus.ACTIVE);
            when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
            when(subscriptionServiceClient.getMaxJobPosts(2L)).thenReturn(ApiResponse.success(2));
            when(jobRepository.countByPostedByAndStatus(2L, JobStatus.ACTIVE)).thenReturn(0L);
            when(jobRepository.save(any(Job.class))).thenReturn(active);

            JobResponse result = jobService.updateJobStatus(1L, 2L, JobStatus.ACTIVE);

            assertThat(result.getStatus()).isEqualTo(JobStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("updateJobStatus()")
    class UpdateStatusTests {

        @Test
        @DisplayName("should pause an active job")
        void shouldPauseActiveJob() {
            Job job = buildJob(1L, 2L, JobStatus.ACTIVE);
            Job paused = buildJob(1L, 2L, JobStatus.PAUSED);

            when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
            when(jobRepository.save(any(Job.class))).thenReturn(paused);

            JobResponse result = jobService.updateJobStatus(1L, 2L, JobStatus.PAUSED);
            assertThat(result.getStatus()).isEqualTo(JobStatus.PAUSED);
        }

        @Test
        @DisplayName("should throw when non-owner tries to update status")
        void shouldThrowForNonOwner() {
            Job job = buildJob(1L, 2L, JobStatus.ACTIVE);
            when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

            assertThatThrownBy(() -> jobService.updateJobStatus(1L, 99L, JobStatus.PAUSED))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("deleteJob()")
    class DeleteJobTests {

        @Test
        @DisplayName("should delete job when recruiter owns it")
        void shouldDeleteJobSuccessfully() {
            Job job = buildJob(1L, 2L, JobStatus.ACTIVE);
            when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

            assertThatCode(() -> jobService.deleteJob(1L, 2L)).doesNotThrowAnyException();
            verify(jobRepository).delete(job);
        }

        @Test
        @DisplayName("should throw AccessDeniedException when not owner")
        void shouldThrowForNonOwner() {
            Job job = buildJob(1L, 2L, JobStatus.ACTIVE);
            when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

            assertThatThrownBy(() -> jobService.deleteJob(1L, 99L))
                    .isInstanceOf(AccessDeniedException.class);
            verify(jobRepository, never()).delete(any());
        }
    }

    @Test
    @DisplayName("jobExists() should return true when job exists")
    void shouldReturnTrueWhenJobExists() {
        when(jobRepository.existsById(1L)).thenReturn(true);
        assertThat(jobService.jobExists(1L)).isTrue();
    }

    @Test
    @DisplayName("getRecruiterIdByJob() should return correct recruiter")
    void shouldReturnRecruiterIdByJob() {
        Job job = buildJob(1L, 2L, JobStatus.ACTIVE);
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        assertThat(jobService.getRecruiterIdByJob(1L)).isEqualTo(2L);
    }

    @Test
    @DisplayName("countJobsByRecruiter() should return correct count")
    void shouldReturnCorrectCount() {
        when(jobRepository.countByPostedBy(2L)).thenReturn(5L);
        assertThat(jobService.countJobsByRecruiter(2L)).isEqualTo(5L);
    }

    @Test
    @DisplayName("admin status update and delete should not require recruiter ownership")
    void adminStatusAndDeleteShouldBypassOwnership() {
        Job job = buildJob(1L, 2L, JobStatus.ACTIVE);
        Job closed = buildJob(1L, 2L, JobStatus.CLOSED);
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job), Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenReturn(closed);

        assertThat(jobService.updateJobStatusAsAdmin(1L, JobStatus.CLOSED).getStatus()).isEqualTo(JobStatus.CLOSED);
        jobService.deleteJobAsAdmin(1L);

        verify(jobRepository).delete(job);
    }
}
