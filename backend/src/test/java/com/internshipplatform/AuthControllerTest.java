package com.internshipplatform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internshipplatform.dto.LoginRequest;
import com.internshipplatform.dto.RegisterRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
}
