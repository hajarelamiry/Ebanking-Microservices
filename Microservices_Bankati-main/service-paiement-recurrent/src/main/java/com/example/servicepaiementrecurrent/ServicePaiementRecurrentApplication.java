package com.example.servicepaiementrecurrent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan(basePackages = "org.example.entites")
@EnableFeignClients
@EnableScheduling
public class ServicePaiementRecurrentApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServicePaiementRecurrentApplication.class, args);
    }

}
