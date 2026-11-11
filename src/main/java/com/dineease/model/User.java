package com.dineease.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "users")
public class User implements java.io.Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long userId;
    public String fullName, email, phone, passwordHash, role; // role: customer | admin
    public LocalDate createdAt = LocalDate.now();
}
