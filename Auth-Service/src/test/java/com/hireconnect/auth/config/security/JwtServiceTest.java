package com.hireconnect.auth.config.security;

import com.hireconnect.auth.entity.UserCredential;
import com.hireconnect.auth.enums.AuthProvider;
import com.hireconnect.auth.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private UserCredential user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "HireConnectTestSecretKeyForJwtServiceCoverage1234567890");
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3_600_000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpirationMs", 86_400_000L);
        user = UserCredential.builder()
                .userId(42L)
                .email("candidate@example.com")
                .fullName("Candidate User")
                .role(UserRole.CANDIDATE)
                .provider(AuthProvider.LOCAL)
                .build();
    }

    @Test
    void generatesValidAccessTokenWithExpectedClaims() {
        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.extractSubject(token)).isEqualTo("42");
        assertThat(jwtService.extractEmail(token)).isEqualTo("candidate@example.com");
        assertThat(jwtService.extractRole(token)).isEqualTo("CANDIDATE");
        assertThat(jwtService.isTokenExpired(token)).isFalse();
        assertThat(jwtService.getExpirationMs()).isEqualTo(3_600_000L);
    }

    @Test
    void generatesValidRefreshTokenAndRejectsMalformedTokens() {
        String refreshToken = jwtService.generateRefreshToken(user);

        assertThat(jwtService.validateToken(refreshToken)).isTrue();
        assertThat(jwtService.extractSubject(refreshToken)).isEqualTo("42");
        assertThat(jwtService.getRefreshExpirationMs()).isEqualTo(86_400_000L);
        assertThat(jwtService.validateToken("not-a-token")).isFalse();
    }
}
