package com.dineease.service;

import java.time.LocalTime;
import java.util.*;

public final class Slots {
    public static final List<LocalTime> TIMES = new ArrayList<>();
    static {
        for (LocalTime t = LocalTime.of(12, 0); !t.isAfter(LocalTime.of(21, 30)); t = t.plusMinutes(30)) TIMES.add(t);
    }
    private Slots() {}
}
