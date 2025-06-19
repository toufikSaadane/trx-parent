package com.toufik.trxalertservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication
public class TrxAlertServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrxAlertServiceApplication.class, args);
    }

}
