package com.hireconnect.interview.mapper;

import com.hireconnect.interview.dto.response.InterviewResponse;
import com.hireconnect.interview.entity.Interview;
import com.hireconnect.interview.enums.InterviewMode;
import com.hireconnect.interview.enums.InterviewStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InterviewMapper Tests")
class InterviewMapperTest {

	private final InterviewMapper mapper = new InterviewMapper();

	@Test
	@DisplayName("should map interview entity to response")
	void shouldMapInterviewToResponse() {

		LocalDateTime now = LocalDateTime.now();

		Interview interview = Interview.builder().interviewId(1L).applicationId(10L).candidateId(20L).recruiterId(30L)
				.jobId(40L).jobTitle("Java Developer").candidateName("Arif").candidateEmail("arif@test.com")
				.companyName("HireConnect").scheduledAt(now).durationMinutes(60).mode(InterviewMode.ONLINE)
				.meetLink("https://meet.google.com/test").location("Bhopal").status(InterviewStatus.SCHEDULED)
				.notes("Technical Round").cancellationReason(null).rescheduleReason(null).interviewerName("John")
				.roundNumber(2).createdAt(now).updatedAt(now).build();

		InterviewResponse response = mapper.toResponse(interview);

		assertThat(response).isNotNull();

		assertThat(response.getInterviewId()).isEqualTo(1L);

		assertThat(response.getApplicationId()).isEqualTo(10L);

		assertThat(response.getCandidateId()).isEqualTo(20L);

		assertThat(response.getRecruiterId()).isEqualTo(30L);

		assertThat(response.getJobId()).isEqualTo(40L);

		assertThat(response.getJobTitle()).isEqualTo("Java Developer");

		assertThat(response.getCandidateName()).isEqualTo("Arif");

		assertThat(response.getCandidateEmail()).isEqualTo("arif@test.com");

		assertThat(response.getCompanyName()).isEqualTo("HireConnect");

		assertThat(response.getScheduledAt()).isEqualTo(now);

		assertThat(response.getDurationMinutes()).isEqualTo(60);

		assertThat(response.getMode()).isEqualTo(InterviewMode.ONLINE);

		assertThat(response.getMeetLink()).isEqualTo("https://meet.google.com/test");

		assertThat(response.getLocation()).isEqualTo("Bhopal");

		assertThat(response.getStatus()).isEqualTo(InterviewStatus.SCHEDULED);

		assertThat(response.getNotes()).isEqualTo("Technical Round");

		assertThat(response.getInterviewerName()).isEqualTo("John");

		assertThat(response.getRoundNumber()).isEqualTo(2);
	}

	@Test
	@DisplayName("should return null when interview is null")
	void shouldReturnNullWhenInterviewIsNull() {

		InterviewResponse response = mapper.toResponse(null);

		assertThat(response).isNull();
	}
}