package com.dineease.model;

import java.io.Serializable;
import java.time.*;

/** In-progress reservation kept in the HTTP session while the customer moves through the steps. */
public class Draft implements Serializable {
    public LocalDate date = LocalDate.now().plusDays(1);
    public LocalTime time = LocalTime.of(19, 0);
    public int party = 2;
    public String occasion = "", requests = "", pref = "none";
    public Long tableId;
}
