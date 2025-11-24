package com.officemate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OfficemateApplication {

    public static void main(String[] args) {
        SpringApplication.run(OfficemateApplication.class, args);
    }
}
