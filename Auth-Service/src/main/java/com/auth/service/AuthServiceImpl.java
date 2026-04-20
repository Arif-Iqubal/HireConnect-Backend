package com.auth.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.auth.dtos.AuthResponse;
import com.auth.dtos.LoginRequest;
import com.auth.dtos.RegisterRequest;
import com.auth.exception.CustomException;
import com.auth.pojo.UserCredential;
import com.auth.repository.AuthRepository;
import com.auth.utils.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

	private final AuthRepository repo;
	private final PasswordEncoder encoder;
	private final JwtUtil jwtUtil;

	@Override
	public AuthResponse register(RegisterRequest request) {

		if (repo.existsByEmail(request.getEmail())) {
			throw new CustomException("User already exists");
		}

		log.info("Incoming password: {}", request.getPassword());
		UserCredential user = UserCredential.builder().email(request.getEmail())
				.passwordHash(encoder.encode(request.getPassword())).role(request.getRole()).provider("LOCAL")
				.createdAt(LocalDateTime.now()).build();

		repo.save(user);

		String token = jwtUtil.generateToken(user.getEmail());

		return new AuthResponse(token, "Registered successfully");
	}

	@Override
	public AuthResponse login(LoginRequest request) {

		UserCredential user = repo.findByEmail(request.getEmail())
				.orElseThrow(() -> new CustomException("User not found"));

		if (!encoder.matches(request.getPassword(), user.getPasswordHash())) {
			throw new CustomException("Invalid credentials");
		}

		String token = jwtUtil.generateToken(user.getEmail());

		return new AuthResponse(token, "Login successful");
	}

	@Override
	public void logout(String token) {
		// future: blacklist token
		log.info("User logged out");
	}

	@Override
	public boolean validateToken(String token) {
		try {
			jwtUtil.extractEmail(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public String refreshToken(String token) {
		String email = jwtUtil.extractEmail(token);
		return jwtUtil.generateToken(email);
	}

	@Override
	public UserCredential getByEmail(String email) {
		return repo.findByEmail(email).orElseThrow(() -> new CustomException("User not found"));
	}
}