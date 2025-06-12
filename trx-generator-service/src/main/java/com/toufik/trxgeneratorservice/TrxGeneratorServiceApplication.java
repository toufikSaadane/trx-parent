package com.toufik.trxgeneratorservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TrxGeneratorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrxGeneratorServiceApplication.class, args);
    }
}
