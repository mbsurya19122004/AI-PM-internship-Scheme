package com.internshipplatform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internshipplatform.dto.LoginRequest;
import com.internshipplatform.dto.RegisterRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_resume",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String accessToken;
    private static Long resume1Id;
    private static Long resume2Id;

    @BeforeAll
    static void setup(@Autowired MockMvc mockMvc, @Autowired ObjectMapper objectMapper) throws Exception {
        // Register user
        RegisterRequest req = RegisterRequest.builder()
                .fullName("Resume User")
                .email("resume@test.com")
                .phoneNumber("9999999999")
                .college("IIT Bombay")
                .password("Pass@1234")
                .confirmPassword("Pass@1234")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        accessToken = objectMapper.readTree(body).path("data").path("accessToken").asText();
    }

    // --- UPLOAD ---

    @Test
    @Order(1)
    @DisplayName("Upload valid PDF returns 201")
    void uploadResume1() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume1.pdf", "application/pdf", "%PDF-1.4 fake resume".getBytes());

        MvcResult result = mockMvc.perform(multipart("/api/resumes/upload")
                        .file(file)
                        .param("description", "Main resume")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileName").isNotEmpty())
                .andExpect(jsonPath("$.data.originalFileName").value("resume1.pdf"))
                .andExpect(jsonPath("$.data.description").value("Main resume"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        resume1Id = objectMapper.readTree(body).path("data").path("id").asLong();
    }

    @Test
    @Order(2)
    @DisplayName("Upload second resume returns 201")
    void uploadResume2() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume2.pdf", "application/pdf", "%PDF-1.4 second resume".getBytes());

        MvcResult result = mockMvc.perform(multipart("/api/resumes/upload")
                        .file(file)
                        .param("description", "Internship resume")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.active").value(false))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        resume2Id = objectMapper.readTree(body).path("data").path("id").asLong();
    }

    @Test
    @Order(3)
    @DisplayName("Upload invalid file type returns 400")
    void uploadInvalidFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "not a pdf".getBytes());

        mockMvc.perform(multipart("/api/resumes/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Only PDF and Word documents are allowed"));
    }

    @Test
    @Order(4)
    @DisplayName("Upload without auth returns 401")
    void uploadNoAuth() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "%PDF-1.4 content".getBytes());

        mockMvc.perform(multipart("/api/resumes/upload")
                        .file(file))
                .andExpect(status().isUnauthorized());
    }

    // --- LIST ---

    @Test
    @Order(10)
    @DisplayName("List resumes returns both uploads")
    void listResumes() throws Exception {
        mockMvc.perform(get("/api/resumes")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    // --- ACTIVE ---

    @Test
    @Order(11)
    @DisplayName("Get active resume returns first upload")
    void getActiveResume() throws Exception {
        mockMvc.perform(get("/api/resumes/active")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.originalFileName").value("resume1.pdf"));
    }

    // --- GET BY ID ---

    @Test
    @Order(12)
    @DisplayName("Get resume by ID returns correct resume")
    void getResumeById() throws Exception {
        mockMvc.perform(get("/api/resumes/" + resume1Id)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(resume1Id))
                .andExpect(jsonPath("$.data.originalFileName").value("resume1.pdf"));
    }

    @Test
    @Order(13)
    @DisplayName("Get nonexistent resume returns error")
    void getNonexistentResume() throws Exception {
        mockMvc.perform(get("/api/resumes/99999")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isInternalServerError());
    }

    // --- ACTIVATE ---

    @Test
    @Order(20)
    @DisplayName("Activate resume 2 makes it active")
    void activateResume2() throws Exception {
        mockMvc.perform(put("/api/resumes/" + resume2Id + "/activate")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.active").value(true));

        // Verify resume 2 is now the active one
        mockMvc.perform(get("/api/resumes/active")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(jsonPath("$.data.id").value(resume2Id));
    }

    // --- DOWNLOAD ---

    @Test
    @Order(30)
    @DisplayName("Download resume returns file content")
    void downloadResume() throws Exception {
        mockMvc.perform(get("/api/resumes/" + resume1Id + "/download")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    // --- DELETE ---

    @Test
    @Order(40)
    @DisplayName("Delete resume 1 removes it")
    void deleteResume1() throws Exception {
        mockMvc.perform(delete("/api/resumes/" + resume1Id)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Resume deleted successfully"));

        // Verify only 1 resume left
        mockMvc.perform(get("/api/resumes")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @Order(41)
    @DisplayName("Download deleted resume returns error")
    void downloadDeletedResume() throws Exception {
        mockMvc.perform(get("/api/resumes/" + resume1Id + "/download")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isInternalServerError());
    }
}
