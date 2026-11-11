package com.dineease.model;

import jakarta.persistence.*;

@Entity
@Table(name = "restaurant_tables")
public class RestaurantTable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long tableId;
    public int tableNumber, capacity;
    public String location; // indoor | outdoor | window
    public String status = "available"; // available | occupied | unavailable
}
