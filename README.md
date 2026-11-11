# DineEase - Restaurant Table Reservation System (Java)

A full-stack restaurant reservation website written in **Java 17** with **Spring Boot**, **Spring Data JPA** and **Thymeleaf**. Customers can explore the restaurant, check live table availability, reserve tables, manage bookings and join a waitlist. Administrators have a separate dashboard for reservations, tables, customers, walk-ins and the waitlist.

## Tech stack
- Java 17, Maven
- Spring Boot 3 (Web MVC), Thymeleaf templates
- Spring Data JPA / Hibernate
- H2 file database by default (no setup), PostgreSQL supported by changing `application.properties`
- BCrypt password hashing (`spring-security-crypto`)
- Plain CSS and a little JavaScript for the UI

## Run it
```bash
mvn spring-boot:run
```
Open http://localhost:8080. Demo data is created on first start.

| Role | Email | Password |
|------|-------|----------|
| Customer | guest@dineease.com | Guest@123 |
| Admin | admin@dineease.com | Admin@123 |

To build a runnable jar: `mvn clean package` then `java -jar target/dineease-1.0.0.jar`.

## Features
**Customer:** homepage, about, menu (4 categories, 6 dishes each), contact form, register/login with validation, 5-step reservation flow, rule-based **Smart Table Matcher**, waitlist with queue position and estimated wait, dashboard (upcoming/past bookings, cancel, view details, update profile).

**Admin:** overview with trends and status distribution, reservation management (search, filters, confirm/modify/complete/cancel), table management on a visual floor plan (add/edit/delete), walk-in registration with table suggestion, waitlist management, customer list.

## Smart Table Matcher
`AvailabilityService.check(...)` is a transparent rule-based matcher, not AI:
1. A table is *reserved* if another pending/confirmed booking overlaps the requested time (90-minute seating), *occupied* if a walk-in or manual status says so today, *unavailable* if switched off.
2. Only free tables that fit the whole party can be recommended.
3. Tables in the preferred seating area rank first, then the one with the fewest spare seats.
4. The best table gets the **Recommended** badge. If none fit, the customer is guided to the waitlist.
5. Booking is re-checked on confirmation so a table is never double-booked.

Waitlist wait times are estimates: queue position divided by matching tables, times an average 45-minute turnover.

## Project structure
```
src/main/java/com/dineease/
  DineEaseApplication.java
  model/       User, RestaurantTable, Reservation, WaitlistEntry, Walkin, Draft
  repo/        Repos (Spring Data repositories)
  service/     AvailabilityService (matcher), DataSeeder, Fmt, Menu, Slots
  web/         PublicController, AuthController, CustomerController, AdminController,
               AuthInterceptor (role-based access), WebConfig, GlobalAdvice
src/main/resources/
  templates/   Thymeleaf pages (+ admin/)
  static/      css/styles.css, js/app.js
  application.properties
```

## Database
Entities map to these tables (columns follow the entity field names in snake_case):
`users`, `restaurant_tables`, `reservations`, `waitlist`, `walkins`.
Switch to PostgreSQL by editing the three `spring.datasource.*` lines in `application.properties`; Hibernate creates the tables automatically (`ddl-auto=update`).

## Notes
- Entity fields are public to keep the code short; for a larger project, switch to private fields with getters/setters (or Lombok).
- There is no CSRF protection or email sending in this demo. Add Spring Security and a mail service before real use.
