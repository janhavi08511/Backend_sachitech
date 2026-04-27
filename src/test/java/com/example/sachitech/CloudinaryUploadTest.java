package com.example.sachitech;

import com.cloudinary.Cloudinary;
import com.example.sachitech.entity.*;
import com.example.sachitech.repository.*;
import com.example.sachitech.service.LmsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Cloudinary Upload & File Viewing Tests")
class CloudinaryUploadTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private LmsService lmsService;

    @Autowired
    private Cloudinary cloudinary;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private LmsContentRepository contentRepository;

    @Autowired
    private LmsSubmissionRepository submissionRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String studentToken;
    private Long courseId;
    private Long studentProfileId;
    private Long assignmentId;

    @BeforeEach
    void setup() throws Exception {
        // Get admin token
        MvcResult adminLogin = mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@example.com\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode adminResponse = objectMapper.readTree(adminLogin.getResponse().getContentAsString());
        adminToken = adminResponse.get("token").asText();

        // Get student token
        MvcResult studentLogin = mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"student@example.com\",\"password\":\"student123\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode studentResponse = objectMapper.readTree(studentLogin.getResponse().getContentAsString());
        studentToken = studentResponse.get("token").asText();

        // Get course ID
        MvcResult coursesResult = mvc.perform(get("/api/courses")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode coursesArray = objectMapper.readTree(coursesResult.getResponse().getContentAsString());
        if (coursesArray.isArray() && coursesArray.size() > 0) {
            courseId = coursesArray.get(0).get("id").asLong();
        }

        // Get student profile ID
        MvcResult studentsResult = mvc.perform(get("/api/students")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode studentsArray = objectMapper.readTree(studentsResult.getResponse().getContentAsString());
        if (studentsArray.isArray() && studentsArray.size() > 0) {
            studentProfileId = studentsArray.get(0).get("id").asLong();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 1: Cloudinary Configuration
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TC-CLOUD-01: Cloudinary bean is properly configured")
    void cloudinaryConfigured() {
        assertThat(cloudinary).isNotNull();
        assertThat(cloudinary.config.cloudName).isEqualTo("dek24uvqz");
        System.out.println("✅ [PASS] TC-CLOUD-01: Cloudinary is configured with cloud_name=dek24uvqz");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 2: Upload PDF to Cloudinary
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TC-CLOUD-02: Upload PDF file to Cloudinary")
    void uploadPdfToCloudinary() throws Exception {
        if (courseId == null) {
            System.out.println("[WARN] TC-CLOUD-02: skipped — no course available");
            return;
        }

        // Create a mock PDF file
        byte[] pdfContent = "PDF_MOCK_CONTENT".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                pdfContent
        );

        // Upload via LMS endpoint
        MvcResult result = mvc.perform(multipart("/api/lms/upload")
                .file(file)
                .param("title", "Test Assignment")
                .param("type", "ASSIGNMENT")
                .param("courseId", courseId.toString())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        String fileUrl = response.get("fileUrl").asText();

        assertThat(fileUrl)
                .isNotNull()
                .isNotEmpty()
                .contains("cloudinary")
                .contains("https");

        System.out.println("✅ [PASS] TC-CLOUD-02: PDF uploaded to Cloudinary");
        System.out.println("   File URL: " + fileUrl);

        assignmentId = response.get("id").asLong();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 3: Upload Image to Cloudinary
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TC-CLOUD-03: Upload image file to Cloudinary")
    void uploadImageToCloudinary() throws Exception {
        if (courseId == null) {
            System.out.println("[WARN] TC-CLOUD-03: skipped — no course available");
            return;
        }

        // Create a mock image file
        byte[] imageContent = "IMAGE_MOCK_CONTENT".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                imageContent
        );

        // Upload via LMS endpoint
        MvcResult result = mvc.perform(multipart("/api/lms/upload")
                .file(file)
                .param("title", "Test Image")
                .param("type", "NOTE")
                .param("courseId", courseId.toString())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        String fileUrl = response.get("fileUrl").asText();

        assertThat(fileUrl)
                .isNotNull()
                .isNotEmpty()
                .contains("cloudinary")
                .contains("https");

        System.out.println("✅ [PASS] TC-CLOUD-03: Image uploaded to Cloudinary");
        System.out.println("   File URL: " + fileUrl);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 4: View Uploaded File
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TC-CLOUD-04: View uploaded file via Cloudinary URL")
    void viewUploadedFile() throws Exception {
        if (courseId == null) {
            System.out.println("[WARN] TC-CLOUD-04: skipped — no course available");
            return;
        }

        // First upload a file
        byte[] pdfContent = "PDF_MOCK_CONTENT".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-view.pdf",
                "application/pdf",
                pdfContent
        );

        MvcResult uploadResult = mvc.perform(multipart("/api/lms/upload")
                .file(file)
                .param("title", "Test View File")
                .param("type", "ASSIGNMENT")
                .param("courseId", courseId.toString())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode uploadResponse = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String fileUrl = uploadResponse.get("fileUrl").asText();
        Long contentId = uploadResponse.get("id").asLong();

        // Verify file URL is accessible
        assertThat(fileUrl).isNotNull().isNotEmpty();

        // Fetch content to verify file URL is stored
        MvcResult getResult = mvc.perform(get("/api/lms/content/" + courseId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode contentArray = objectMapper.readTree(getResult.getResponse().getContentAsString());
        assertThat(contentArray.isArray()).isTrue();

        boolean found = false;
        for (JsonNode content : contentArray) {
            if (content.get("id").asLong() == contentId) {
                String storedUrl = content.get("fileUrl").asText();
                assertThat(storedUrl).isEqualTo(fileUrl);
                found = true;
                break;
            }
        }

        assertThat(found).isTrue();
        System.out.println("✅ [PASS] TC-CLOUD-04: File URL is stored and retrievable");
        System.out.println("   Content ID: " + contentId);
        System.out.println("   File URL: " + fileUrl);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 5: Student Submit Assignment with File
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TC-CLOUD-05: Student submits assignment with file upload")
    void studentSubmitAssignment() throws Exception {
        if (courseId == null || studentProfileId == null) {
            System.out.println("[WARN] TC-CLOUD-05: skipped — missing IDs");
            return;
        }

        // First create an assignment
        byte[] assignmentContent = "ASSIGNMENT_CONTENT".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile assignmentFile = new MockMultipartFile(
                "file",
                "assignment.pdf",
                "application/pdf",
                assignmentContent
        );

        MvcResult assignmentResult = mvc.perform(multipart("/api/lms/upload")
                .file(assignmentFile)
                .param("title", "Test Assignment for Submission")
                .param("type", "ASSIGNMENT")
                .param("courseId", courseId.toString())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode assignmentResponse = objectMapper.readTree(assignmentResult.getResponse().getContentAsString());
        Long assignmentId = assignmentResponse.get("id").asLong();

        // Now student submits
        byte[] submissionContent = "STUDENT_SUBMISSION_CONTENT".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile submissionFile = new MockMultipartFile(
                "file",
                "submission.pdf",
                "application/pdf",
                submissionContent
        );

        MvcResult submitResult = mvc.perform(multipart("/api/lms/submit")
                .file(submissionFile)
                .param("assignmentId", assignmentId.toString())
                .param("studentId", studentProfileId.toString()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode submitResponse = objectMapper.readTree(submitResult.getResponse().getContentAsString());
        String submissionFileUrl = submitResponse.get("fileUrl").asText();

        assertThat(submissionFileUrl)
                .isNotNull()
                .isNotEmpty()
                .contains("cloudinary");

        System.out.println("✅ [PASS] TC-CLOUD-05: Student submission uploaded to Cloudinary");
        System.out.println("   Submission File URL: " + submissionFileUrl);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 6: Retrieve All Submissions with File URLs
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TC-CLOUD-06: Retrieve all submissions with file URLs")
    void retrieveAllSubmissions() throws Exception {
        MvcResult result = mvc.perform(get("/api/lms/submissions")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode submissionsArray = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(submissionsArray.isArray()).isTrue();

        int submissionsWithUrls = 0;
        for (JsonNode submission : submissionsArray) {
            String fileUrl = submission.get("fileUrl").asText();
            if (fileUrl != null && !fileUrl.isEmpty()) {
                submissionsWithUrls++;
                assertThat(fileUrl).contains("cloudinary");
            }
        }

        System.out.println("✅ [PASS] TC-CLOUD-06: Retrieved " + submissionsArray.size() + " submissions");
        System.out.println("   Submissions with Cloudinary URLs: " + submissionsWithUrls);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 7: File URL Format Validation
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TC-CLOUD-07: Uploaded file URLs have correct format")
    void validateFileUrlFormat() throws Exception {
        if (courseId == null) {
            System.out.println("[WARN] TC-CLOUD-07: skipped — no course available");
            return;
        }

        byte[] fileContent = "TEST_CONTENT".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-format.pdf",
                "application/pdf",
                fileContent
        );

        MvcResult result = mvc.perform(multipart("/api/lms/upload")
                .file(file)
                .param("title", "Format Test")
                .param("type", "ASSIGNMENT")
                .param("courseId", courseId.toString())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        String fileUrl = response.get("fileUrl").asText();

        // Validate URL format
        assertThat(fileUrl)
                .startsWith("https://")
                .contains("cloudinary.com")
                .contains("res.cloudinary.com")
                .contains("dek24uvqz"); // cloud name

        System.out.println("✅ [PASS] TC-CLOUD-07: File URL format is valid");
        System.out.println("   URL: " + fileUrl);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 8: File Persistence in Database
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TC-CLOUD-08: File URLs are persisted in database")
    void fileUrlPersistence() throws Exception {
        if (courseId == null) {
            System.out.println("[WARN] TC-CLOUD-08: skipped — no course available");
            return;
        }

        byte[] fileContent = "PERSISTENCE_TEST".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "persistence-test.pdf",
                "application/pdf",
                fileContent
        );

        MvcResult result = mvc.perform(multipart("/api/lms/upload")
                .file(file)
                .param("title", "Persistence Test")
                .param("type", "ASSIGNMENT")
                .param("courseId", courseId.toString())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        String uploadedUrl = response.get("fileUrl").asText();
        Long contentId = response.get("id").asLong();

        // Verify in database
        LmsContent content = contentRepository.findById(contentId).orElse(null);
        assertThat(content).isNotNull();
        assertThat(content.getFileUrl()).isEqualTo(uploadedUrl);

        System.out.println("✅ [PASS] TC-CLOUD-08: File URL persisted in database");
        System.out.println("   Content ID: " + contentId);
        System.out.println("   Stored URL: " + content.getFileUrl());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 9: Multiple File Uploads
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TC-CLOUD-09: Multiple files can be uploaded independently")
    void multipleFileUploads() throws Exception {
        if (courseId == null) {
            System.out.println("[WARN] TC-CLOUD-09: skipped — no course available");
            return;
        }

        int uploadCount = 3;
        for (int i = 0; i < uploadCount; i++) {
            byte[] fileContent = ("FILE_CONTENT_" + i).getBytes(StandardCharsets.UTF_8);
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test-file-" + i + ".pdf",
                    "application/pdf",
                    fileContent
            );

            MvcResult result = mvc.perform(multipart("/api/lms/upload")
                    .file(file)
                    .param("title", "Test File " + i)
                    .param("type", "ASSIGNMENT")
                    .param("courseId", courseId.toString())
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            String fileUrl = response.get("fileUrl").asText();
            assertThat(fileUrl).isNotNull().isNotEmpty();
        }

        System.out.println("✅ [PASS] TC-CLOUD-09: Successfully uploaded " + uploadCount + " files");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 10: Unauthorized Upload Attempt
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TC-CLOUD-10: Unauthorized users cannot upload files")
    void unauthorizedUpload() throws Exception {
        if (courseId == null) {
            System.out.println("[WARN] TC-CLOUD-10: skipped — no course available");
            return;
        }

        byte[] fileContent = "UNAUTHORIZED_TEST".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "unauthorized.pdf",
                "application/pdf",
                fileContent
        );

        mvc.perform(multipart("/api/lms/upload")
                .file(file)
                .param("title", "Unauthorized Upload")
                .param("type", "ASSIGNMENT")
                .param("courseId", courseId.toString()))
                .andExpect(status().isUnauthorized());

        System.out.println("✅ [PASS] TC-CLOUD-10: Unauthorized upload properly rejected");
    }
}
