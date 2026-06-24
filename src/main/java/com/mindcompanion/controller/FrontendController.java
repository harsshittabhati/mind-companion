package com.mindcompanion.controller;

import com.mindcompanion.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class FrontendController {

    private final UserRepository userRepository;

    public FrontendController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private void addUserInfo(Model model, UserDetails userDetails) {
        System.out.println("DEBUG userDetails: " + userDetails);
        if (userDetails == null) return;
        userRepository.findByUsername(userDetails.getUsername()).ifPresent(user -> {
            String fullName = user.getFullName();
            model.addAttribute("fullName", (fullName != null && !fullName.isBlank())
                    ? fullName : user.getUsername());
            model.addAttribute("avatarInitial",
                    (fullName != null && !fullName.isBlank())
                            ? String.valueOf(fullName.charAt(0)).toUpperCase()
                            : String.valueOf(user.getUsername().charAt(0)).toUpperCase());
        });
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String success,
            Model model) {
        if (error != null) model.addAttribute("error", "Invalid username or password.");
        if (success != null) model.addAttribute("success", "Registration successful! Please log in.");
        return "auth/login";
    }

    @GetMapping("/register")
    public String register() {
        return "auth/register";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("activePage", "dashboard");
        addUserInfo(model, userDetails);
        return "dashboard";
    }

    @GetMapping("/chat")
    public String chat(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        model.addAttribute("pageTitle", "Chat with Serenity");
        model.addAttribute("activePage", "chat");
        addUserInfo(model, userDetails);
        return "chat";
    }

    @GetMapping("/mood")
    public String mood(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        model.addAttribute("pageTitle", "Mood Check-in");
        model.addAttribute("activePage", "mood");
        addUserInfo(model, userDetails);
        return "mood";
    }

    @GetMapping("/journal")
    public String journal(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        model.addAttribute("pageTitle", "My Journal");
        model.addAttribute("activePage", "journal");
        addUserInfo(model, userDetails);
        return "journal";
    }

    @GetMapping("/profile")
    public String profile(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        model.addAttribute("pageTitle", "Profile & Settings");
        model.addAttribute("activePage", "profile");
        addUserInfo(model, userDetails);
        return "profile";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPassword(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("token", token);
        return "auth/reset-password";
    }
}