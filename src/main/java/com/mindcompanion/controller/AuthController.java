package com.mindcompanion.controller;

import com.mindcompanion.dto.request.LoginRequest;
import com.mindcompanion.dto.request.RegisterRequest;
import com.mindcompanion.dto.response.JwtResponse;
import com.mindcompanion.dto.response.MessageResponse;
import com.mindcompanion.model.User;
import com.mindcompanion.model.enums.Role;
import com.mindcompanion.repository.UserRepository;
import com.mindcompanion.security.UserDetailsImpl;
import com.mindcompanion.security.jwt.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    // ─── REGISTER ───────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(
            @Valid @RequestBody RegisterRequest request) {

        // Check username taken
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(
                            "Error: Username is already taken!"));
        }

        // Check email taken
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(
                            "Error: Email is already in use!"));
        }

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(Role.PATIENT)
                .build();

        userRepository.save(user);

        return ResponseEntity.ok(
                new MessageResponse("User registered successfully!"));
    }

    // ─── LOGIN ──────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(
            @Valid @RequestBody LoginRequest request) {

        // Authenticate with Spring Security
        Authentication authentication = authenticationManager
                .authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getUsername(),
                                request.getPassword()
                        )
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        // Generate JWT token
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails =
                (UserDetailsImpl) authentication.getPrincipal();

        String role = userDetails.getAuthorities()
                .stream()
                .findFirst()
                .map(item -> item.getAuthority())
                .orElse("");

        return ResponseEntity.ok(new JwtResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                role
        ));
    }
}