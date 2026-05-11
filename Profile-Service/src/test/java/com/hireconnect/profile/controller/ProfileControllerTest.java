package com.hireconnect.profile.controller;

import com.hireconnect.profile.dto.request.CandidateProfileRequest;
import com.hireconnect.profile.dto.request.RecruiterProfileRequest;
import com.hireconnect.profile.dto.response.ApiResponse;
import com.hireconnect.profile.dto.response.CandidateNotificationRecipientResponse;
import com.hireconnect.profile.dto.response.CandidateProfileResponse;
import com.hireconnect.profile.dto.response.RecruiterProfileResponse;
import com.hireconnect.profile.dto.response.ResumeUploadResponse;
import com.hireconnect.profile.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock private ProfileService profileService;
    @InjectMocks private ProfileController controller;

    private final CandidateProfileResponse candidate = CandidateProfileResponse.builder()
            .userId(1L)
            .fullName("Candidate")
            .email("candidate@example.com")
            .build();

    private final RecruiterProfileResponse recruiter = RecruiterProfileResponse.builder()
            .userId(2L)
            .fullName("Recruiter")
            .companyName("HireConnect")
            .build();

    @Test
    void createCandidateProfileReturnsCreatedResponse() {
        CandidateProfileRequest request = new CandidateProfileRequest();
        when(profileService.createCandidateProfile(1L, request)).thenReturn(candidate);

        ResponseEntity<ApiResponse<CandidateProfileResponse>> response = controller.createCandidateProfile(request, 1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatusCode()).isEqualTo(201);
        assertThat(response.getBody().getData()).isEqualTo(candidate);
    }

    @Test
    void candidateEndpointsDelegateToService() {
        CandidateProfileRequest request = new CandidateProfileRequest();
        MockMultipartFile resume = new MockMultipartFile("resume", "resume.pdf", "application/pdf", "%PDF".getBytes());
        ResumeUploadResponse uploadResponse = ResumeUploadResponse.builder().resumeUrl("/uploads/resume.pdf").build();
        when(profileService.getCandidateProfile(1L)).thenReturn(candidate);
        when(profileService.updateCandidateProfile(1L, request)).thenReturn(candidate);
        when(profileService.uploadCandidateResume(1L, resume)).thenReturn(uploadResponse);
        when(profileService.getCandidateResumeUrl(1L)).thenReturn("/uploads/resume.pdf");
        when(profileService.candidateProfileExists(1L)).thenReturn(true);
        when(profileService.getCandidateNotificationRecipients()).thenReturn(List.of(
                new CandidateNotificationRecipientResponse(1L, "Candidate", "candidate@example.com")));

        assertThat(controller.getCandidateProfile(1L).getBody().getData()).isEqualTo(candidate);
        assertThat(controller.getMyProfile(1L).getBody().getData()).isEqualTo(candidate);
        assertThat(controller.updateCandidateProfile(request, 1L).getBody().getData()).isEqualTo(candidate);
        assertThat(controller.uploadCandidateResume(resume, 1L).getBody().getData()).isEqualTo(uploadResponse);
        assertThat(controller.getCandidateResumeUrl(1L).getBody().getData()).isEqualTo("/uploads/resume.pdf");
        assertThat(controller.candidateExists(1L).getBody().getData()).isTrue();
        assertThat(controller.getCandidateNotificationRecipients().getBody().getData()).hasSize(1);

        controller.deleteCandidateProfile(1L);
        verify(profileService).deleteCandidateProfile(1L);
    }

    @Test
    void createRecruiterProfileReturnsCreatedResponse() {
        RecruiterProfileRequest request = new RecruiterProfileRequest();
        when(profileService.createRecruiterProfile(2L, request)).thenReturn(recruiter);

        ResponseEntity<ApiResponse<RecruiterProfileResponse>> response = controller.createRecruiterProfile(request, 2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo(recruiter);
    }

    @Test
    void recruiterEndpointsDelegateToService() {
        RecruiterProfileRequest request = new RecruiterProfileRequest();
        when(profileService.getRecruiterProfile(2L)).thenReturn(recruiter);
        when(profileService.updateRecruiterProfile(2L, request)).thenReturn(recruiter);

        assertThat(controller.getRecruiterProfile(2L).getBody().getData()).isEqualTo(recruiter);
        assertThat(controller.getMyRecruiterProfile(2L).getBody().getData()).isEqualTo(recruiter);
        assertThat(controller.updateRecruiterProfile(request, 2L).getBody().getData()).isEqualTo(recruiter);

        controller.deleteRecruiterProfile(2L);
        verify(profileService).deleteRecruiterProfile(2L);
    }
}
