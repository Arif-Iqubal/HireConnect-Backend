package com.profile.repository;


import com.profile.pojo.RecruiterProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecruiterRepository extends JpaRepository<RecruiterProfile, Long> {

	Optional<RecruiterProfile> findByEmail(String email);
}