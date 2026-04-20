package com.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.auth.pojo.UserCredential;

@Repository
public interface AuthRepository extends JpaRepository<UserCredential, Long> {

	Optional<UserCredential> findByEmail(String email);

	boolean existsByEmail(String email);
}
