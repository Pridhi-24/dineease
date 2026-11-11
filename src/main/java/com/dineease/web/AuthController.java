package com.dineease.web;

import com.dineease.model.User;
import com.dineease.repo.Repos;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.*;
import java.util.regex.Pattern;

@Controller
public class AuthController {
    static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[\\d\\s-]{8,15}$");
    private static final Pattern PASSWORD = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");
    private final Repos.Users users;
    private final BCryptPasswordEncoder enc = new BCryptPasswordEncoder(); // passwords are never stored in plain text

    public AuthController(Repos.Users users) { this.users = users; }

    @GetMapping("/login") String login() { return "login"; }
    @GetMapping("/register") String register() { return "register"; }

    @PostMapping("/login")
    String doLogin(@RequestParam String email, @RequestParam String password, @RequestParam(required = false) String remember,
                   HttpSession s, Model m, RedirectAttributes ra) {
        Map<String, String> e = new HashMap<>();
        if (!EMAIL.matcher(email).matches()) e.put("email", "Enter a valid email address.");
        if (password.isEmpty()) e.put("password", "Password is required.");
        User u = null;
        if (e.isEmpty()) {
            u = users.findByEmailIgnoreCase(email).orElse(null);
            if (u == null || !enc.matches(password, u.passwordHash)) e.put("password", "Incorrect email or password.");
        }
        if (!e.isEmpty()) { m.addAttribute("errors", e); m.addAttribute("f", Map.of("email", email)); return "login"; }
        s.setAttribute("user", u);
        s.setMaxInactiveInterval(remember != null ? 7 * 24 * 3600 : 1800);
        String next = (String) s.getAttribute("next");
        s.removeAttribute("next");
        ra.addFlashAttribute("msg", "Welcome back, " + u.fullName.split(" ")[0] + "!");
        return "redirect:" + ("admin".equals(u.role) ? "/admin" : next != null ? next : "/dashboard");
    }

    @PostMapping("/register")
    String doRegister(@RequestParam String fullName, @RequestParam String email, @RequestParam String phone,
                      @RequestParam String password, @RequestParam String confirm, HttpSession s, Model m, RedirectAttributes ra) {
        Map<String, String> e = new HashMap<>();
        if (fullName.trim().length() < 2) e.put("fullName", "Please enter your full name.");
        if (!EMAIL.matcher(email).matches()) e.put("email", "Enter a valid email address.");
        else if (users.findByEmailIgnoreCase(email).isPresent()) e.put("email", "An account with this email already exists.");
        if (!PHONE.matcher(phone).matches()) e.put("phone", "Enter a valid phone number.");
        if (!PASSWORD.matcher(password).matches()) e.put("password", "Use 8+ characters with letters and numbers.");
        if (!confirm.equals(password)) e.put("confirm", "Passwords do not match.");
        if (!e.isEmpty()) { m.addAttribute("errors", e); m.addAttribute("f", Map.of("fullName", fullName, "email", email, "phone", phone)); return "register"; }
        User u = new User();
        u.fullName = fullName.trim(); u.email = email.toLowerCase(); u.phone = phone; u.passwordHash = enc.encode(password); u.role = "customer";
        s.setAttribute("user", users.save(u));
        ra.addFlashAttribute("msg", "Account created. Welcome to DineEase!");
        String next = (String) s.getAttribute("next");
        s.removeAttribute("next");
        return "redirect:" + (next != null ? next : "/dashboard");
    }

    @GetMapping("/logout")
    String logout(HttpSession s) {
        s.invalidate();
        return "redirect:/";
    }
}
