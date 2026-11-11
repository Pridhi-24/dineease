package com.dineease.web;

import com.dineease.service.Menu;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.*;

@Controller
public class PublicController {
    @GetMapping("/") String home(@RequestParam(defaultValue = "Starters") String cat, Model m) { fillMenu(m, cat, "/"); return "home"; }
    @GetMapping("/menu") String menu(@RequestParam(defaultValue = "Starters") String cat, Model m) { fillMenu(m, cat, "/menu"); return "menu"; }
    @GetMapping("/about") String about() { return "about"; }
    @GetMapping("/contact") String contact() { return "contact"; }

    @PostMapping("/contact")
    String send(@RequestParam String name, @RequestParam String email, @RequestParam String message, Model m, RedirectAttributes ra) {
        Map<String, String> e = new HashMap<>();
        if (name.trim().length() < 2) e.put("name", "Please enter your name.");
        if (!AuthController.EMAIL.matcher(email).matches()) e.put("email", "Enter a valid email address.");
        if (message.trim().length() < 10) e.put("message", "Please write at least 10 characters.");
        if (!e.isEmpty()) { m.addAttribute("errors", e); m.addAttribute("f", Map.of("name", name, "email", email, "message", message)); return "contact"; }
        ra.addFlashAttribute("msg", "Thank you! Your message was sent (demo: not actually emailed).");
        return "redirect:/contact";
    }

    private void fillMenu(Model m, String cat, String base) {
        if (!Menu.CATS.contains(cat)) cat = "Starters";
        m.addAttribute("cats", Menu.CATS); m.addAttribute("cat", cat); m.addAttribute("base", base);
        m.addAttribute("items", Menu.of(cat)); m.addAttribute("grad", Menu.GRADIENT.get(cat));
    }
}
