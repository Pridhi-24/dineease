package com.dineease.web;

import com.dineease.model.*;
import com.dineease.repo.Repos;
import com.dineease.service.AvailabilityService;
import com.dineease.service.AvailabilityService.TableView;
import com.dineease.service.Fmt;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private static final List<String> ACTIVE = List.of("pending", "confirmed");
    private static final List<String> RES_STATUS = List.of("pending", "confirmed", "completed", "cancelled");
    private static final List<String> WL_STATUS = List.of("Waiting", "Table Available", "Notified", "Seated", "Cancelled");
    private final AvailabilityService av; private final Fmt fmt;
    private final Repos.Users users; private final Repos.Tables tables; private final Repos.Reservations res;
    private final Repos.Waitlist wait; private final Repos.Walkins walkins;

    public AdminController(AvailabilityService av, Fmt fmt, Repos.Users u, Repos.Tables t, Repos.Reservations r, Repos.Waitlist w, Repos.Walkins wk) {
        this.av = av; this.fmt = fmt; users = u; tables = t; res = r; wait = w; walkins = wk;
    }

    public static class Day {
        public final String label; public final long n; public final int pct;
        Day(String l, long n, int p) { label = l; this.n = n; pct = p; }
    }

    // ---------- overview ----------
    @GetMapping
    String overview(Model m) {
        LocalDate t = LocalDate.now();
        List<Reservation> all = res.findAll();
        List<TableView> v = av.check(t, av.nowSlot(), 1, "none", null);
        m.addAttribute("cards", List.of(
            Map.entry("Total reservations", (long) all.size()),
            Map.entry("Today's reservations", all.stream().filter(r -> r.reservationDate.equals(t)).count()),
            Map.entry("Available tables", v.stream().filter(x -> x.state.equals("available")).count()),
            Map.entry("Occupied tables", v.stream().filter(x -> x.state.equals("occupied")).count()),
            Map.entry("Active waitlist", wait.findAll().stream().filter(w -> List.of("Waiting", "Table Available", "Notified").contains(w.status)).count()),
            Map.entry("Registered customers", (long) users.findByRole("customer").size())));
        long[] counts = new long[7];
        for (int i = 0; i < 7; i++) { LocalDate d = t.plusDays(i - 3); counts[i] = all.stream().filter(r -> r.reservationDate.equals(d) && !r.status.equals("cancelled")).count(); }
        long max = Math.max(1, Arrays.stream(counts).max().orElse(1));
        List<Day> trend = new ArrayList<>();
        for (int i = 0; i < 7; i++) trend.add(new Day(t.plusDays(i - 3).format(DateTimeFormatter.ofPattern("d MMM")), counts[i], (int) (counts[i] * 100 / max)));
        m.addAttribute("trend", trend);
        List<Day> dist = new ArrayList<>();
        for (String s : RES_STATUS) { long n = all.stream().filter(r -> r.status.equals(s)).count(); dist.add(new Day(s, n, (int) (n * 100 / Math.max(1, all.size())))); }
        m.addAttribute("dist", dist);
        m.addAttribute("recent", all.stream().sorted(Comparator.comparing((Reservation r) -> r.reservationId).reversed()).limit(5).toList());
        m.addAttribute("upcoming", all.stream().filter(r -> !r.reservationDate.isBefore(t) && ACTIVE.contains(r.status))
            .sorted(Comparator.comparing((Reservation r) -> r.reservationDate).thenComparing(r -> r.reservationTime)).limit(5).toList());
        return "admin/overview";
    }

    // ---------- reservations ----------
    @GetMapping("/reservations")
    String reservations(@RequestParam(defaultValue = "") String q,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @RequestParam(defaultValue = "") String status, @RequestParam(defaultValue = "0") int minParty, Model m) {
        String ql = q.toLowerCase();
        List<Reservation> l = res.findAll().stream()
            .filter(r -> date == null || r.reservationDate.equals(date))
            .filter(r -> status.isEmpty() || r.status.equals(status))
            .filter(r -> r.partySize >= minParty)
            .filter(r -> ql.isEmpty() || ("rsv-" + r.reservationId + fmt.name(r.userId) + fmt.phone(r.userId)).toLowerCase().contains(ql))
            .sorted(Comparator.comparing((Reservation r) -> r.reservationDate).thenComparing(r -> r.reservationTime).reversed()).toList();
        m.addAttribute("list", l); m.addAttribute("q", q); m.addAttribute("date", date); m.addAttribute("status", status); m.addAttribute("minParty", minParty);
        m.addAttribute("statuses", RES_STATUS);
        return "admin/reservations";
    }

    @PostMapping("/reservations/{id}/status")
    String setStatus(@PathVariable Long id, @RequestParam String status, RedirectAttributes ra) {
        if (RES_STATUS.contains(status)) {
            Reservation r = res.findById(id).orElseThrow();
            r.status = status; res.save(r);
            ra.addFlashAttribute("msg", "Reservation " + status + ".");
        }
        return "redirect:/admin/reservations";
    }

    @GetMapping("/reservations/{id}/edit")
    String edit(@PathVariable Long id, Model m) {
        m.addAttribute("r", res.findById(id).orElseThrow()); m.addAttribute("allTables", tables.findAll());
        return "admin/reservation-edit";
    }

    @PostMapping("/reservations/{id}/edit")
    String saveEdit(@PathVariable Long id, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                    @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime time, @RequestParam Long tableId, RedirectAttributes ra) {
        Reservation r = res.findById(id).orElseThrow();
        TableView v = av.check(date, time, r.partySize, "none", id).stream().filter(x -> x.table.tableId.equals(tableId)).findFirst().orElseThrow();
        if (!v.state.equals("available")) { ra.addFlashAttribute("error", "Table " + v.table.tableNumber + " is " + v.state + " at that time."); return "redirect:/admin/reservations/" + id + "/edit"; }
        r.reservationDate = date; r.reservationTime = time; r.tableId = tableId; res.save(r);
        ra.addFlashAttribute("msg", "Reservation updated.");
        return "redirect:/admin/reservations";
    }

    // ---------- tables (visual floor plan) ----------
    @GetMapping("/tables")
    String tables(@RequestParam(required = false) Long edit, Model m) {
        LocalDate t = LocalDate.now(); LocalTime now = av.nowSlot();
        List<TableView> views = av.check(t, now, 1, "none", null);
        views.sort(Comparator.comparingInt(v -> v.table.tableNumber));
        List<Reservation> today = res.findByReservationDate(t).stream().filter(r -> ACTIVE.contains(r.status))
            .sorted(Comparator.comparing((Reservation r) -> r.reservationTime)).toList();
        List<Walkin> ws = walkins.findByWalkinDate(t);
        for (TableView v : views) {
            Optional<Walkin> w = ws.stream().filter(x -> x.tableId.equals(v.table.tableId) && !x.released).findFirst();
            Optional<Reservation> r = today.stream().filter(x -> x.tableId.equals(v.table.tableId) && x.reservationTime.plusMinutes(AvailabilityService.DURATION_MINUTES).isAfter(now)).findFirst();
            v.info = w.isPresent() ? "Walk-in - " + w.get().partySize + " - " + w.get().customerName
                : r.isPresent() ? fmt.time(r.get().reservationTime) + " - " + r.get().partySize + " - " + fmt.name(r.get().userId)
                : v.state + " - seats " + v.table.capacity;
        }
        m.addAttribute("views", views); m.addAttribute("todayRes", today); m.addAttribute("now", now);
        m.addAttribute("free", views.stream().filter(v -> v.state.equals("available")).count());
        m.addAttribute("occupied", views.stream().filter(v -> v.state.equals("occupied")).count());
        m.addAttribute("edit", edit == null ? null : tables.findById(edit).orElse(null));
        return "admin/tables";
    }

    @PostMapping("/tables")
    String saveTable(@RequestParam(required = false) Long id, @RequestParam int tableNumber, @RequestParam int capacity,
                     @RequestParam String location, @RequestParam String status, RedirectAttributes ra) {
        String err = null;
        if (tableNumber < 1) err = "Enter a table number.";
        else { Optional<RestaurantTable> ex = tables.findByTableNumber(tableNumber); if (ex.isPresent() && !ex.get().tableId.equals(id)) err = "That table number is already used."; }
        if (err == null && (capacity < 1 || capacity > 20)) err = "Capacity must be 1-20.";
        if (err != null) { ra.addFlashAttribute("error", err); return "redirect:/admin/tables" + (id != null ? "?edit=" + id : ""); }
        RestaurantTable t = id == null ? new RestaurantTable() : tables.findById(id).orElseThrow();
        t.tableNumber = tableNumber; t.capacity = capacity; t.location = location; t.status = status;
        tables.save(t);
        ra.addFlashAttribute("msg", "Table saved.");
        return "redirect:/admin/tables";
    }

    @PostMapping("/tables/{id}/delete")
    String deleteTable(@PathVariable Long id, RedirectAttributes ra) {
        LocalDate t = LocalDate.now();
        boolean busy = res.findAll().stream().anyMatch(r -> r.tableId.equals(id) && ACTIVE.contains(r.status) && !r.reservationDate.isBefore(t));
        if (busy) ra.addFlashAttribute("error", "This table has upcoming reservations.");
        else { tables.deleteById(id); ra.addFlashAttribute("msg", "Table deleted."); }
        return "redirect:/admin/tables";
    }

    // ---------- walk-ins ----------
    private void walkinPage(Model m) {
        List<Walkin> l = new ArrayList<>(walkins.findByWalkinDate(LocalDate.now()));
        Collections.reverse(l);
        m.addAttribute("walkins", l); m.addAttribute("nowSlot", av.nowSlot());
    }

    @GetMapping("/walkins") String walkinsPage(Model m) { walkinPage(m); return "admin/walkins"; }

    @PostMapping("/walkins/suggest")
    String suggest(@RequestParam String customerName, @RequestParam String phone, @RequestParam int party,
                   @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime arrival, @RequestParam String pref, Model m) {
        Map<String, String> e = new HashMap<>();
        if (customerName.trim().length() < 2) e.put("customerName", "Enter the customer name.");
        if (!phone.matches("^\\+?[\\d\\s-]{8,15}$")) e.put("phone", "Enter a valid phone number.");
        if (party < 1 || party > 12) e.put("party", "Enter 1-12 guests.");
        walkinPage(m);
        m.addAttribute("f", Map.of("customerName", customerName, "phone", phone, "party", String.valueOf(party),
            "arrival", arrival.format(DateTimeFormatter.ofPattern("HH:mm")), "pref", pref));
        if (!e.isEmpty()) { m.addAttribute("errors", e); return "admin/walkins"; }
        m.addAttribute("checked", true);
        m.addAttribute("sug", av.best(LocalDate.now(), arrival, party, pref).orElse(null));
        return "admin/walkins";
    }

    @PostMapping("/walkins/assign")
    String assign(@RequestParam String customerName, @RequestParam String phone, @RequestParam int party,
                  @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime arrival, @RequestParam String pref,
                  @RequestParam Long tableId, RedirectAttributes ra) {
        Walkin w = new Walkin();
        w.customerName = customerName.trim(); w.phone = phone; w.partySize = party; w.arrivalTime = arrival; w.seatingPreference = pref; w.tableId = tableId;
        walkins.save(w);
        ra.addFlashAttribute("msg", "Table " + fmt.tableNo(tableId) + " assigned to " + w.customerName + ".");
        return "redirect:/admin/walkins";
    }

    @PostMapping("/walkins/waitlist")
    String walkinToWaitlist(@RequestParam String customerName, @RequestParam int party, @RequestParam String pref, RedirectAttributes ra) {
        WaitlistEntry w = new WaitlistEntry();
        w.guestName = customerName.trim(); w.partySize = party; w.seatingPreference = pref;
        wait.save(w);
        ra.addFlashAttribute("msg", "Added to the waitlist.");
        return "redirect:/admin/waitlist";
    }

    @PostMapping("/walkins/{id}/release")
    String release(@PathVariable Long id, RedirectAttributes ra) {
        Walkin w = walkins.findById(id).orElseThrow();
        w.released = true; walkins.save(w);
        ra.addFlashAttribute("msg", "Table released.");
        return "redirect:/admin/walkins";
    }

    // ---------- waitlist ----------
    @GetMapping("/waitlist")
    String waitlist(Model m) {
        m.addAttribute("list", wait.findAll().stream().sorted(Comparator.comparing((WaitlistEntry w) -> w.createdAt).reversed()).toList());
        m.addAttribute("statuses", WL_STATUS);
        return "admin/waitlist";
    }

    @PostMapping("/waitlist/{id}/status")
    String wlStatus(@PathVariable Long id, @RequestParam String status, RedirectAttributes ra) {
        if (WL_STATUS.contains(status)) { WaitlistEntry w = wait.findById(id).orElseThrow(); w.status = status; wait.save(w); ra.addFlashAttribute("msg", "Status updated."); }
        return "redirect:/admin/waitlist";
    }

    @PostMapping("/waitlist/{id}/assign")
    String wlAssign(@PathVariable Long id, RedirectAttributes ra) {
        WaitlistEntry w = wait.findById(id).orElseThrow();
        Optional<TableView> b = av.best(LocalDate.now(), av.nowSlot(), w.partySize, w.seatingPreference);
        if (b.isEmpty()) ra.addFlashAttribute("error", "No suitable table is free right now.");
        else { w.status = "Table Available"; w.tableId = b.get().table.tableId; wait.save(w); ra.addFlashAttribute("msg", "Table " + b.get().table.tableNumber + " assigned."); }
        return "redirect:/admin/waitlist";
    }

    @PostMapping("/waitlist/{id}/remove")
    String wlRemove(@PathVariable Long id, RedirectAttributes ra) { wait.deleteById(id); ra.addFlashAttribute("msg", "Entry removed."); return "redirect:/admin/waitlist"; }

    // ---------- customers ----------
    @GetMapping("/customers")
    String customers(@RequestParam(defaultValue = "") String q, Model m) {
        String ql = q.toLowerCase();
        m.addAttribute("list", users.findByRole("customer").stream().filter(u -> (u.fullName + u.email + u.phone).toLowerCase().contains(ql)).toList());
        m.addAttribute("counts", res.findAll().stream().collect(Collectors.groupingBy(r -> r.userId, Collectors.counting())));
        m.addAttribute("q", q);
        return "admin/customers";
    }
}
