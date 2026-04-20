package com.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.auth.dtos.AuthResponse;
import com.auth.dtos.LoginRequest;
import com.auth.dtos.RegisterRequest;
import com.auth.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService service;
	
	private static final String RegisterUser1 = """
			{
				"username":"arif", "email":"arif@gmail.com", "password":"arif1234", "role":"USER"
			}
			""";

	private static final String RegisterUser2 = """
			{
				"username":"mohan", "email":"mohan@gmail.com", "password":"mohan1234", "role":"USER"
			}
			""";
	private static final String LoginUser1 = """
			{
				"email":"arif@gmail.com", "password":"arif1234"
			}
			""";
	private static final String LoginUser2 = """
			{
				"email":"mohan@gamil.com", "password":"mohan1234"
			}
			""";

	@PostMapping("/register")
	@Operation(summary = "Register", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
			@ExampleObject(name = "User 1", value = RegisterUser1),
			@ExampleObject(name = "User 2", value = RegisterUser2), })))
	public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
		return ResponseEntity.ok(service.register(request));
	}

	@PostMapping("/login")
	@Operation(summary = "Login", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
			@ExampleObject(name = "User 1", value = LoginUser1),
			@ExampleObject(name = "User 2", value = LoginUser2), })))
	public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
		return ResponseEntity.ok(service.login(request));
	}

	@GetMapping("/validate")
	public ResponseEntity<Boolean> validate(@RequestParam String token) {
		return ResponseEntity.ok(service.validateToken(token));
	}

	@PostMapping("/refresh")
	public ResponseEntity<String> refresh(@RequestParam String token) {
		return ResponseEntity.ok(service.refreshToken(token));
	}
	
	@GetMapping("/oauth-success")
	public ResponseEntity<String> oauthSuccess(@RequestParam String token) {
	    return ResponseEntity.ok("OAuth Login Successful. Token: " + token);
	}
}
