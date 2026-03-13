package com.umiot.microclimate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MicroclimateApplication { // main class

    public static void main(String[] args) {
        SpringApplication.run(MicroclimateApplication.class, args);
    }

}
