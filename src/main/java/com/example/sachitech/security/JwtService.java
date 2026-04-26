package com.example.sachitech.security;

import com.example.sachitech.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    private final String SECRET = "sachitech-secret-key-12345678901234567123";

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    // ✅ FIXED TOKEN GENERATION
    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())   // ✅ FIXED
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    // ✅ Extract Email
    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // ✅ Validate Token
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return (email.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // ✅ Check Expiry
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ✅ Extract Expiration
    private Date extractExpiration(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
    }
}