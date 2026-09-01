package com.gvp.marifariyaad.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves every page of the Mari-Fariyaad portal.
 *
 * The existing templates link to each other using paths like "/complaint.html",
 * "/login.html", etc. (carried over from the original static frontend), so those
 * exact paths are mapped here rather than "clean" Spring-style routes, to avoid
 * touching any template markup.
 */
@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/index.html")
    public String index() {
        return "index";
    }

    @GetMapping("/about.html")
    public String about() {
        return "about";
    }

    @GetMapping("/complaint.html")
    public String complaint() {
        return "complaint";
    }

    @GetMapping("/track.html")
    public String track() {
        return "track";
    }

    @GetMapping("/login.html")
    public String login() {
        return "login";
    }

    @GetMapping("/admin-login.html")
    public String adminLogin() {
        return "admin-login";
    }

    @GetMapping("/register.html")
    public String register() {
        return "register";
    }

    @GetMapping("/forgot-password.html")
    public String forgotPassword() {
        return "forgot-password";
    }

    @GetMapping("/dashboard.html")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/admin-dashboard.html")
    public String adminDashboard() {
        return "admin-dashboard";
    }

    @GetMapping("/departments.html")
    public String departments() {
        return "departments";
    }

    @GetMapping("/hostels.html")
    public String hostels() {
        return "hostels";
    }

    @GetMapping("/profile.html")
    public String profile() {
        return "profile";
    }

    @GetMapping("/faq.html")
    public String faq() {
        return "faq";
    }

    @GetMapping("/contact.html")
    public String contact() {
        return "contact";
    }
}
