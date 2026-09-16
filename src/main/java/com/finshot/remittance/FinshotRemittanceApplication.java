package com.finshot.remittance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class FinshotRemittanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinshotRemittanceApplication.class, args);
    }

}
