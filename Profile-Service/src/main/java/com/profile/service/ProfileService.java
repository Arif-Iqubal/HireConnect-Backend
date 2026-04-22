package com.profile.service;

import java.util.List;

import com.profile.dto.request.CandidateProfileRequest;
import com.profile.dto.request.RecruiterProfileRequest;
import com.profile.dto.response.CandidateProfileResponse;
import com.profile.dto.response.RecruiterProfileResponse;

public interface ProfileService {

	CandidateProfileResponse addCandidateProfile(CandidateProfileRequest request);

	RecruiterProfileResponse addRecruiterProfile(RecruiterProfileRequest request);

	CandidateProfileResponse updateCandidateProfile(Long id, CandidateProfileRequest request);

	RecruiterProfileResponse updateRecruiterProfile(Long id, RecruiterProfileRequest request);

	CandidateProfileResponse getCandidateById(Long id);

	RecruiterProfileResponse getRecruiterById(Long id);

	CandidateProfileResponse getCandidateByEmail(String email);

	RecruiterProfileResponse getRecruiterByEmail(String email);

	void deleteCandidate(Long id);

	void deleteRecruiter(Long id);
}