package com.example.sachitech.component;

import com.example.sachitech.entity.*;
import com.example.sachitech.repository.*;
import com.example.sachitech.service.FeeService;
import com.example.sachitech.service.FeeService.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

/**
 * Seeds the 31 students from DataSheet.xlsx (Admission tab) into the database.
 * Runs after DatabaseSeeder (Order 2).
 * Idempotent — skips rows that already exist by email.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class DataSheetSeeder implements CommandLineRunner {

    private final UserRepository            userRepo;
    private final StudentProfileRepository  profileRepo;
    private final CourseRepository          courseRepo;
    private final FeeService                feeService;
    private final PasswordEncoder           encoder;

    // ── DataSheet row ────────────────────────────────────────────────────────
    record Row(
        String name, String email, String phone,
        String courseName, String admissionDate,
        double totalFee,
        List<Inst> installments
    ) {}
    record Inst(String date, double amount) {}

    @Override
    public void run(String... args) {

        // ── 1. Ensure courses exist ──────────────────────────────────────────
        Map<String, Long> courseIds = ensureCourses();

        // ── 2. DataSheet rows ────────────────────────────────────────────────
        List<Row> rows = buildRows();

        int seeded = 0, skipped = 0, txSeeded = 0;

        for (Row r : rows) {
            if (userRepo.findByEmail(r.email()).isPresent()) {
                skipped++;
                continue;
            }

            // Create user
            User u = new User();
            u.setName(r.name());
            u.setEmail(r.email());
            u.setPassword(encoder.encode("Student@123"));
            u.setRole(Role.STUDENT);
            u = userRepo.save(u);

            // Create student profile
            StudentProfile sp = new StudentProfile();
            sp.setUser(u);
            sp.setPhone(r.phone());
            sp.setCourse(r.courseName());
            sp.setAdmissionDate(r.admissionDate() != null ? LocalDate.parse(r.admissionDate()) : null);
            sp.setFeePaid(false);
            sp = profileRepo.save(sp);

            // Enroll in course + post installments
            Long courseId = courseIds.get(r.courseName());
            if (courseId != null) {
                try {
                    feeService.createFeeRecord(sp.getId(), courseId);
                    for (Inst inst : r.installments()) {
                        CollectInstallmentRequest req = new CollectInstallmentRequest(
                            sp.getId(), courseId,
                            inst.amount(),
                            "CASH",
                            "RCP-" + r.email().split("@")[0] + "-" + System.nanoTime(),
                            LocalDate.parse(inst.date())
                        );
                        feeService.collectInstallment(req);
                        txSeeded++;
                    }
                    // Mark feePaid if fully settled
                    if (r.installments().stream().mapToDouble(Inst::amount).sum() >= r.totalFee()) {
                        sp.setFeePaid(true);
                        profileRepo.save(sp);
                    }
                } catch (Exception e) {
                    log.warn("Fee seeding failed for {}: {}", r.name(), e.getMessage());
                }
            }
            seeded++;
        }

        log.info("DataSheetSeeder: seeded={}, skipped={}, transactions={}", seeded, skipped, txSeeded);
    }

    // ── Course setup ─────────────────────────────────────────────────────────
    private Map<String, Long> ensureCourses() {
        record CourseDef(String name, double fee, String category, String duration) {}
        List<CourseDef> defs = List.of(
            new CourseDef("Python",         5000,  "Programming", "3 months"),
            new CourseDef("Web Design",     14000, "Web",         "4 months"),
            new CourseDef("C/C++",          5000,  "Programming", "3 months"),
            new CourseDef("Web Internship", 3000,  "Internship",  "2 months"),
            new CourseDef("C,C++,Python",   10000, "Programming", "5 months"),
            new CourseDef("CCNA Exam Prep", 5000,  "Networking",  "2 months"),
            new CourseDef("Red Hat",        10000, "Linux",       "2 months")
        );

        Map<String, Long> ids = new HashMap<>();
        for (CourseDef d : defs) {
            courseRepo.findAll().stream()
                .filter(c -> c.getName() != null && c.getName().equalsIgnoreCase(d.name()))
                .findFirst()
                .ifPresentOrElse(
                    c -> ids.put(d.name(), c.getId()),
                    () -> {
                        Course c = new Course();
                        c.setName(d.name());
                        c.setTotalFee(d.fee());
                        c.setCategory(d.category());
                        c.setDuration(d.duration());
                        c.setStatus("ACTIVE");
                        c.setDescription(d.name() + " course");
                        ids.put(d.name(), courseRepo.save(c).getId());
                    }
                );
        }
        return ids;
    }

    // ── All 31 rows from DataSheet ────────────────────────────────────────────
    private List<Row> buildRows() {
        return List.of(
            new Row("Samiksha Balasaheb Ugale",    "samiksha.ugale@test.com",    "9689544130", "Python",         "2021-09-08", 5000,
                List.of(new Inst("2021-09-16",2000), new Inst("2021-10-04",2000))),
            new Row("Mrunmai Rajesh Suryavanshi",  "mrunmai.surya@test.com",     "8446405165", "Python",         "2021-09-08", 4500,
                List.of(new Inst("2021-09-22",2500), new Inst("2021-10-08",2000))),
            new Row("Saveri Milind Waghmode",      "saveri.waghmode@test.com",   "9405839240", "Python",         "2021-09-08", 4500,
                List.of(new Inst("2021-09-15",4500))),
            new Row("Kamini Harshal Gaikwad",      "kamini.gaikwad@test.com",    "9112452031", "Web Design",     "2021-09-13", 14000,
                List.of(new Inst("2021-09-30",6000))),
            new Row("Kashish Prashant Nikam",      "kashish.nikam@test.com",     "9356938067", "Python",         "2021-09-13", 5000,
                List.of(new Inst("2021-09-15",2000))),
            new Row("Akanksha Avinash Navale",     "akanksha.navale@test.com",   "8208823934", "Python",         "2021-09-13", 5000,
                List.of(new Inst("2021-09-15",2000))),
            new Row("Vaishnavi Balasaheb Sonwane", "vaishnavi.sonwane@test.com", "9175657323", "C/C++",          "2021-09-13", 5000,
                List.of()),
            new Row("Devraj Shriram Shimpi",       "devraj.shimpi@test.com",     "7218938678", "Python",         "2021-09-13", 5000,
                List.of(new Inst("2021-09-15",2000))),
            new Row("Rushikesh Bajirao Nikumbh",   "rushikesh.nikumbh@test.com", "7066297548", "C/C++",          "2021-09-20", 5000,
                List.of(new Inst("2021-09-20",3000))),
            new Row("Chetan Sharad Salunke",       "chetan.salunke@test.com",    "9804048412", "C/C++",          "2021-09-20", 5000,
                List.of()),
            new Row("Harshal Sampat Gaikar",       "harshal.gaikar@test.com",    "7378699600", "C/C++",          "2021-09-20", 5000,
                List.of()),
            new Row("Abhishek Sunil Niphade",      "abhishek.niphade@test.com",  "8669881714", "Web Internship", "2021-09-07", 3000,
                List.of()),
            new Row("Krishna Santosh Kharde",      "krishna.kharde@test.com",    "9373518243", "Web Internship", "2021-09-07", 3000,
                List.of()),
            new Row("Sakshi Shravan Chavan",       "sakshi.chavan@test.com",     "7507911002", "Web Internship", "2021-09-07", 3000,
                List.of(new Inst("2021-10-06",3000))),
            new Row("Payal Sanjay Bhangare",       "payal.bhangare@test.com",    "8669064578", "Web Internship", "2021-09-07", 3000,
                List.of()),
            new Row("Aditya Rajendra Karle",       "aditya.karle@test.com",      "9112157415", "Web Internship", "2021-09-07", 3000,
                List.of()),
            new Row("Jayesh Bhalchandra Borase",   "jayesh.borase@test.com",     "9665106506", "C,C++,Python",   "2021-09-29", 10000,
                List.of()),
            new Row("Kajal Ramrao Gaikar",         "kajal.gaikar@test.com",      "7083054566", "C,C++,Python",   "2021-09-20", 10000,
                List.of()),
            new Row("Samruddhi Vilas Birari",      "samruddhi.birari@test.com",  "9172734822", "C,C++,Python",   "2021-09-20", 10000,
                List.of(new Inst("2021-09-30",5000))),
            new Row("Manasi Dattatrey Gawali",     "manasi.gawali@test.com",     "9673698806", "C,C++,Python",   "2021-09-20", 10000,
                List.of(new Inst("2021-09-29",5000))),
            new Row("Divya Ravindra Patil",        "divya.patil@test.com",       "8010493433", "Python",         "2021-09-23", 5000,
                List.of()),
            new Row("Kalyani Shekhar Kapadnis",    "kalyani.kapadnis@test.com",  "8177910120", "Python",         "2021-09-23", 5000,
                List.of()),
            new Row("Aasavari Harshal Madiwale",   "aasavari.madiwale@test.com", "8010166967", "Python",         "2021-09-22", 5000,
                List.of()),
            new Row("Pranav Madhav Thakare",       "pranav.thakare@test.com",    "9637979102", "Python",         "2021-09-22", 5000,
                List.of()),
            new Row("Aaditya Gopal Chitode",       "aaditya.chitode@test.com",   "9822370289", "Python",         "2021-09-22", 5000,
                List.of()),
            new Row("Tejas Bhat",                  "tejas.bhat@test.com",        "0000000001", "CCNA Exam Prep", "2021-10-01", 5000,
                List.of(new Inst("2021-10-01",3000))),
            new Row("Aman Bendkoli",               "aman.bendkoli@test.com",     "0000000002", "Web Design",     "2021-10-01", 15000,
                List.of(new Inst("2021-10-03",5000))),
            new Row("Ankush Gangurde",             "ankush.gangurde@test.com",   "0000000003", "CCNA Exam Prep", "2021-10-01", 5000,
                List.of(new Inst("2021-10-08",5000))),
            new Row("Aniket Gaikwad",              "aniket.gaikwad@test.com",    "0000000004", "C,C++,Python",   "2021-10-01", 10000,
                List.of(new Inst("2021-10-09",3000))),
            new Row("Aditya Shimpi",               "aditya.shimpi@test.com",     "0000000005", "Red Hat",        "2021-10-01", 10000,
                List.of(new Inst("2021-10-09",3000))),
            new Row("Akashay Rayate",              "akashay.rayate@test.com",    "0000000006", "Python",         "2021-10-10", 6000,
                List.of(new Inst("2021-10-10",3000)))
        );
    }
}
