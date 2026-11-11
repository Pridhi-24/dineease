package com.dineease.repo;

import com.dineease.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;

public class Repos {
    public interface Users extends JpaRepository<User, Long> {
        Optional<User> findByEmailIgnoreCase(String email);
        List<User> findByRole(String role);
    }
    public interface Tables extends JpaRepository<RestaurantTable, Long> {
        Optional<RestaurantTable> findByTableNumber(int number);
    }
    public interface Reservations extends JpaRepository<Reservation, Long> {
        List<Reservation> findByReservationDate(LocalDate date);
        List<Reservation> findByUserIdOrderByReservationDateDescReservationTimeDesc(Long userId);
    }
    public interface Waitlist extends JpaRepository<WaitlistEntry, Long> {
        List<WaitlistEntry> findByUserIdOrderByCreatedAtDesc(Long userId);
        List<WaitlistEntry> findByStatusOrderByCreatedAtAsc(String status);
    }
    public interface Walkins extends JpaRepository<Walkin, Long> {
        List<Walkin> findByWalkinDate(LocalDate date);
    }
}
