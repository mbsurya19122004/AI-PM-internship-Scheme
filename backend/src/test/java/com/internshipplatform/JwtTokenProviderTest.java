package com.internshipplatform;

import com.internshipplatform.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_jwt",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class JwtTokenProviderTest {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Test
    @DisplayName("Generate and validate access token")
    void generateAndValidateAccessToken() {
        String token = tokenProvider.generateAccessTokenFromEmail("test@example.com");

        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("Generate and validate refresh token")
    void generateAndValidateRefreshToken() {
        String token = tokenProvider.generateRefreshTokenFromEmail("test@example.com");

        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
        assertTrue(tokenProvider.isRefreshToken(token));
    }

    @Test
    @DisplayName("Extract email from valid token")
    void extractEmail() {
        String token = tokenProvider.generateAccessTokenFromEmail("user@test.com");
        String email = tokenProvider.getEmailFromToken(token);

        assertEquals("user@test.com", email);
    }

    @Test
    @DisplayName("Invalid token fails validation")
    void invalidToken() {
        assertFalse(tokenProvider.validateToken("completely.invalid.token"));
    }

    @Test
    @DisplayName("Access token is not a refresh token")
    void accessTokenIsNotRefreshToken() {
        String token = tokenProvider.generateAccessTokenFromEmail("test@example.com");
        assertFalse(tokenProvider.isRefreshToken(token));
    }

    @Test
    @DisplayName("Token expiration values are positive")
    void expirationValues() {
        assertTrue(tokenProvider.getJwtExpiration() > 0);
        assertTrue(tokenProvider.getRefreshExpiration() > 0);
        assertTrue(tokenProvider.getRefreshExpiration() > tokenProvider.getJwtExpiration());
    }
}
