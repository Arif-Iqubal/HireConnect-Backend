package com.profile.repository;


import com.profile.pojo.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CandidateRepository extends JpaRepository<CandidateProfile, Long> {

    Optional<CandidateProfile> findByEmail(String email);

    Optional<CandidateProfile> findByMobile(Long mobile);
}