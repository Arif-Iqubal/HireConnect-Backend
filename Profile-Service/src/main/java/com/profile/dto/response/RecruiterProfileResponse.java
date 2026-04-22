package com.profile.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecruiterProfileResponse {

    private Long profileId;
    private String fullName;
    private String email;
    private String companyName;
    private String industry;
}
