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
import com.mindcompanion.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;

    // ─── REGISTER ───────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(
            @Valid @RequestBody RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(Role.PATIENT)
                .emailVerified(false)
                .verificationToken(verificationToken)
                .build();

        userRepository.save(user);

        // Send verification email
        try {
            emailService.sendVerificationEmail(
                    request.getEmail(),
                    request.getUsername(),
                    verificationToken);
            log.info("Verification email sent to {}", request.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email: {}", e.getMessage());
        }

        return ResponseEntity.ok(new MessageResponse(
                "Registration successful! Please check your email to verify your account."));
    }

    // ─── VERIFY EMAIL ────────────────────────────────
    @GetMapping("/verify")
    public org.springframework.web.servlet.view.RedirectView verifyEmail(
            @RequestParam String token) {
        return userRepository.findByVerificationToken(token)
                .map(user -> {
                    user.setEmailVerified(true);
                    user.setVerificationToken(null);
                    userRepository.save(user);
                    log.info("Email verified for user '{}'", user.getUsername());
                    return new org.springframework.web.servlet.view.RedirectView(
                            "/login?success=Email verified! You can now log in.");
                })
                .orElse(new org.springframework.web.servlet.view.RedirectView(
                        "/login?error=Invalid or expired verification link."));
    }

    // ─── LOGIN ──────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(
            @Valid @RequestBody LoginRequest request) {

        // Check email verified before authenticating
        userRepository.findByUsername(request.getUsername()).ifPresent(user -> {
            // null means registered before verification was added — allow login
            if (user.getEmailVerified() != null && Boolean.FALSE.equals(user.getEmailVerified())) {
                throw new RuntimeException("EMAIL_NOT_VERIFIED");
            }
        });

        try {
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(
                            request.getUsername(), request.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            String role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(item -> item.getAuthority())
                    .orElse("");

            return ResponseEntity.ok(new JwtResponse(
                    jwt, userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getEmail(), role));

        } catch (RuntimeException e) {
            if ("EMAIL_NOT_VERIFIED".equals(e.getMessage())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse(
                                "Please verify your email before logging in. Check your inbox."));
            }
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Invalid username or password."));
        }
    }
}