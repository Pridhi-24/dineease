package com.dineease.web;

import com.dineease.model.*;
import com.dineease.repo.Repos;
import com.dineease.service.AvailabilityService;
import com.dineease.service.AvailabilityService.TableView;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.*;
import java.util.*;

@Controller
public class CustomerController {
    private static final List<String> ACTIVE = List.of("pending", "confirmed");
    private final AvailabilityService av;
    private final Repos.Tables tables; private final Repos.Reservations res; private final Repos.Waitlist wait; private final Repos.Users users;

    public CustomerController(AvailabilityService av, Repos.Tables t, Repos.Reservations r, Repos.Waitlist w, Repos.Users u) {
        this.av = av; tables = t; res = r; wait = w; users = u;
    }

    private Draft draft(HttpSession s) {
        Draft d = (Draft) s.getAttribute("draft");
        if (d == null) { d = new Draft(); s.setAttribute("draft", d); }
        return d;
    }
    private String step(Model m, HttpSession s, int n) { m.addAttribute("step", n); m.addAttribute("draft", draft(s)); return "reserve"; }
    private User me(HttpSession s) { return (User) s.getAttribute("user"); }

    // ---------- 5-step reservation flow ----------
    @GetMapping("/reserve") String step1(Model m, HttpSession s) { return step(m, s, 1); }

    @PostMapping("/reserve/details")
    String saveDetails(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime time, @RequestParam int party,
                       @RequestParam(defaultValue = "") String occasion, @RequestParam(defaultValue = "") String requests,
                       Model m, HttpSession s) {
        Map<String, String> e = new HashMap<>();
        if (date == null || date.isBefore(LocalDate.now())) e.put("date", "Choose today or a future date.");
        else if (date.equals(LocalDate.now()) && !time.isAfter(av.nowSlot())) e.put("time", "That time has passed. Choose a later slot.");
        if (party < 1 || party > 12) e.put("party", "Parties of 1-12 can book online.");
        if (!e.isEmpty()) { m.addAttribute("errors", e); return step(m, s, 1); }
        Draft d = draft(s);
        d.date = date; d.time = time; d.party = party; d.occasion = occasion; d.requests = requests; d.tableId = null;
        return "redirect:/reserve/seating";
    }

    @GetMapping("/reserve/seating") String step2(Model m, HttpSession s) { return step(m, s, 2); }

    @PostMapping("/reserve/seating")
    String saveSeating(@RequestParam String pref, HttpSession s) { draft(s).pref = pref; return "redirect:/reserve/tables"; }

    @GetMapping("/reserve/tables")
    String step3(Model m, HttpSession s) {
        Draft d = draft(s);
        List<TableView> views = av.check(d.date, d.time, d.party, d.pref, null);
        m.addAttribute("views", views);
        m.addAttribute("anyOk", views.stream().anyMatch(v -> v.ok));
        return step(m, s, 3);
    }

    @PostMapping("/reserve/select")
    String select(@RequestParam Long tableId, HttpSession s) { draft(s).tableId = tableId; return "redirect:/reserve/summary"; }

    @GetMapping("/reserve/summary")
    String step4(Model m, HttpSession s) {
        Draft d = draft(s);
        if (d.tableId == null) return "redirect:/reserve/tables";
        m.addAttribute("table", tables.findById(d.tableId).orElseThrow());
        return step(m, s, 4);
    }

    @PostMapping("/reserve/confirm")
    String confirm(HttpSession s, RedirectAttributes ra) {
        Draft d = draft(s);
        if (d.tableId == null) return "redirect:/reserve";
        boolean free = av.check(d.date, d.time, d.party, d.pref, null).stream()
            .anyMatch(v -> v.table.tableId.equals(d.tableId) && v.state.equals("available")); // re-check: never double-book
        if (!free) { ra.addFlashAttribute("error", "That table was just taken. Please choose another."); return "redirect:/reserve/tables"; }
        Reservation r = new Reservation();
        r.userId = me(s).userId; r.tableId = d.tableId; r.reservationDate = d.date; r.reservationTime = d.time; r.partySize = d.party;
        r.seatingPreference = d.pref; r.occasion = d.occasion; r.specialRequests = d.requests;
        r = res.save(r);
        s.removeAttribute("draft");
        return "redirect:/reserve/done/" + r.reservationId;
    }

