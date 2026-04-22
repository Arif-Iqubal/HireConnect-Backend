package com.profile.controller;

import com.profile.dto.request.*;
import com.profile.dto.response.*;
import com.profile.service.ProfileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

	private final ProfileService profileService;

	// 🔹 Candidate Example
	private static final String CANDIDATE_EXAMPLE = """
			{
			  "fullName": "Arif Iqubal",
			  "email": "arif@gmail.com",
			  "mobile": "9876543210",
			  "dob": "2000-05-15",
			  "gender": "MALE",
			  "skills": ["Java", "Spring Boot", "React"],
			  "experience": 2,
			  "resumeUrl": "https://example.com/resume.pdf",
			  "addresses": [
			    {
			      "street": "MP Nagar",
			      "city": "Bhopal",
			      "state": "MP",
			      "zip": "462011"
			    }
			  ]
			}
			""";

	// 🔹 Recruiter Example
	private static final String RECRUITER_EXAMPLE = """
			{
			  "fullName": "Mohan Sharma",
			  "email": "mohan@gmail.com",
			  "companyName": "TechCorp",
			  "companySize": "100-500",
			  "industry": "IT",
			  "website": "https://techcorp.com",
			  "addresses": [
			    {
			      "street": "Sector 62",
			      "city": "Noida",
			      "state": "UP",
			      "zip": "201309"
			    }
			  ]
			}
			""";

	// 🔹 Update Candidate Example
	private static final String UPDATE_CANDIDATE_EXAMPLE = """
			{
			  "fullName": "Arif Iqubal Updated",
			  "email": "arif.updated@gmail.com",
			  "mobile": "9876543210",
			  "dob": "2000-05-15",
			  "gender": "MALE",
			  "skills": ["Java", "Spring Boot", "Microservices"],
			  "experience": 3,
			  "resumeUrl": "https://example.com/resume-new.pdf",
			  "addresses": [
			    {
			      "street": "MP Nagar Zone 2",
			      "city": "Bhopal",
			      "state": "MP",
			      "zip": "462011"
			    }
			  ]
			}
			""";

	// 🔹 Update Recruiter Example
	private static final String UPDATE_RECRUITER_EXAMPLE = """
			{
			  "fullName": "Mohan Sharma Updated",
			  "email": "mohan.updated@gmail.com",
			  "companyName": "TechCorp Pvt Ltd",
			  "companySize": "500-1000",
			  "industry": "IT Services",
			  "website": "https://techcorp.com",
			  "addresses": [
			    {
			      "street": "Cyber City",
			      "city": "Gurgaon",
			      "state": "Haryana",
			      "zip": "122002"
			    }
			  ]
			}
			""";

	// 🔹 Add Candidate Profile
	@PostMapping("/candidate")
	@Operation(summary = "Add Candidate Profile", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
			@ExampleObject(name = "Candidate Example", value = CANDIDATE_EXAMPLE) })))

	public ResponseEntity<CandidateProfileResponse> addCandidate(@Valid @RequestBody CandidateProfileRequest request) {

		return ResponseEntity.ok(profileService.addCandidateProfile(request));
	}

	// 🔹 Add Recruiter Profile
	@PostMapping("/recruiter")
	@Operation(summary = "Add Recruiter Profile", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
			@ExampleObject(name = "Recruiter Example", value = RECRUITER_EXAMPLE) })))
	public ResponseEntity<RecruiterProfileResponse> addRecruiter(@Valid @RequestBody RecruiterProfileRequest request) {

		return ResponseEntity.ok(profileService.addRecruiterProfile(request));
	}

	@PutMapping("/candidate/{id}")
	@Operation(summary = "Update Candidate Profile", description = "Update an existing candidate profile by ID", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
			@ExampleObject(name = "Update Candidate Example", value = UPDATE_CANDIDATE_EXAMPLE) })))
	public ResponseEntity<CandidateProfileResponse> updateCandidate(@PathVariable Long id,
			@Valid @RequestBody CandidateProfileRequest request) {

		return ResponseEntity.ok(profileService.updateCandidateProfile(id, request));
	}

	@PutMapping("/recruiter/{id}")
	@Operation(summary = "Update Recruiter Profile", description = "Update an existing recruiter profile by ID", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
			@ExampleObject(name = "Update Recruiter Example", value = UPDATE_RECRUITER_EXAMPLE) })))
	public ResponseEntity<RecruiterProfileResponse> updateRecruiter(@PathVariable Long id,
			@Valid @RequestBody RecruiterProfileRequest request) {

		return ResponseEntity.ok(profileService.updateRecruiterProfile(id, request));
	}

	// 🔹 Get Profile by ID
	@GetMapping("/candidate/{id}")
	@Operation(summary = "Get Candidate Profile by ID", description = "Example: /profile/candidate/1")
	public ResponseEntity<CandidateProfileResponse> getCandidateById(@PathVariable Long id) {
	    return ResponseEntity.ok(profileService.getCandidateById(id));
	}
	
	
	@GetMapping("/recruiter/{id}")
	@Operation(summary = "Get Recruiter Profile by ID", description = "Example: /profile/recruiter/1")
	public ResponseEntity<RecruiterProfileResponse> getRecruiterById(@PathVariable Long id) {
	    return ResponseEntity.ok(profileService.getRecruiterById(id));
	}
	
	
	@GetMapping("/candidate/email")
	@Operation(summary = "Get Candidate Profile by Email", description = "Example: /profile/candidate/email?email=arif@gmail.com")
	public ResponseEntity<CandidateProfileResponse> getCandidateByEmail(@RequestParam String email) {
	    return ResponseEntity.ok(profileService.getCandidateByEmail(email));
	}
	
	
	@GetMapping("/recruiter/email")
	@Operation(summary = "Get Recruiter Profile by Email", description = "Example: /profile/recruiter/email?email=arif@gmail.com")
	public ResponseEntity<RecruiterProfileResponse> getRecruiterByEmail(@RequestParam String email) {
	    return ResponseEntity.ok(profileService.getRecruiterByEmail(email));
	}
	
	
	@DeleteMapping("/candidate/{id}")
	@Operation(summary = "Delete Candidate Profile", description = "Example: DELETE /profile/candidate/1")
	public ResponseEntity<String> deleteCandidate(@PathVariable Long id) {

	    profileService.deleteCandidate(id);
	    return ResponseEntity.ok("Candidate deleted successfully");
	}
	
	
	@DeleteMapping("/recruiter/{id}")
	@Operation(summary = "Delete Recruiter Profile", description = "Example: DELETE /profile/recruiter/1")
	public ResponseEntity<String> deleteRecruiter(@PathVariable Long id) {

	    profileService.deleteRecruiter(id);
	    return ResponseEntity.ok("Recruiter deleted successfully");
	}
	
}