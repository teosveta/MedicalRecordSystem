package com.medicalrecord.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// Сервира HTML страниците — данните се зареждат от REST API чрез JavaScript
@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/doctor/dashboard")
    public String doctorDashboard() {
        return "doctor/dashboard";
    }

    @GetMapping("/patient/dashboard")
    public String patientDashboard() {
        return "patient/dashboard";
    }
}
