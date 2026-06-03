package com.mindcompanion.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class FrontendController {

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
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("activePage", "dashboard");
        return "dashboard";
    }

    @GetMapping("/chat")
    public String chat(Model model) {
        model.addAttribute("pageTitle", "Chat with Serenity");
        model.addAttribute("activePage", "chat");
        return "chat";
    }

    @GetMapping("/mood")
    public String mood(Model model) {
        model.addAttribute("pageTitle", "Mood Check-in");
        model.addAttribute("activePage", "mood");
        return "mood";
    }

    @GetMapping("/journal")
    public String journal(Model model) {
        model.addAttribute("pageTitle", "My Journal");
        model.addAttribute("activePage", "journal");
        return "journal";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("pageTitle", "Profile & Settings");
        model.addAttribute("activePage", "profile");
        return "profile";
    }
}