    @GetMapping("/reserve/done/{id}")
    String step5(@PathVariable Long id, Model m, HttpSession s) {
        Reservation r = res.findById(id).orElseThrow();
        if (!r.userId.equals(me(s).userId)) return "redirect:/dashboard";
        m.addAttribute("r", r);
        return step(m, s, 5);
    }

    // ---------- waitlist ----------
    @GetMapping("/waitlist")
    String waitlist(Model m, HttpSession s) {
        User u = me(s);
        m.addAttribute("mine", u == null ? List.of() : wait.findByUserIdOrderByCreatedAtDesc(u.userId));
        return "waitlist";
    }

    @PostMapping("/waitlist/join")
    String join(@RequestParam int party, @RequestParam String pref, HttpSession s, RedirectAttributes ra) {
        if (party < 1 || party > 12) { ra.addFlashAttribute("error", "Enter 1-12 guests."); return "redirect:/waitlist"; }
        WaitlistEntry w = new WaitlistEntry();
        w.userId = me(s).userId; w.partySize = party; w.seatingPreference = pref;
        wait.save(w);
        ra.addFlashAttribute("msg", "You are on the waitlist.");
        return "redirect:/waitlist";
    }

    @PostMapping("/waitlist/{id}/cancel")
    String leave(@PathVariable Long id, HttpSession s, RedirectAttributes ra) {
        WaitlistEntry w = wait.findById(id).orElseThrow();
        if (w.userId != null && w.userId.equals(me(s).userId)) { w.status = "Cancelled"; wait.save(w); ra.addFlashAttribute("msg", "Waitlist entry cancelled."); }
        return "redirect:/waitlist";
    }

    // ---------- dashboard ----------
    @GetMapping("/dashboard")
    String dashboard(Model m, HttpSession s) {
        User u = me(s);
        List<Reservation> all = res.findByUserIdOrderByReservationDateDescReservationTimeDesc(u.userId);
        LocalDate t = LocalDate.now();
        List<Reservation> up = new ArrayList<>(all.stream().filter(r -> !r.reservationDate.isBefore(t) && ACTIVE.contains(r.status)).toList());
        Collections.reverse(up); // soonest first
        List<Reservation> past = all.stream().filter(r -> !up.contains(r)).toList();
        m.addAttribute("upcoming", up); m.addAttribute("past", past);
        m.addAttribute("waits", wait.findByUserIdOrderByCreatedAtDesc(u.userId));
        return "dashboard";
    }

    @GetMapping("/reservations/{id}")
    String view(@PathVariable Long id, Model m, HttpSession s) {
        Reservation r = res.findById(id).orElseThrow();
        if (!r.userId.equals(me(s).userId)) return "redirect:/dashboard";
        m.addAttribute("r", r);
        return "reservation";
    }

    @PostMapping("/reservations/{id}/cancel")
    String cancel(@PathVariable Long id, HttpSession s, RedirectAttributes ra) {
        Reservation r = res.findById(id).orElseThrow();
        if (r.userId.equals(me(s).userId) && ACTIVE.contains(r.status)) { r.status = "cancelled"; res.save(r); ra.addFlashAttribute("msg", "Reservation cancelled."); }
        return "redirect:/dashboard";
    }

    @PostMapping("/profile")
    String profile(@RequestParam String fullName, @RequestParam String phone, HttpSession s, RedirectAttributes ra) {
        if (fullName.trim().length() < 2 || !phone.matches("^\\+?[\\d\\s-]{8,15}$")) { ra.addFlashAttribute("error", "Enter a valid name and phone number."); return "redirect:/dashboard"; }
        User u = users.findById(me(s).userId).orElseThrow();
        u.fullName = fullName.trim(); u.phone = phone;
        s.setAttribute("user", users.save(u));
        ra.addFlashAttribute("msg", "Profile updated.");
        return "redirect:/dashboard";
    }
}
