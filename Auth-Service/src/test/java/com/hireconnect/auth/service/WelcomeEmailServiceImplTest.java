package com.hireconnect.auth.service;

import com.hireconnect.auth.entity.UserCredential;
import com.hireconnect.auth.enums.AuthProvider;
import com.hireconnect.auth.enums.UserRole;
import com.hireconnect.auth.service.impl.WelcomeEmailServiceImpl;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WelcomeEmailServiceImplTest {

    @Mock private JavaMailSender mailSender;
    private WelcomeEmailServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new WelcomeEmailServiceImpl(mailSender);
        ReflectionTestUtils.setField(service, "fromEmail", "noreply@hireconnect.test");
        ReflectionTestUtils.setField(service, "fromName", "HireConnect");
        lenient().when(mailSender.createMimeMessage())
                .thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    void sendsCandidateWelcomeEmail() throws Exception {
        UserCredential user = user(UserRole.CANDIDATE, "candidate@example.com", "Candidate User");

        service.sendWelcomeEmail(user);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo("Welcome to HireConnect");
        assertThat(captor.getValue().getAllRecipients()[0].toString()).isEqualTo("candidate@example.com");
        assertThat(messageBody(captor.getValue())).contains("Candidate User", "/candidate/dashboard");
    }

    @Test
    void sendsRecruiterWelcomeEmailAndPasswordResetEmail() throws Exception {
        UserCredential user = user(UserRole.RECRUITER, "recruiter@example.com", "Recruiter User");

        service.sendWelcomeEmail(user);
        service.sendPasswordResetEmail(user, "reset-token", "http://localhost/reset");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(2)).send(captor.capture());
        assertThat(messageBody(captor.getAllValues().get(0))).contains("/recruiter/dashboard");
        assertThat(messageBody(captor.getAllValues().get(1))).contains("reset-token", "http://localhost/reset");
    }

    @Test
    void skipsUsersWithoutEmail() {
        service.sendWelcomeEmail(user(UserRole.ADMIN, " ", "Admin User"));
        service.sendPasswordResetEmail(user(UserRole.ADMIN, null, "Admin User"), "token", "url");

        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void swallowsMailSenderFailures() {
        UserCredential user = user(UserRole.CANDIDATE, "candidate@example.com", "");
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        service.sendWelcomeEmail(user);

        verify(mailSender).send(any(MimeMessage.class));
    }

    private UserCredential user(UserRole role, String email, String fullName) {
        return UserCredential.builder()
                .userId(1L)
                .email(email)
                .fullName(fullName)
                .role(role)
                .provider(AuthProvider.LOCAL)
                .build();
    }

    private String messageBody(MimeMessage message) throws Exception {
        return contentToString(message.getContent());
    }

    private String contentToString(Object content) throws Exception {
        if (content instanceof MimeMultipart multipart) {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                text.append(contentToString(multipart.getBodyPart(i).getContent()));
            }
            return text.toString();
        }
        return String.valueOf(content);
    }
}
