package com.dineease.service;

import com.dineease.repo.Repos;
import org.springframework.stereotype.Component;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatting and lookup helpers used from templates, e.g. @fmt.time(t), @fmt.date(d), @fmt.name(userId). */
@Component("fmt")
public class Fmt {
    private final Repos.Users users;
    private final Repos.Tables tables;
    public Fmt(Repos.Users users, Repos.Tables tables) { this.users = users; this.tables = tables; }

    public String time(LocalTime t) { return t.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)); }
    public String date(LocalDate d) { return d.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)); }
    public String name(Long userId) { return userId == null ? "Walk-in guest" : users.findById(userId).map(u -> u.fullName).orElse("Unknown"); }
    public String phone(Long userId) { return userId == null ? "" : users.findById(userId).map(u -> u.phone).orElse(""); }
    public String tableNo(Long tableId) { return tableId == null ? "-" : tables.findById(tableId).map(t -> String.valueOf(t.tableNumber)).orElse("?"); }
}
