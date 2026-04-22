package com.profile.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CandidateProfileRequest {

    @NotBlank(message = "Name required")
    private String fullName;

    @Email
    @NotBlank
    private String email;

    @Pattern(regexp = "^[0-9]{10}$")
    private String mobile;

    @Past
    private LocalDate dob;

    @NotBlank
    private String gender;

    private List<String> skills;

    @Min(0)
    private int experience;

    private String resumeUrl;

    private List<AddressRequest> addresses;
}