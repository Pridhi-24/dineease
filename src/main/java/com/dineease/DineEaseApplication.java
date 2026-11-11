package com.dineease;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(considerNestedRepositories = true)
public class DineEaseApplication {
    public static void main(String[] args) {
        SpringApplication.run(DineEaseApplication.class, args);
    }
}
