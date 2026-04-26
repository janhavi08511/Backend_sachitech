package com.example.sachitech.component;

import com.example.sachitech.entity.Role;
import com.example.sachitech.entity.User;
import com.example.sachitech.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        String adminEmail = "admin@gmail.com";
        String adminPassword = "admin123";
        Optional<User> adminUserOpt = userRepository.findByEmail(adminEmail);

        if (adminUserOpt.isEmpty()) {
            User adminUser = new User();
            adminUser.setName("System Admin");
            adminUser.setEmail(adminEmail);
            adminUser.setPassword(passwordEncoder.encode(adminPassword));
            adminUser.setRole(Role.ADMIN);
            userRepository.save(adminUser);
            System.out.println("✅ Admin user created: " + adminEmail);
        } else {
            // Fix plain-text passwords left from before BCrypt was enforced
            User existing = adminUserOpt.get();
            if (!existing.getPassword().startsWith("$2")) {
                existing.setPassword(passwordEncoder.encode(adminPassword));
                userRepository.save(existing);
                System.out.println("✅ Admin password re-encoded with BCrypt.");
            } else {
                System.out.println("⚡ Admin user already exists. Skipping seed.");
            }
        }

        // ✅ Fix ALL existing users whose passwords are plain-text (not BCrypt)
        // This handles students/trainers created before BCrypt encoding was enforced
        List<User> allUsers = userRepository.findAll();
        int fixed = 0;
        for (User u : allUsers) {
            if (!u.getPassword().startsWith("$2")) {
                // Password is plain-text — we can re-encode it as-is
                String plainPassword = u.getPassword();
                u.setPassword(passwordEncoder.encode(plainPassword));
                userRepository.save(u);
                fixed++;
                System.out.println("✅ Re-encoded password for user: " + u.getEmail());
            }
        }
        if (fixed > 0) {
            System.out.println("✅ Total passwords re-encoded: " + fixed);
        }
    }
}
