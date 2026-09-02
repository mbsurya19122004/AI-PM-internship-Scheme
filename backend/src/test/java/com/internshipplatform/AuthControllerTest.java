package com.internshipplatform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internshipplatform.dto.*;
import com.internshipplatform.entity.PasswordResetToken;
import com.internshipplatform.entity.User;
import com.internshipplatform.repository.PasswordResetTokenRepository;
import com.internshipplatform.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_auth",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static String accessToken;
    private static String refreshToken;

    private RegisterRequest validRegisterRequest() {
        return RegisterRequest.builder()
                .fullName("Test User")
                .email("test@example.com")
                .phoneNumber("9876543210")
                .college("IIT Delhi")
                .department("CS")
                .graduationYear("2027")
                .password("Pass@1234")
                .confirmPassword("Pass@1234")
                .build();
    }

    // --- REGISTER ---

    @Test
    @Order(1)
    @DisplayName("Register with valid data returns 201 and tokens")
    void registerSuccess() throws Exception {
        RegisterRequest req = validRegisterRequest();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.fullName").value("Test User"))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.user.college").value("IIT Delhi"))
                .andExpect(jsonPath("$.data.user.phoneNumber").value("9876543210"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        accessToken = objectMapper.readTree(body).path("data").path("accessToken").asText();
        refreshToken = objectMapper.readTree(body).path("data").path("refreshToken").asText();
    }

    @Test
    @Order(2)
    @DisplayName("Register with duplicate email returns 400")
    void registerDuplicateEmail() throws Exception {
        RegisterRequest req = validRegisterRequest();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    @Order(3)
    @DisplayName("Register with duplicate phone returns 400")
    void registerDuplicatePhone() throws Exception {
        RegisterRequest req = validRegisterRequest();
        req.setEmail("other@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Phone number is already registered"));
    }

    @Test
    @Order(4)
    @DisplayName("Register with weak password returns 400")
    void registerWeakPassword() throws Exception {
        RegisterRequest req = validRegisterRequest();
        req.setEmail("weak@example.com");
        req.setPhoneNumber("1111111111");
        req.setPassword("weak");
        req.setConfirmPassword("weak");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Order(5)
    @DisplayName("Register with mismatched passwords returns 400")
    void registerMismatchedPasswords() throws Exception {
        RegisterRequest req = validRegisterRequest();
        req.setEmail("mismatch@example.com");
        req.setPhoneNumber("2222222222");
        req.setConfirmPassword("Different@123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Order(6)
    @DisplayName("Register with invalid email returns 400")
    void registerInvalidEmail() throws Exception {
        RegisterRequest req = validRegisterRequest();
        req.setEmail("not-an-email");
        req.setPhoneNumber("3333333333");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // --- LOGIN ---

    @Test
    @Order(10)
    @DisplayName("Login with correct credentials returns 200 and tokens")
    void loginSuccess() throws Exception {
        LoginRequest req = new LoginRequest("test@example.com", "Pass@1234");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    @Order(11)
    @DisplayName("Login with wrong password returns 401")
    void loginWrongPassword() throws Exception {
        LoginRequest req = new LoginRequest("test@example.com", "WrongPass@1");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @Order(12)
    @DisplayName("Login with nonexistent email returns 401")
    void loginNonexistentUser() throws Exception {
        LoginRequest req = new LoginRequest("nobody@example.com", "Pass@1234");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // --- PROFILE ---

    @Test
    @Order(20)
    @DisplayName("GET /me with valid token returns user profile")
    void getMeWithToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Test User"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.college").value("IIT Delhi"));
    }

    @Test
    @Order(21)
    @DisplayName("GET /me without token returns 401")
    void getMeWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(22)
    @DisplayName("GET /me with invalid token returns 401")
    void getMeWithBadToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }

    // --- REFRESH TOKEN ---

    @Test
    @Order(30)
    @DisplayName("Refresh token returns new access token")
    void refreshTokenSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @Order(31)
    @DisplayName("Refresh with invalid token returns 401")
    void refreshTokenInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"bad.token.here\"}"))
                .andExpect(status().isUnauthorized());
    }

    // --- VALIDATION EDGE CASES ---

    @Test
    @Order(40)
    @DisplayName("Register with empty body returns 400")
    void registerEmptyBody() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(41)
    @DisplayName("Login with empty body returns 400")
    void loginEmptyBody() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(42)
    @DisplayName("Register with short name returns 400")
    void registerShortName() throws Exception {
        RegisterRequest req = validRegisterRequest();
        req.setEmail("short@example.com");
        req.setPhoneNumber("4444444444");
        req.setFullName("A");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // --- CHANGE PASSWORD ---

    @Test
    @Order(50)
    @DisplayName("Change password with valid data returns 200")
    void changePasswordSuccess() throws Exception {
        // Re-login to get a fresh token
        LoginRequest loginReq = new LoginRequest("test@example.com", "Pass@1234");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        String body = loginResult.getResponse().getContentAsString();
        String freshToken = objectMapper.readTree(body).path("data").path("accessToken").asText();

        ChangePasswordRequest req = ChangePasswordRequest.builder()
                .currentPassword("Pass@1234")
                .newPassword("NewPass@1234")
                .confirmNewPassword("NewPass@1234")
                .build();

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + freshToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully"));

        // Verify old password no longer works
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("test@example.com", "Pass@1234"))))
                .andExpect(status().isUnauthorized());

        // Verify new password works
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("test@example.com", "NewPass@1234"))))
                .andExpect(status().isOk());

        // Reset password back for other tests
        ChangePasswordRequest resetReq = ChangePasswordRequest.builder()
                .currentPassword("NewPass@1234")
                .newPassword("Pass@1234")
                .confirmNewPassword("Pass@1234")
                .build();

        MvcResult loginResult2 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("test@example.com", "NewPass@1234"))))
                .andExpect(status().isOk())
                .andReturn();

        String body2 = loginResult2.getResponse().getContentAsString();
        String freshToken2 = objectMapper.readTree(body2).path("data").path("accessToken").asText();

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + freshToken2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(51)
    @DisplayName("Change password with wrong current password returns 401")
    void changePasswordWrongCurrent() throws Exception {
        LoginRequest loginReq = new LoginRequest("test@example.com", "Pass@1234");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        String body = loginResult.getResponse().getContentAsString();
        String freshToken = objectMapper.readTree(body).path("data").path("accessToken").asText();

        ChangePasswordRequest req = ChangePasswordRequest.builder()
                .currentPassword("WrongPass@123")
                .newPassword("NewPass@1234")
                .confirmNewPassword("NewPass@1234")
                .build();

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + freshToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Order(52)
    @DisplayName("Change password with mismatched new passwords returns 400")
    void changePasswordMismatched() throws Exception {
        LoginRequest loginReq = new LoginRequest("test@example.com", "Pass@1234");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        String body = loginResult.getResponse().getContentAsString();
        String freshToken = objectMapper.readTree(body).path("data").path("accessToken").asText();

        ChangePasswordRequest req = ChangePasswordRequest.builder()
                .currentPassword("Pass@1234")
                .newPassword("NewPass@1234")
                .confirmNewPassword("Different@123")
                .build();

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + freshToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Passwords do not match"));
    }

    @Test
    @Order(53)
    @DisplayName("Change password without auth returns 401")
    void changePasswordNoAuth() throws Exception {
        ChangePasswordRequest req = ChangePasswordRequest.builder()
                .currentPassword("Pass@1234")
                .newPassword("NewPass@1234")
                .confirmNewPassword("NewPass@1234")
                .build();

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // --- FORGOT PASSWORD ---

    @Test
    @Order(60)
    @DisplayName("Forgot password with valid email returns success (even if user exists)")
    void forgotPasswordSuccess() throws Exception {
        ForgotPasswordRequest req = new ForgotPasswordRequest("test@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If the email exists, a reset link has been sent"));
    }

    @Test
    @Order(61)
    @DisplayName("Forgot password with non-existent email returns same success message")
    void forgotPasswordNonExistent() throws Exception {
        ForgotPasswordRequest req = new ForgotPasswordRequest("nobody@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If the email exists, a reset link has been sent"));
    }

    // --- RESET PASSWORD ---

    @Test
    @Order(70)
    @DisplayName("Reset password with valid token returns 200")
    void resetPasswordSuccess() throws Exception {
        // Create a valid reset token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .email("test@example.com")
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .attemptCount(0)
                .build();
        passwordResetTokenRepository.save(resetToken);

        ResetPasswordRequest req = ResetPasswordRequest.builder()
                .token(token)
                .newPassword("ResetPass@1234")
                .confirmNewPassword("ResetPass@1234")
                .build();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password reset successfully"));

        // Verify old password no longer works
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("test@example.com", "Pass@1234"))))
                .andExpect(status().isUnauthorized());

        // Verify new password works
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("test@example.com", "ResetPass@1234"))))
                .andExpect(status().isOk());

        // Reset password back for other tests
        String resetToken2 = UUID.randomUUID().toString();
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .token(resetToken2)
                .email("test@example.com")
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .attemptCount(0)
                .build());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ResetPasswordRequest.builder()
                                .token(resetToken2)
                                .newPassword("Pass@1234")
                                .confirmNewPassword("Pass@1234")
                                .build())))
                .andExpect(status().isOk());
    }

    @Test
    @Order(71)
    @DisplayName("Reset password with invalid token returns 400")
    void resetPasswordInvalidToken() throws Exception {
        ResetPasswordRequest req = ResetPasswordRequest.builder()
                .token("invalid-token")
                .newPassword("NewPass@1234")
                .confirmNewPassword("NewPass@1234")
                .build();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired reset token"));
    }

    @Test
    @Order(72)
    @DisplayName("Reset password with expired token returns 400")
    void resetPasswordExpiredToken() throws Exception {
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .email("test@example.com")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .used(false)
                .attemptCount(0)
                .build();
        passwordResetTokenRepository.save(resetToken);

        ResetPasswordRequest req = ResetPasswordRequest.builder()
                .token(token)
                .newPassword("NewPass@1234")
                .confirmNewPassword("NewPass@1234")
                .build();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired reset token"));
    }

    @Test
    @Order(73)
    @DisplayName("Reset password with used token returns 400")
    void resetPasswordUsedToken() throws Exception {
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .email("test@example.com")
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(true)
                .attemptCount(0)
                .build();
        passwordResetTokenRepository.save(resetToken);

        ResetPasswordRequest req = ResetPasswordRequest.builder()
                .token(token)
                .newPassword("NewPass@1234")
                .confirmNewPassword("NewPass@1234")
                .build();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired reset token"));
    }

    @Test
    @Order(74)
    @DisplayName("Reset password with mismatched passwords returns 400")
    void resetPasswordMismatched() throws Exception {
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .email("test@example.com")
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .attemptCount(0)
                .build();
        passwordResetTokenRepository.save(resetToken);

        ResetPasswordRequest req = ResetPasswordRequest.builder()
                .token(token)
                .newPassword("NewPass@1234")
                .confirmNewPassword("Different@123")
                .build();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Passwords do not match"));
    }
}
