package com.dineease.model;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "walkins")
public class Walkin {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long walkinId;
    public String customerName, phone, seatingPreference;
    public int partySize;
    public LocalTime arrivalTime;
    public LocalDate walkinDate = LocalDate.now();
    public Long tableId;
    public boolean released;
}
