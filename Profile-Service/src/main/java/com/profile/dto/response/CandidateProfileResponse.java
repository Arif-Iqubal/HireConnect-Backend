package com.profile.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CandidateProfileResponse {

	private Long profileId;
	private String fullName;
	private String email;
	private Long mobile;
	private List<String> skills;
	private int experience;
}
