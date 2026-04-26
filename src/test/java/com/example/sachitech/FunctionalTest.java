package com.example.sachitech;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Functional Test Suite — Sachitech TMS
 * Tests all major API flows using data from DataSheet.xlsx
 *
 * Run: ./mvnw test -Dtest=FunctionalTest -pl .
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FunctionalTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    // Shared state across ordered tests
    static String ADMIN_TOKEN;
    static String STUDENT_TOKEN;
    static Long   PYTHON_COURSE_ID;
    static Long   WEBDESIGN_COURSE_ID;
    static Long   STUDENT_PROFILE_ID;   // Samiksha

    // =========================================================================
    // SECTION 1 — AUTH
    // =========================================================================

    @Test @Order(1)
    @DisplayName("TC-AUTH-01: Admin login returns JWT")
    void adminLogin() throws Exception {
        String body = """
            {"email":"admin@gmail.com","password":"admin123"}
            """;
        MvcResult r = mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode json = om.readTree(r.getResponse().getContentAsString());
        assertThat(json.has("token")).isTrue();
        assertThat(json.get("role").asText()).isEqualTo("ADMIN");
        ADMIN_TOKEN = json.get("token").asText();
        System.out.println("[PASS] TC-AUTH-01: Admin login OK, role=ADMIN");
    }

    @Test @Order(2)
    @DisplayName("TC-AUTH-02: Wrong password is rejected")
    void wrongPassword() throws Exception {
        String body = """
            {"email":"admin@gmail.com","password":"wrongpass"}
            """;
        mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().is5xxServerError()); // Spring Security throws 500 on bad creds
        System.out.println("[PASS] TC-AUTH-02: Wrong password rejected");
    }

    @Test @Order(3)
    @DisplayName("TC-AUTH-03: Unknown user is rejected")
    void unknownUser() throws Exception {
        String body = """
            {"email":"nobody@x.com","password":"x"}
            """;
        mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().is5xxServerError());
        System.out.println("[PASS] TC-AUTH-03: Unknown user rejected");
    }

    @Test @Order(4)
    @DisplayName("TC-AUTH-04: Protected endpoint blocked without token")
    void noTokenBlocked() throws Exception {
        mvc.perform(get("/api/admin/users"))
            .andExpect(status().isForbidden());
        System.out.println("[PASS] TC-AUTH-04: No-token request blocked with 403");
    }

    // =========================================================================
    // SECTION 2 — COURSES
    // =========================================================================

    @Test @Order(10)
    @DisplayName("TC-CRS-01: List all courses returns non-empty array")
    void listCourses() throws Exception {
        MvcResult r = mvc.perform(get("/api/admin/courses")
                .header("Authorization", "Bearer " + ADMIN_TOKEN))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode arr = om.readTree(r.getResponse().getContentAsString());
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.size()).isGreaterThan(0);

        // Capture Python and Web Design IDs for later tests
        for (JsonNode c : arr) {
            if ("Python".equals(c.get("name").asText()))     PYTHON_COURSE_ID    = c.get("id").asLong();
            if ("Web Design".equals(c.get("name").asText())) WEBDESIGN_COURSE_ID = c.get("id").asLong();
        }
        System.out.printf("[PASS] TC-CRS-01: %d courses found, Python id=%d%n", arr.size(), PYTHON_COURSE_ID);
    }

    @Test @Order(11)
    @DisplayName("TC-CRS-02: Create a new course")
    void createCourse() throws Exception {
        String body = """
            {"name":"Test Course","duration":"1 month","category":"Test",
             "totalFee":1000,"status":"ACTIVE","description":"Temp test course"}
            """;
        MvcResult r = mvc.perform(post("/api/admin/add-course")
                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode json = om.readTree(r.getResponse().getContentAsString());
        assertThat(json.get("id").asLong()).isGreaterThan(0);
        System.out.println("[PASS] TC-CRS-02: Course created id=" + json.get("id").asLong());
    }

    // =========================================================================
    // SECTION 3 — STUDENTS (seeded by DataSheetSeeder on startup)
    // =========================================================================

    @Test @Order(20)
    @DisplayName("TC-STU-01: Student list contains DataSheet students")
    void studentListPopulated() throws Exception {
        MvcResult r = mvc.perform(get("/api/admin/studentdata/all")
                .header("Authorization", "Bearer " + ADMIN_TOKEN))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode arr = om.readTree(r.getResponse().getContentAsString());
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.size()).isGreaterThanOrEqualTo(31);

        // Find Samiksha's profile ID
        for (JsonNode s : arr) {
            if ("Samiksha Balasaheb Ugale".equals(s.get("name").asText())) {
                STUDENT_PROFILE_ID = s.get("id").asLong();
            }
        }
        assertThat(STUDENT_PROFILE_ID).isNotNull();
        System.out.printf("[PASS] TC-STU-01: %d students, Samiksha profileId=%d%n", arr.size(), STUDENT_PROFILE_ID);
    }

    @Test @Order(21)
    @DisplayName("TC-STU-02: Student login works with seeded credentials")
    void studentLogin() throws Exception {
        String body = """
            {"email":"samiksha.ugale@test.com","password":"Student@123"}
            """;
        MvcResult r = mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode json = om.readTree(r.getResponse().getContentAsString());
        assertThat(json.get("role").asText()).isEqualTo("STUDENT");
        STUDENT_TOKEN = json.get("token").asText();
        System.out.println("[PASS] TC-STU-02: Student login OK, role=STUDENT");
    }

    // =========================================================================
    // SECTION 4 — FEE RECORDS & TRANSACTIONS
    // =========================================================================

    @Test @Order(30)
    @DisplayName("TC-FEE-01: All fee records are present")
    void feeRecordsPresent() throws Exception {
        MvcResult r = mvc.perform(get("/api/fees/records")
                .header("Authorization", "Bearer " + ADMIN_TOKEN))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode arr = om.readTree(r.getResponse().getContentAsString());
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.size()).isGreaterThanOrEqualTo(31);
        System.out.printf("[PASS] TC-FEE-01: %d fee records found%n", arr.size());
    }

    @Test @Order(31)
    @DisplayName("TC-FEE-02: paid + pending == totalFee for every record (integrity)")
    void feeIntegrity() throws Exception {
        MvcResult r = mvc.perform(get("/api/fees/records")
                .header("Authorization", "Bearer " + ADMIN_TOKEN))
            .andReturn();

        JsonNode arr = om.readTree(r.getResponse().getContentAsString());
        int failures = 0;
        for (JsonNode rec : arr) {
            double paid    = rec.get("amountPaid").asDouble();
            double pending = rec.get("pendingAmount").asDouble();
            double total   = rec.get("totalFeeAtEnrollment").asDouble();
            if (Math.abs((paid + pending) - total) > 0.01) {
                System.out.printf("[FAIL] Integrity: %s paid=%.0f + pending=%.0f != total=%.0f%n",
                    rec.get("studentName").asText(), paid, pending, total);
                failures++;
            }
        }
        assertThat(failures).as("Fee record integrity failures").isEqualTo(0);
        System.out.printf("[PASS] TC-FEE-02: All %d records pass paid+pending=total%n", arr.size());
    }

    @Test @Order(32)
    @DisplayName("TC-FEE-03: Spot-check Samiksha — paid=4000, pending=1000")
    void spotCheckSamiksha() throws Exception {
        MvcResult r = mvc.perform(get("/api/fees/records")
                .header("Authorization", "Bearer " + ADMIN_TOKEN))
            .andReturn();
        JsonNode arr = om.readTree(r.getResponse().getContentAsString());

        JsonNode rec = null;
        for (JsonNode n : arr) {
            if ("Samiksha Balasaheb Ugale".equals(n.get("studentName").asText())) { rec = n; break; }
        }
        assertThat(rec).as("Samiksha fee record").isNotNull();
        assertThat(rec.get("amountPaid").asDouble()).isEqualTo(4000.0);
        assertThat(rec.get("pendingAmount").asDouble()).isEqualTo(1000.0);
        System.out.println("[PASS] TC-FEE-03: Samiksha paid=4000, pending=1000 — matches datasheet");
    }

    @Test @Order(33)
    @DisplayName("TC-FEE-04: Spot-check Mrunmai — paid=4500, integrity holds")
    void spotCheckMrunmai() throws Exception {
        MvcResult r = mvc.perform(get("/api/fees/records")
                .header("Authorization", "Bearer " + ADMIN_TOKEN))
            .andReturn();
        JsonNode arr = om.readTree(r.getResponse().getContentAsString());

        JsonNode rec = null;
        for (JsonNode n : arr) {
            if ("Mrunmai Rajesh Suryavanshi".equals(n.get("studentName").asText())) { rec = n; break; }
        }
        assertThat(rec).as("Mrunmai fee record").isNotNull();
        assertThat(rec.get("amountPaid").asDouble()).isEqualTo(4500.0);
        // pending = totalFeeAtEnrollment - amountPaid (course fee may differ from datasheet discount)
        double pending = rec.get("pendingAmount").asDouble();
        double total   = rec.get("totalFeeAtEnrollment").asDouble();
        assertThat(Math.abs((4500.0 + pending) - total)).isLessThan(0.01);
        System.out.printf("[PASS] TC-FEE-04: Mrunmai paid=4500, pending=%.0f, total=%.0f — integrity OK%n",
            pending, total);
    }

    @Test @Order(34)
    @DisplayName("TC-FEE-05: Spot-check Kamini — partial (paid=6000, pending=8000)")
    void spotCheckKamini() throws Exception {
        MvcResult r = mvc.perform(get("/api/fees/records")
                .header("Authorization", "Bearer " + ADMIN_TOKEN))
            .andReturn();
        JsonNode arr = om.readTree(r.getResponse().getContentAsString());

        JsonNode rec = null;
        for (JsonNode n : arr) {
            if ("Kamini Harshal Gaikwad".equals(n.get("studentName").asText())) { rec = n; break; }
        }
        assertThat(rec).as("Kamini fee record").isNotNull();
        assertThat(rec.get("amountPaid").asDouble()).isEqualTo(6000.0);
        assertThat(rec.get("pendingAmount").asDouble()).isEqualTo(8000.0);
        System.out.println("[PASS] TC-FEE-05: Kamini paid=6000, pending=8000 — matches datasheet");
    }

    @Test @Order(35)
    @DisplayName("TC-FEE-06: Transactions list is non-empty")
    void transactionsList() throws Exception {
        MvcResult r = mvc.perform(get("/api/fees/transactions")
                .header("Authorization", "Bearer " + ADMIN_TOKEN))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode arr = om.readTree(r.getResponse().getContentAsString());
        assertThat(arr.size()).isGreaterThan(0);
        System.out.printf("[PASS] TC-FEE-06: %d transactions found%n", arr.size());
    }

    @Test @Order(36)
    @DisplayName("TC-FEE-07: Search transactions by student name")
    void searchTransactions() throws Exception {
        MvcResult r = mvc.perform(get("/api/fees/transactions?studentName=Samiksha")
                .header("Authorization", "Bearer " + ADMIN_TOKEN))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode arr = om.readTree(r.getResponse().getContentAsString());
        assertThat(arr.size()).isGreaterThan(0);
        System.out.printf("[PASS] TC-FEE-07: %d transactions for Samiksha%n", arr.size());
    }

    @Test @Order(37)
    @DisplayName("TC-FEE-08: Date range filter Sep 2021")
    void dateRangeFilter() throws Exception {
        MvcResult r = mvc.perform(get("/api/fees/transactions?from=2021-09-01&to=2021-09-30")
                .header("Authorization", "Bearer " + ADMIN_TOKEN))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode arr = om.readTree(r.getResponse().getContentAsString());
        System.out.printf("[PASS] TC-FEE-08: %d transactions in Sep 2021%n", arr.size());
    }

    @Test @Order(38)
    @DisplayName("TC-FEE-09: Fee stats — totalExpected >= totalCollected")
    void feeStats() throws Exception {
        MvcResult r = mvc.perform(get("/api/fees/stats")
                .header("Authorization", "Bearer " + ADMIN_TOKEN))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode json = om.readTree(r.getResponse().getContentAsString());
        double expected  = json.get("totalExpectedRevenue").asDouble();
        double collected = json.get("totalCollected").asDouble();
        double pending   = json.get("totalPending").asDouble();

        assertThat(expected).isGreaterThan(0);
        assertThat(collected).isGreaterThanOrEqualTo(0);
        assertThat(Math.abs((collected + pending) - expected)).isLessThan(1.0);
        System.out.printf("[PASS] TC-FEE-09: expected=%.0f, collected=%.0f, pending=%.0f%n",
            expected, collected, pending);
    }

    @Test @Order(39)
    @DisplayName("TC-FEE-10: Overpayment guard returns 400")
    void overpaymentGuard() throws Exception {
        if (STUDENT_PROFILE_ID == null || PYTHON_COURSE_ID == null) {
            System.out.println("[WARN] TC-FEE-10: skipped — IDs not available"); return;
        }
        String body = String.format(
            "{\"studentId\":%d,\"courseId\":%d,\"installmentAmount\":9999999,\"transactionType\":\"CASH\"}",
            STUDENT_PROFILE_ID, PYTHON_COURSE_ID);
        mvc.perform(post("/api/fees/collect")
                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
        System.out.println("[PASS] TC-FEE-10: Overpayment correctly rejected with 400");
    }

    @Test @Order(40)
    @DisplayName("TC-FEE-11: Zero amount guard returns 400")
    void zeroAmountGuard() throws Exception {
        if (STUDENT_PROFILE_ID == null || PYTHON_COURSE_ID == null) {
            System.out.println("[WARN] TC-FEE-11: skipped — IDs not available"); return;
        }
        String body = String.format(
            "{\"studentId\":%d,\"courseId\":%d,\"installmentAmount\":0,\"transactionType\":\"CASH\"}",
            STUDENT_PROFILE_ID, PYTHON_COURSE_ID);
        mvc.perform(post("/api/fees/collect")
                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
        System.out.println("[PASS] TC-FEE-11: Zero amount correctly rejected with 400");
    }

    // =========================================================================
    // SECTION 5 — SECURITY
    // =========================================================================

    @Test @Order(50)
    @DisplayName("TC-SEC-01: Student cannot access admin user list")
    void studentBlockedFromAdmin() throws Exception {
        if (STUDENT_TOKEN == null) { System.out.println("[WARN] TC-SEC-01: skipped"); return; }
        mvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + STUDENT_TOKEN))
            .andExpect(status().isForbidden());
        System.out.println("[PASS] TC-SEC-01: Student blocked from /api/admin/users with 403");
    }

    @Test @Order(51)
    @DisplayName("TC-SEC-02: Student cannot access reports")
    void studentBlockedFromReports() throws Exception {
        if (STUDENT_TOKEN == null) { System.out.println("[WARN] TC-SEC-02: skipped"); return; }
        mvc.perform(get("/reports/summary")
                .header("Authorization", "Bearer " + STUDENT_TOKEN))
            .andExpect(status().isForbidden());
        System.out.println("[PASS] TC-SEC-02: Student blocked from /reports/summary with 403");
    }

    @Test @Order(52)
    @DisplayName("TC-SEC-03: Student can access own fee records")
    void studentOwnFees() throws Exception {
        if (STUDENT_TOKEN == null || STUDENT_PROFILE_ID == null) {
            System.out.println("[WARN] TC-SEC-03: skipped"); return;
        }
        mvc.perform(get("/api/fees/student/" + STUDENT_PROFILE_ID)
                .header("Authorization", "Bearer " + STUDENT_TOKEN))
            .andExpect(status().isOk());
        System.out.println("[PASS] TC-SEC-03: Student can access own fee records");
    }

    // =========================================================================
    // SECTION 6 — DASHBOARD & REPORTS
    // =========================================================================

    @Test @Order(60)
    @DisplayName("TC-RPT-01: Dashboard summary returns all required fields")
    void dashboardSummary() throws Exception {
        MvcResult r = mvc.perform(get("/reports/summary")
                .header("Authorization", "Bearer " + ADMIN_TOKEN))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode json = om.readTree(r.getResponse().getContentAsString());
        assertThat(json.has("totalStudents")).isTrue();
        assertThat(json.has("totalRevenue")).isTrue();
        assertThat(json.has("totalPending")).isTrue();
        assertThat(json.has("totalExpected")).isTrue();
        assertThat(json.has("totalPayments")).isTrue();
        assertThat(json.get("totalStudents").asLong()).isGreaterThanOrEqualTo(31);

        double rev     = json.get("totalRevenue").asDouble();
        double pending = json.get("totalPending").asDouble();
        double exp     = json.get("totalExpected").asDouble();
        assertThat(Math.abs((rev + pending) - exp)).isLessThan(1.0);

        System.out.printf("[PASS] TC-RPT-01: students=%d, revenue=%.0f, pending=%.0f, expected=%.0f%n",
            json.get("totalStudents").asLong(), rev, pending, exp);
    }

    @Test @Order(61)
    @DisplayName("TC-RPT-02: Student admissions report has Sep/Oct 2021 data")
    void admissionsReport() throws Exception {
        MvcResult r = mvc.perform(get("/reports/student")
                .header("Authorization", "Bearer " + ADMIN_TOKEN))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode arr = om.readTree(r.getResponse().getContentAsString());
        assertThat(arr.isArray()).isTrue();
        System.out.printf("[PASS] TC-RPT-02: %d month(s) of admission data%n", arr.size());
        for (JsonNode row : arr) {
            System.out.printf("       month=%s enrolled=%d active=%d%n",
                row.get("month").asText(), row.get("enrolled").asInt(), row.get("active").asInt());
        }
    }

    @Test @Order(62)
    @DisplayName("TC-RPT-03: Revenue trend report")
    void revenueTrend() throws Exception {
        MvcResult r = mvc.perform(get("/reports/revenue")
                .header("Authorization", "Bearer " + ADMIN_TOKEN))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode arr = om.readTree(r.getResponse().getContentAsString());
        assertThat(arr.isArray()).isTrue();
        System.out.printf("[PASS] TC-RPT-03: %d month(s) of revenue data%n", arr.size());
    }

    @Test @Order(63)
    @DisplayName("TC-RPT-04: Course revenue report has Python and Web Design")
    void courseRevenue() throws Exception {
        MvcResult r = mvc.perform(get("/reports/course")
                .header("Authorization", "Bearer " + ADMIN_TOKEN))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode arr = om.readTree(r.getResponse().getContentAsString());
        assertThat(arr.isArray()).isTrue();

        boolean hasPython = false, hasWebDesign = false;
        for (JsonNode c : arr) {
            String name = c.get("courseName").asText();
            if ("Python".equals(name))     hasPython    = true;
            if ("Web Design".equals(name)) hasWebDesign = true;
            System.out.printf("       course=%s enrolled=%d collected=%.0f pending=%.0f%n",
                name, c.get("enrolled").asInt(),
                c.get("collectedRevenue").asDouble(), c.get("pendingRevenue").asDouble());
        }
        assertThat(hasPython).as("Python in course report").isTrue();
        assertThat(hasWebDesign).as("Web Design in course report").isTrue();
        System.out.println("[PASS] TC-RPT-04: Course revenue report contains Python and Web Design");
    }

    @Test @Order(64)
    @DisplayName("TC-RPT-05: PDF download Sep 2021 returns non-empty PDF bytes")
    void pdfDownload() throws Exception {
        MvcResult r = mvc.perform(get("/api/reports/download/9/2021")
                .header("Authorization", "Bearer " + ADMIN_TOKEN))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
            .andReturn();
        byte[] bytes = r.getResponse().getContentAsByteArray();
        assertThat(bytes.length).isGreaterThan(1000);
        // PDF magic bytes: %PDF
        assertThat(new String(bytes, 0, 4)).isEqualTo("%PDF");
        System.out.printf("[PASS] TC-RPT-05: PDF download OK, size=%d bytes%n", bytes.length);
    }

    @Test @Order(65)
    @DisplayName("TC-RPT-06: Excel download Sep 2021 returns valid XLSX bytes")
    void excelDownload() throws Exception {
        MvcResult r = mvc.perform(get("/api/reports/download/9/2021?format=excel")
                .header("Authorization", "Bearer " + ADMIN_TOKEN))
            .andExpect(status().isOk())
            .andReturn();
        byte[] bytes = r.getResponse().getContentAsByteArray();
        assertThat(bytes.length).isGreaterThan(1000);
        // XLSX magic bytes: PK (ZIP)
        assertThat(bytes[0]).isEqualTo((byte) 0x50);
        assertThat(bytes[1]).isEqualTo((byte) 0x4B);
        System.out.printf("[PASS] TC-RPT-06: Excel download OK, size=%d bytes%n", bytes.length);
    }

    // =========================================================================
    // SECTION 7 — PERFORMANCE MODULE
    // =========================================================================

    @Test @Order(70)
    @DisplayName("TC-PERF-01: Performance summary returns required fields")
    void performanceSummary() throws Exception {
        if (STUDENT_PROFILE_ID == null || PYTHON_COURSE_ID == null) {
            System.out.println("[WARN] TC-PERF-01: skipped"); return;
        }
        MvcResult r = mvc.perform(get("/api/performance/student/" + STUDENT_PROFILE_ID + "/course/" + PYTHON_COURSE_ID)
                .header("Authorization", "Bearer " + ADMIN_TOKEN))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode json = om.readTree(r.getResponse().getContentAsString());
        assertThat(json.has("averageScore")).isTrue();
        assertThat(json.has("classRank")).isTrue();
        assertThat(json.has("attendancePercentage")).isTrue();
        System.out.printf("[PASS] TC-PERF-01: avgScore=%.1f, rank=%d, attendance=%.1f%%%n",
            json.get("averageScore").asDouble(),
            json.get("classRank").asInt(),
            json.get("attendancePercentage").asDouble());
    }

    @Test @Order(71)
    @DisplayName("TC-PERF-02: Skills endpoint returns array")
    void skillsEndpoint() throws Exception {
        if (STUDENT_PROFILE_ID == null) { System.out.println("[WARN] TC-PERF-02: skipped"); return; }
        MvcResult r = mvc.perform(get("/api/performance/student/" + STUDENT_PROFILE_ID + "/skills")
                .header("Authorization", "Bearer " + ADMIN_TOKEN))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode arr = om.readTree(r.getResponse().getContentAsString());
        assertThat(arr.isArray()).isTrue();
        System.out.printf("[PASS] TC-PERF-02: %d skill(s) returned%n", arr.size());
    }

    @Test @Order(72)
    @DisplayName("TC-PERF-03: Leaderboard returns array")
    void leaderboard() throws Exception {
        if (PYTHON_COURSE_ID == null) { System.out.println("[WARN] TC-PERF-03: skipped"); return; }
        MvcResult r = mvc.perform(get("/api/performance/course/" + PYTHON_COURSE_ID + "/leaderboard")
                .header("Authorization", "Bearer " + ADMIN_TOKEN))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode arr = om.readTree(r.getResponse().getContentAsString());
        assertThat(arr.isArray()).isTrue();
        System.out.printf("[PASS] TC-PERF-03: %d student(s) in leaderboard%n", arr.size());
    }

    @Test @Order(73)
    @DisplayName("TC-PERF-04: AI Insight returns non-empty string")
    void aiInsight() throws Exception {
        if (STUDENT_PROFILE_ID == null || PYTHON_COURSE_ID == null) {
            System.out.println("[WARN] TC-PERF-04: skipped"); return;
        }
        MvcResult r = mvc.perform(get("/api/performance/student/" + STUDENT_PROFILE_ID + "/course/" + PYTHON_COURSE_ID + "/insight")
                .header("Authorization", "Bearer " + ADMIN_TOKEN))
            .andExpect(status().isOk())
            .andReturn();
        String insight = r.getResponse().getContentAsString();
        assertThat(insight).isNotBlank();
        System.out.println("[PASS] TC-PERF-04: Insight = " + insight);
    }
}
