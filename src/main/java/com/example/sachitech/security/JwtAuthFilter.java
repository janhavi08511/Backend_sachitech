package com.example.sachitech.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@org.springframework.lang.NonNull HttpServletRequest request,
                                    @org.springframework.lang.NonNull HttpServletResponse response,
                                    @org.springframework.lang.NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            final String authHeader = request.getHeader("Authorization");

            // ❌ No token → skip (don't block publicly accessible endpoints)
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = authHeader.substring(7);

            // Validate if the string even looks like a JWT before parsing
            if (token.chars().filter(ch -> ch == '.').count() != 2) {
                System.out.println("⚠️ Malformed JWT detected");
                filterChain.doFilter(request, response);
                return;
            }

            String email = jwtService.extractEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // ✅ Validate token
                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    System.out.println("✅ JWT validated for: " + email);
                } else {
                    System.out.println("⚠️ JWT token invalid for: " + email);
                }
            }

        } catch (JwtException e) {
            System.out.println("⚠️ JWT Error: " + e.getMessage());
            // Don't throw - let it continue without authentication
            // Spring will handle the authorization check
        } catch (Exception e) {
            System.out.println("⚠️ Filter Exception: " + e.getMessage());
            e.printStackTrace();
        }

        filterChain.doFilter(request, response);
    }
}