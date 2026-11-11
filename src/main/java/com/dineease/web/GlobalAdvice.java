package com.dineease.web;

import com.dineease.model.User;
import com.dineease.service.Slots;
import jakarta.servlet.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

/** Values every template can use: user, today, times, locs, uri. */
@ControllerAdvice
public class GlobalAdvice {
    @ModelAttribute("user") User user(HttpSession s) { return (User) s.getAttribute("user"); }
    @ModelAttribute("today") LocalDate today() { return LocalDate.now(); }
    @ModelAttribute("times") List<LocalTime> times() { return Slots.TIMES; }
    @ModelAttribute("uri") String uri(HttpServletRequest r) { return r.getRequestURI(); }
    @ModelAttribute("locs") Map<String, String> locs() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("indoor", "Indoor"); m.put("outdoor", "Outdoor"); m.put("window", "Window-side"); m.put("none", "No preference");
        return m;
    }
}
