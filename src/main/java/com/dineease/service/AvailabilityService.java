package com.dineease.service;

import com.dineease.model.*;
import com.dineease.repo.Repos;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;
import java.util.stream.IntStream;

/**
 * Rule-based Smart Table Matcher (not AI). A table is recommended when it
 *  1) is free for the whole requested slot, 2) fits the entire party,
 *  3) prefers the requested seating area, 4) wastes the fewest seats.
 */
@Service("availabilityService")
public class AvailabilityService {
    public static final int DURATION_MINUTES = 90;
    private final Repos.Tables tables;
    private final Repos.Reservations reservations;
    private final Repos.Walkins walkins;
    private final Repos.Waitlist waitlist;

    public AvailabilityService(Repos.Tables t, Repos.Reservations r, Repos.Walkins w, Repos.Waitlist wl) {
        this.tables = t; this.reservations = r; this.walkins = w; this.waitlist = wl;
    }

    public static class TableView {
        public final RestaurantTable table;
        public final String state; // available | reserved | occupied | unavailable
        public final boolean fits, prefMatch, ok;
        public boolean recommended;
        public String info = "";
        public final List<Integer> topSeats, botSeats;
        public final int width;

        TableView(RestaurantTable t, String state, boolean fits, boolean prefMatch) {
            this.table = t; this.state = state; this.fits = fits; this.prefMatch = prefMatch;
            this.ok = state.equals("available") && fits;
            int top = (t.capacity + 1) / 2;
            this.topSeats = IntStream.rangeClosed(1, top).boxed().toList();
            this.botSeats = IntStream.rangeClosed(1, t.capacity - top).boxed().toList();
            this.width = Math.max(140, top * 58 + 36);
        }
    }

    private static boolean overlaps(LocalTime a, LocalTime b) {
        return Math.abs(Duration.between(a, b).toMinutes()) < DURATION_MINUTES;
    }

    /** skipReservationId lets an admin re-check a booking against everything except itself. */
    public List<TableView> check(LocalDate date, LocalTime time, int party, String pref, Long skipReservationId) {
        boolean isToday = date.equals(LocalDate.now());
        List<Reservation> rs = reservations.findByReservationDate(date);
        List<Walkin> ws = isToday ? walkins.findByWalkinDate(date) : List.of();
        List<TableView> out = new ArrayList<>();
        for (RestaurantTable t : tables.findAll()) {
            String st = "available";
            if ("unavailable".equals(t.status)) st = "unavailable";
            else if (rs.stream().anyMatch(r -> r.tableId.equals(t.tableId) && !r.reservationId.equals(skipReservationId)
                    && (r.status.equals("pending") || r.status.equals("confirmed")) && overlaps(r.reservationTime, time))) st = "reserved";
            else if (isToday && ("occupied".equals(t.status)
                    || ws.stream().anyMatch(w -> w.tableId.equals(t.tableId) && !w.released && overlaps(w.arrivalTime, time)))) st = "occupied";
            out.add(new TableView(t, st, t.capacity >= party, "none".equals(pref) || t.location.equals(pref)));
        }
        out.sort((a, b) -> {
            if (a.ok != b.ok) return a.ok ? -1 : 1;
            if (a.ok) return Integer.compare(score(a, party), score(b, party));
            return Integer.compare(a.table.tableNumber, b.table.tableNumber);
        });
        if (!out.isEmpty() && out.get(0).ok) out.get(0).recommended = true;
        return out;
    }

    private static int score(TableView v, int party) { return (v.prefMatch ? 0 : 100) + v.table.capacity - party; }

    public Optional<TableView> best(LocalDate d, LocalTime t, int party, String pref) {
        return check(d, t, party, pref, null).stream().filter(v -> v.ok).findFirst();
    }

    public LocalTime nowSlot() {
        LocalTime n = LocalTime.now();
        return LocalTime.of(n.getHour(), n.getMinute() >= 30 ? 30 : 0);
    }

    // ----- waitlist helpers (used from templates as @availabilityService.position(w)) -----
    public int position(WaitlistEntry w) {
        if (!"Waiting".equals(w.status)) return 0;
        return (int) waitlist.findByStatusOrderByCreatedAtAsc("Waiting").stream().filter(x -> !x.createdAt.isAfter(w.createdAt)).count();
    }

    /** Estimate only: queue position / matching tables, times an average turnover of 45 minutes. */
    public String eta(WaitlistEntry w) {
        int p = position(w);
        if (p == 0) return "-";
        long m = Math.max(1, tables.findAll().stream().filter(t -> !"unavailable".equals(t.status) && t.capacity >= w.partySize
                && ("none".equals(w.seatingPreference) || t.location.equals(w.seatingPreference))).count());
        return "approx. " + Math.max(10, (int) Math.ceil((double) p / m) * 45) + " min (estimate)";
    }
}
