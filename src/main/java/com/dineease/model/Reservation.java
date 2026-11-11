package com.dineease.model;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "reservations")
public class Reservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long reservationId;
    public Long userId, tableId;
    public LocalDate reservationDate;
    public LocalTime reservationTime;
    public int partySize;
    public String seatingPreference = "none", occasion = "", specialRequests = "";
    public String status = "pending"; // pending | confirmed | completed | cancelled
    public LocalDate createdAt = LocalDate.now();
}
