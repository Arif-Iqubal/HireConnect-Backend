package com.hireconnect.auth.service;

import com.hireconnect.auth.client.ProfileServiceClient;
import com.hireconnect.auth.config.security.JwtService;
import com.hireconnect.auth.dto.request.ChangePasswordRequest;
import com.hireconnect.auth.dto.request.ForgotPasswordRequest;
import com.hireconnect.auth.dto.request.LoginRequest;
import com.hireconnect.auth.dto.request.RefreshTokenRequest;
import com.hireconnect.auth.dto.request.RegisterRequest;
import com.hireconnect.auth.dto.response.AuthResponse;
import com.hireconnect.auth.dto.response.ForgotPasswordResponse;
import com.hireconnect.auth.entity.UserCredential;
import com.hireconnect.auth.enums.AuthProvider;
import com.hireconnect.auth.enums.UserRole;
import com.hireconnect.auth.exception.InvalidCredentialsException;
import com.hireconnect.auth.exception.InvalidTokenException;
import com.hireconnect.auth.exception.UserAlreadyExistsException;
import com.hireconnect.auth.repository.UserCredentialRepository;
import com.hireconnect.auth.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Tests")
class AuthServiceImplTest {

    @Mock
    private UserCredentialRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ProfileServiceClient profileServiceClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private WelcomeEmailService welcomeEmailService;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "exchange", "hireconnect.exchange");
        ReflectionTestUtils.setField(authService, "notificationRoutingKey", "notification.routing.key");
        ReflectionTestUtils.setField(authService, "frontendUrl", "http://localhost:4200");
    }

    private UserCredential buildUser(Long id, String email, UserRole role) {
        return UserCredential.builder()
                .userId(id)
                .email(email)
                .passwordHash("$2a$12$hashedpassword")
                .fullName("Test User")
                .role(role)
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .isEmailVerified(false)
                .refreshTokenExpiry(LocalDateTime.now().plusDays(7))
                .build();
    }

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("should register new candidate successfully")
        void shouldRegisterCandidate() {

            RegisterRequest req = new RegisterRequest();
            req.setEmail("alice@example.com");
            req.setPassword("password123");
            req.setFullName("Alice Smith");
            req.setRole(UserRole.CANDIDATE);

            UserCredential saved =
                    buildUser(1L, "alice@example.com", UserRole.CANDIDATE);

            when(userRepository.existsByEmail("alice@example.com"))
                    .thenReturn(false);

            when(passwordEncoder.encode("password123"))
                    .thenReturn("$2a$12$hashedpassword");

            when(userRepository.save(any(UserCredential.class)))
                    .thenReturn(saved);

            when(jwtService.generateAccessToken(any()))
                    .thenReturn("access-token");

            when(jwtService.generateRefreshToken(any()))
                    .thenReturn("refresh-token");

            when(jwtService.getExpirationMs())
                    .thenReturn(86400000L);

            when(jwtService.getRefreshExpirationMs())
                    .thenReturn(604800000L);

            AuthResponse result = authService.register(req);

            assertThat(result.getEmail())
                    .isEqualTo("alice@example.com");

            assertThat(result.getRole())
                    .isEqualTo(UserRole.CANDIDATE);

            assertThat(result.getAccessToken())
                    .isEqualTo("access-token");

            assertThat(result.getTokenType())
                    .isEqualTo("Bearer");

            verify(userRepository).save(any(UserCredential.class));

            verify(profileServiceClient)
                    .createCandidateProfile(
                            any(),
                            eq(1L),
                            eq("Bearer access-token")
                    );

            verify(welcomeEmailService)
                    .sendWelcomeEmail(saved);

            verify(rabbitTemplate)
                    .convertAndSend(
                            eq("hireconnect.exchange"),
                            eq("notification.routing.key"),
                            ArgumentMatchers.<Object>argThat(
                                    event ->
                                            ((Map<?, ?>) event).get("eventType").equals("USER_REGISTERED")
                                                    &&
                                                    ((Map<?, ?>) event).get("userEmail").equals("alice@example.com")
                            )
                    );
        }

        @Test
        @DisplayName("should create recruiter profile")
        void shouldCreateRecruiterProfile() {

            RegisterRequest req = new RegisterRequest();

            req.setEmail("recruiter@example.com");
            req.setPassword("password");
            req.setFullName("Recruiter");
            req.setRole(UserRole.RECRUITER);

            UserCredential saved =
                    buildUser(2L, "recruiter@example.com", UserRole.RECRUITER);

            when(userRepository.existsByEmail(anyString()))
                    .thenReturn(false);

            when(passwordEncoder.encode(anyString()))
                    .thenReturn("encoded");

            when(userRepository.save(any()))
                    .thenReturn(saved);

            when(jwtService.generateAccessToken(any()))
                    .thenReturn("access");

            when(jwtService.generateRefreshToken(any()))
                    .thenReturn("refresh");

            when(jwtService.getExpirationMs())
                    .thenReturn(86400000L);

            when(jwtService.getRefreshExpirationMs())
                    .thenReturn(604800000L);

            authService.register(req);

            verify(profileServiceClient)
                    .createRecruiterProfile(
                            any(),
                            eq(2L),
                            eq("Bearer access")
                    );
        }

        @Test
        @DisplayName("should not fail registration when welcome event publish fails")
        void shouldIgnoreWelcomeEventPublishFailure() {

            RegisterRequest req = new RegisterRequest();
            req.setEmail("alice@example.com");
            req.setPassword("password123");
            req.setFullName("Alice Smith");
            req.setRole(UserRole.CANDIDATE);

            UserCredential saved =
                    buildUser(1L, "alice@example.com", UserRole.CANDIDATE);

            when(userRepository.existsByEmail("alice@example.com"))
                    .thenReturn(false);

            when(passwordEncoder.encode("password123"))
                    .thenReturn("$2a$12$hashedpassword");

            when(userRepository.save(any(UserCredential.class)))
                    .thenReturn(saved);

            when(jwtService.generateAccessToken(any()))
                    .thenReturn("access-token");

            when(jwtService.generateRefreshToken(any()))
                    .thenReturn("refresh-token");

            when(jwtService.getExpirationMs())
                    .thenReturn(86400000L);

            when(jwtService.getRefreshExpirationMs())
                    .thenReturn(604800000L);

            doThrow(new RuntimeException("rabbit down"))
                    .when(rabbitTemplate)
                    .convertAndSend(anyString(), anyString(), any(Object.class));

            AuthResponse result = authService.register(req);

            assertThat(result.getEmail())
                    .isEqualTo("alice@example.com");

            verify(welcomeEmailService)
                    .sendWelcomeEmail(saved);
        }

        @Test
        @DisplayName("should throw UserAlreadyExistsException for duplicate email")
        void shouldThrowForDuplicateEmail() {

            RegisterRequest req = new RegisterRequest();
            req.setEmail("alice@example.com");
            req.setPassword("password123");
            req.setFullName("Alice Smith");
            req.setRole(UserRole.CANDIDATE);

            when(userRepository.existsByEmail("alice@example.com"))
                    .thenReturn(true);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("alice@example.com");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("should login with valid credentials")
        void shouldLoginSuccessfully() {

            LoginRequest req = new LoginRequest();
            req.setEmail("alice@example.com");
            req.setPassword("password123");

            UserCredential user =
                    buildUser(1L, "alice@example.com", UserRole.CANDIDATE);

            when(userRepository.findByEmail("alice@example.com"))
                    .thenReturn(Optional.of(user));

            when(passwordEncoder.matches("password123", user.getPasswordHash()))
                    .thenReturn(true);

            when(jwtService.generateAccessToken(user))
                    .thenReturn("access-token");

            when(jwtService.generateRefreshToken(user))
                    .thenReturn("refresh-token");

            when(jwtService.getExpirationMs())
                    .thenReturn(86400000L);

            when(jwtService.getRefreshExpirationMs())
                    .thenReturn(604800000L);

            AuthResponse result = authService.login(req);

            assertThat(result.getAccessToken()).isNotBlank();

            assertThat(result.getUserId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should throw InvalidCredentialsException for wrong password")
        void shouldThrowForWrongPassword() {

            LoginRequest req = new LoginRequest();
            req.setEmail("alice@example.com");
            req.setPassword("wrongpassword");

            UserCredential user =
                    buildUser(1L, "alice@example.com", UserRole.CANDIDATE);

            when(userRepository.findByEmail("alice@example.com"))
                    .thenReturn(Optional.of(user));

            when(passwordEncoder.matches("wrongpassword", user.getPasswordHash()))
                    .thenReturn(false);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("should throw for non-existent email")
        void shouldThrowForNonExistentEmail() {

            LoginRequest req = new LoginRequest();
            req.setEmail("nobody@example.com");
            req.setPassword("password123");

            when(userRepository.findByEmail("nobody@example.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("should throw for deactivated account")
        void shouldThrowForDeactivatedAccount() {

            LoginRequest req = new LoginRequest();
            req.setEmail("alice@example.com");
            req.setPassword("password123");

            UserCredential user =
                    buildUser(1L, "alice@example.com", UserRole.CANDIDATE);

            user.setIsActive(false);

            when(userRepository.findByEmail("alice@example.com"))
                    .thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("deactivated");
        }
    }

    @Nested
    @DisplayName("refreshToken()")
    class RefreshTokenTests {

        @Test
        @DisplayName("should refresh token successfully")
        void shouldRefreshTokenSuccessfully() {

            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("refresh-token");

            UserCredential user =
                    buildUser(1L, "alice@example.com", UserRole.CANDIDATE);

            user.setRefreshToken("refresh-token");
            user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(1));

            when(userRepository.findByRefreshToken("refresh-token"))
                    .thenReturn(Optional.of(user));

            when(jwtService.generateAccessToken(user))
                    .thenReturn("new-access");

            when(jwtService.generateRefreshToken(user))
                    .thenReturn("new-refresh");

            when(jwtService.getExpirationMs())
                    .thenReturn(86400000L);

            when(jwtService.getRefreshExpirationMs())
                    .thenReturn(604800000L);

            AuthResponse response =
                    authService.refreshToken(request);

            assertThat(response.getAccessToken())
                    .isEqualTo("new-access");
        }

        @Test
        @DisplayName("should throw for expired refresh token")
        void shouldThrowForExpiredRefreshToken() {

            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("expired-token");

            UserCredential user =
                    buildUser(1L, "alice@example.com", UserRole.CANDIDATE);

            user.setRefreshTokenExpiry(LocalDateTime.now().minusMinutes(1));

            when(userRepository.findByRefreshToken("expired-token"))
                    .thenReturn(Optional.of(user));

            assertThatThrownBy(() ->
                    authService.refreshToken(request))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("expired");
        }
    }

    @Nested
    @DisplayName("changePassword()")
    class ChangePasswordTests {

        @Test
        @DisplayName("should change password successfully")
        void shouldChangePasswordSuccessfully() {

            ChangePasswordRequest request =
                    new ChangePasswordRequest();

            request.setCurrentPassword("old");
            request.setNewPassword("new");

            UserCredential user =
                    buildUser(1L, "alice@example.com", UserRole.CANDIDATE);

            when(userRepository.findById(1L))
                    .thenReturn(Optional.of(user));

            when(passwordEncoder.matches("old", user.getPasswordHash()))
                    .thenReturn(true);

            when(passwordEncoder.encode("new"))
                    .thenReturn("encoded-new");

            authService.changePassword(1L, request);

            verify(userRepository).save(user);

            assertThat(user.getPasswordHash())
                    .isEqualTo("encoded-new");
        }

        @Test
        @DisplayName("should throw when current password is incorrect")
        void shouldThrowWhenCurrentPasswordWrong() {

            ChangePasswordRequest request =
                    new ChangePasswordRequest();

            request.setCurrentPassword("wrong");
            request.setNewPassword("new");

            UserCredential user =
                    buildUser(1L, "alice@example.com", UserRole.CANDIDATE);

            when(userRepository.findById(1L))
                    .thenReturn(Optional.of(user));

            when(passwordEncoder.matches("wrong", user.getPasswordHash()))
                    .thenReturn(false);

            assertThatThrownBy(() ->
                    authService.changePassword(1L, request))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
    }

    @Nested
    @DisplayName("forgotPassword()")
    class ForgotPasswordTests {

        @Test
        @DisplayName("should generate token, send email, and not expose token in response")
        void shouldSendResetEmailWithoutExposingToken() {

            ForgotPasswordRequest req = new ForgotPasswordRequest();
            req.setEmail("alice@example.com");

            UserCredential user =
                    buildUser(1L, "alice@example.com", UserRole.CANDIDATE);

            when(userRepository.findByEmail("alice@example.com"))
                    .thenReturn(Optional.of(user));

            ForgotPasswordResponse response =
                    authService.forgotPassword(req);

            assertThat(response.getResetToken()).isNull();

            assertThat(user.getPasswordResetToken()).isNotBlank();

            assertThat(user.getPasswordResetTokenExpiry())
                    .isAfter(LocalDateTime.now());

            verify(userRepository).save(user);

            verify(welcomeEmailService)
                    .sendPasswordResetEmail(
                            eq(user),
                            eq(user.getPasswordResetToken()),
                            contains("/auth/reset-password?token=")
                    );
        }

        @Test
        @DisplayName("should not reveal whether email exists")
        void shouldNotRevealMissingEmail() {

            ForgotPasswordRequest req =
                    new ForgotPasswordRequest();

            req.setEmail("missing@example.com");

            when(userRepository.findByEmail("missing@example.com"))
                    .thenReturn(Optional.empty());

            ForgotPasswordResponse response =
                    authService.forgotPassword(req);

            assertThat(response.getResetToken()).isNull();

            verify(welcomeEmailService, never())
                    .sendPasswordResetEmail(any(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("handleOAuthLogin()")
    class OAuthTests {

        @Test
        @DisplayName("should login existing OAuth user")
        void shouldLoginExistingOAuthUser() {

            UserCredential user =
                    buildUser(1L, "alice@example.com", UserRole.CANDIDATE);

            user.setProvider(AuthProvider.GITHUB);
            user.setProviderId("google-123");

            when(userRepository.findByProviderIdAndProvider(
                    "google-123",
                    AuthProvider.GITHUB))
                    .thenReturn(Optional.of(user));

            when(jwtService.generateAccessToken(user))
                    .thenReturn("access");

            when(jwtService.generateRefreshToken(user))
                    .thenReturn("refresh");

            when(jwtService.getExpirationMs())
                    .thenReturn(86400000L);

            when(jwtService.getRefreshExpirationMs())
                    .thenReturn(604800000L);

            AuthResponse response =
                    authService.handleOAuthLogin(
                            "alice@example.com",
                            "Alice",
                            "google-123",
                            AuthProvider.GITHUB,
                            UserRole.CANDIDATE
                    );

            assertThat(response.getAccessToken())
                    .isEqualTo("access");
        }

        @Test
        @DisplayName("should throw when OAuth email already exists")
        void shouldThrowWhenOAuthEmailAlreadyExists() {

            when(userRepository.findByProviderIdAndProvider(
                    "google-123",
                    AuthProvider.GITHUB))
                    .thenReturn(Optional.empty());

            when(userRepository.existsByEmail("alice@example.com"))
                    .thenReturn(true);

            assertThatThrownBy(() ->
                    authService.handleOAuthLogin(
                            "alice@example.com",
                            "Alice",
                            "google-123",
                            AuthProvider.GITHUB,
                            UserRole.CANDIDATE
                    ))
                    .isInstanceOf(UserAlreadyExistsException.class);
        }
    }

    @Nested
    @DisplayName("logout()")
    class LogoutTests {

        @Test
        @DisplayName("should clear refresh token on logout")
        void shouldClearRefreshToken() {

            assertThatCode(() ->
                    authService.logout(1L))
                    .doesNotThrowAnyException();

            verify(userRepository)
                    .updateRefreshToken(eq(1L), isNull(), isNull());
        }
    }

    @Test
    @DisplayName("validateToken() should return true for valid token")
    void shouldReturnTrueForValidToken() {

        when(jwtService.validateToken("valid-token"))
                .thenReturn(true);

        assertThat(authService.validateToken("valid-token"))
                .isTrue();
    }

    @Test
    @DisplayName("validateToken() should return false for invalid token")
    void shouldReturnFalseForInvalidToken() {

        when(jwtService.validateToken("bad-token"))
                .thenReturn(false);

        assertThat(authService.validateToken("bad-token"))
                .isFalse();
    }
}