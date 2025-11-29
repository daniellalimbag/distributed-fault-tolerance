package com.example.enrollment.authenroll;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.example.enrollment")
@EnableJpaRepositories(basePackages = "com.example.enrollment.data")
@EntityScan(basePackages = "com.example.enrollment.data")
public class AuthEnrollmentApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthEnrollmentApplication.class, args);
    }
}
