package com.lifepath;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LifepathApplication {

    public static void main(String[] args) {
        SpringApplication.run(LifepathApplication.class, args);
    }
}
