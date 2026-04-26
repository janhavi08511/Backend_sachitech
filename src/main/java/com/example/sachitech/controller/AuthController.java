package com.example.sachitech.controller;

import com.example.sachitech.entity.User;
import com.example.sachitech.repository.StudentProfileRepository;
import com.example.sachitech.repository.UserRepository;
import com.example.sachitech.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        String token = jwtService.generateToken(user);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("userId", user.getId());
        response.put("email", user.getEmail());
        response.put("name", user.getName());
        response.put("role", user.getRole().name());

        // Include studentProfileId if user is a student
        if (user.getRole().name().equals("STUDENT")) {
            studentProfileRepository.findByUserId(user.getId())
                    .ifPresent(sp -> response.put("studentProfileId", sp.getId()));
        }

        return ResponseEntity.ok(response);
    }
}
