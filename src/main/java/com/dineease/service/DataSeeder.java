package com.dineease.service;

import com.dineease.model.*;
import com.dineease.repo.Repos;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.List;

/** Loads demo data on first start so the app is usable straight away. */
@Component
public class DataSeeder implements CommandLineRunner {
    private final Repos.Users users; private final Repos.Tables tables; private final Repos.Reservations res;
    public DataSeeder(Repos.Users u, Repos.Tables t, Repos.Reservations r) { users = u; tables = t; res = r; }

    @Override public void run(String... args) {
        if (users.count() > 0) return;
        var enc = new BCryptPasswordEncoder();
        mk(enc, "Restaurant Admin", "admin@dineease.com", "+91 98000 00001", "Admin@123", "admin");
        User g = mk(enc, "Demo Guest", "guest@dineease.com", "+91 98000 00002", "Guest@123", "customer");
        int[] cap = {2, 2, 2, 4, 4, 4, 4, 6, 6, 8};
        String[] loc = {"window", "window", "indoor", "indoor", "indoor", "window", "outdoor", "indoor", "outdoor", "indoor"};
        for (int i = 0; i < cap.length; i++) {
            RestaurantTable t = new RestaurantTable();
            t.tableNumber = i + 1; t.capacity = cap[i]; t.location = loc[i];
            tables.save(t);
        }
        List<RestaurantTable> ts = tables.findAll();
        LocalDate d = LocalDate.now();
        book(g, ts.get(3), d, 19, 0, 4, "confirmed");
        book(g, ts.get(0), d, 20, 0, 2, "confirmed");
        book(g, ts.get(5), d.plusDays(1), 19, 30, 3, "pending");
        book(g, ts.get(7), d.plusDays(2), 13, 0, 6, "confirmed");
        book(g, ts.get(2), d.minusDays(3), 20, 0, 2, "completed");
    }

    private User mk(BCryptPasswordEncoder enc, String name, String email, String phone, String pw, String role) {
        User u = new User();
        u.fullName = name; u.email = email; u.phone = phone; u.passwordHash = enc.encode(pw); u.role = role;
        return users.save(u);
    }

    private void book(User u, RestaurantTable t, LocalDate d, int h, int m, int party, String status) {
        Reservation r = new Reservation();
        r.userId = u.userId; r.tableId = t.tableId; r.reservationDate = d; r.reservationTime = LocalTime.of(h, m);
        r.partySize = party; r.status = status;
        res.save(r);
    }
}
