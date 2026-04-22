package com.profile.service;

import com.profile.dto.request.*;
import com.profile.dto.response.*;
import com.profile.pojo.*;
import com.profile.exception.CustomException;
import com.profile.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

	private final CandidateRepository candidateRepo;
	private final RecruiterRepository recruiterRepo;

	// 🔹 Add Candidate
	@Override
	public CandidateProfileResponse addCandidateProfile(CandidateProfileRequest request) {

		if (candidateRepo.findByEmail(request.getEmail()).isPresent()) {
			throw new CustomException("Candidate already exists with this email");
		}

		CandidateProfile entity = new CandidateProfile();
		entity.setFullName(request.getFullName());
		entity.setEmail(request.getEmail());
		entity.setMobile(Long.parseLong(request.getMobile()));
		entity.setDob(request.getDob());
		entity.setGender(request.getGender());
		entity.setSkills(request.getSkills());
		entity.setExperience(request.getExperience());
		entity.setResumeUrl(request.getResumeUrl());

		// Address mapping
		if (request.getAddresses() != null) {
			entity.setAddresses(request.getAddresses().stream().map(a -> {
				Address addr = new Address();
				addr.setHouseNo(a.getHouseNo());
				addr.setStreet(a.getStreet());
				addr.setCity(a.getCity());
				addr.setState(a.getState());
				addr.setPincode(a.getPincode());
				return addr;
			}).toList());
		}

		CandidateProfile saved = candidateRepo.save(entity);

		return CandidateProfileResponse.builder().profileId(saved.getProfileId()).fullName(saved.getFullName())
				.email(saved.getEmail()).mobile(saved.getMobile()).skills(saved.getSkills())
				.experience(saved.getExperience()).build();
	}

	// 🔹 Add Recruiter
	@Override
	public RecruiterProfileResponse addRecruiterProfile(RecruiterProfileRequest request) {

		if (recruiterRepo.findByEmail(request.getEmail()).isPresent()) {
			throw new CustomException("Recruiter already exists");
		}

		RecruiterProfile entity = new RecruiterProfile();
		entity.setFullName(request.getFullName());
		entity.setEmail(request.getEmail());
		entity.setCompanyName(request.getCompanyName());
		entity.setCompanySize(request.getCompanySize());
		entity.setIndustry(request.getIndustry());
		entity.setWebsite(request.getWebsite());

		if (request.getAddresses() != null) {
			entity.setAddresses(request.getAddresses().stream().map(a -> {
				Address addr = new Address();
				addr.setHouseNo(a.getHouseNo());
				addr.setStreet(a.getStreet());
				addr.setCity(a.getCity());
				addr.setState(a.getState());
				addr.setPincode(a.getPincode());
				return addr;
			}).toList());
		}

		RecruiterProfile saved = recruiterRepo.save(entity);

		return RecruiterProfileResponse.builder().profileId(saved.getProfileId()).fullName(saved.getFullName())
				.email(saved.getEmail()).companyName(saved.getCompanyName()).industry(saved.getIndustry()).build();
	}

	@Override
	public CandidateProfileResponse updateCandidateProfile(Long id, CandidateProfileRequest request) {

		CandidateProfile existing = candidateRepo.findById(id)
				.orElseThrow(() -> new CustomException("Candidate not found"));

		existing.setFullName(request.getFullName());
		existing.setEmail(request.getEmail());
		existing.setMobile(Long.parseLong(request.getMobile()));
		existing.setDob(request.getDob());
		existing.setGender(request.getGender());

		// ✅ FIXED
		existing.setSkills(request.getSkills() != null ? new ArrayList<>(request.getSkills()) : new ArrayList<>());

		existing.setExperience(request.getExperience());
		existing.setResumeUrl(request.getResumeUrl());

		if (request.getAddresses() != null) {

			List<Address> addressList = request.getAddresses().stream().map(a -> {
				Address addr = new Address();
				addr.setHouseNo(a.getHouseNo());
				addr.setStreet(a.getStreet());
				addr.setCity(a.getCity());
				addr.setState(a.getState());
				addr.setPincode(a.getPincode());
				return addr;
			}).collect(Collectors.toList());

			// ✅ BEST PRACTICE
			existing.getAddresses().clear();
			existing.getAddresses().addAll(addressList);
		}

		CandidateProfile updated = candidateRepo.save(existing);

		return CandidateProfileResponse.builder().profileId(updated.getProfileId()).fullName(updated.getFullName())
				.email(updated.getEmail()).mobile(updated.getMobile()).skills(updated.getSkills())
				.experience(updated.getExperience()).build();
	}

	@Override
	public RecruiterProfileResponse updateRecruiterProfile(Long id, RecruiterProfileRequest request) {

		RecruiterProfile existing = recruiterRepo.findById(id)
				.orElseThrow(() -> new CustomException("Recruiter not found"));

		// 🔹 Update fields
		existing.setFullName(request.getFullName());
		existing.setEmail(request.getEmail());
		existing.setCompanyName(request.getCompanyName());
		existing.setCompanySize(request.getCompanySize());
		existing.setIndustry(request.getIndustry());
		existing.setWebsite(request.getWebsite());

		//  FIXED ADDRESS HANDLING
		if (request.getAddresses() != null) {

			List<Address> addressList = request.getAddresses().stream().map(a -> {
				Address addr = new Address();
				addr.setHouseNo(a.getHouseNo());
				addr.setStreet(a.getStreet());
				addr.setCity(a.getCity());
				addr.setState(a.getState());
				addr.setPincode(a.getPincode());
				return addr;
			}).collect(Collectors.toList()); // ✅ MUTABLE LIST

			// ✅ BEST PRACTICE FOR JPA
			existing.getAddresses().clear();
			existing.getAddresses().addAll(addressList);
		}

		RecruiterProfile updated = recruiterRepo.save(existing);

		return RecruiterProfileResponse.builder().profileId(updated.getProfileId()).fullName(updated.getFullName())
				.email(updated.getEmail()).companyName(updated.getCompanyName()).industry(updated.getIndustry())
				.build();
	}

	// 🔹 Get by ID
	@Override
	public CandidateProfileResponse getCandidateById(Long id) {

	    CandidateProfile c = candidateRepo.findById(id)
	            .orElseThrow(() -> new CustomException("Candidate not found"));

	    return CandidateProfileResponse.builder()
	            .profileId(c.getProfileId())
	            .fullName(c.getFullName())
	            .email(c.getEmail())
	            .mobile(c.getMobile())
	            .skills(c.getSkills())
	            .experience(c.getExperience())
	            .build();
	}
	
	@Override
	public RecruiterProfileResponse getRecruiterById(Long id) {

	    RecruiterProfile r = recruiterRepo.findById(id)
	            .orElseThrow(() -> new CustomException("Recruiter not found"));

	    return RecruiterProfileResponse.builder()
	            .profileId(r.getProfileId())
	            .fullName(r.getFullName())
	            .email(r.getEmail())
	            .companyName(r.getCompanyName())
	            .industry(r.getIndustry())
	            .build();
	}
	
	@Override
	public CandidateProfileResponse getCandidateByEmail(String email) {

	    CandidateProfile c = candidateRepo.findByEmail(email)
	            .orElseThrow(() -> new CustomException("Candidate not found"));

	    return CandidateProfileResponse.builder()
	            .profileId(c.getProfileId())
	            .fullName(c.getFullName())
	            .email(c.getEmail())
	            .mobile(c.getMobile())
	            .skills(c.getSkills())
	            .experience(c.getExperience())
	            .build();
	}
	
	@Override
	public RecruiterProfileResponse getRecruiterByEmail(String email) {

	    RecruiterProfile r = recruiterRepo.findByEmail(email)
	            .orElseThrow(() -> new CustomException("Recruiter not found"));

	    return RecruiterProfileResponse.builder()
	            .profileId(r.getProfileId())
	            .fullName(r.getFullName())
	            .email(r.getEmail())
	            .companyName(r.getCompanyName())
	            .industry(r.getIndustry())
	            .build();
	}
	
	@Override
	public void deleteCandidate(Long id) {

	    if (!candidateRepo.existsById(id)) {
	        throw new CustomException("Candidate not found");
	    }

	    candidateRepo.deleteById(id);
	}
	
	@Override
	public void deleteRecruiter(Long id) {

	    if (!recruiterRepo.existsById(id)) {
	        throw new CustomException("Recruiter not found");
	    }

	    recruiterRepo.deleteById(id);
	}
}
