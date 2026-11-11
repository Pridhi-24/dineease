package com.dineease.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "waitlist")
public class WaitlistEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long waitlistId;
    public Long userId, tableId; // userId is null for walk-in guests
    public String guestName;
    public int partySize;
    public String seatingPreference = "none";
    public String status = "Waiting"; // Waiting | Table Available | Notified | Seated | Cancelled
    public LocalDateTime createdAt = LocalDateTime.now();
}
