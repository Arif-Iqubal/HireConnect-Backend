package com.auth.service;

import com.auth.dtos.AuthResponse;
import com.auth.dtos.LoginRequest;
import com.auth.dtos.RegisterRequest;
import com.auth.pojo.UserCredential;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    void logout(String token);

    boolean validateToken(String token);

    String refreshToken(String token);

    UserCredential getByEmail(String email);
}