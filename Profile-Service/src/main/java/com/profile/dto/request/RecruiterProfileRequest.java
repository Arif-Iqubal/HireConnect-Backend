package com.profile.dto.request;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RecruiterProfileRequest {

    @NotBlank
    private String fullName;

    @Email
    private String email;

    @NotBlank
    private String companyName;

    private String companySize;
    private String industry;

    @Pattern(regexp = "^(http|https)://.*$")
    private String website;

    private List<AddressRequest> addresses;
}