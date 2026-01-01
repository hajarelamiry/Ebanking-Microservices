package com.example.cmi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {"org.example.entites","com.example.cmi.model"})
@EnableFeignClients
@EnableJpaRepositories(basePackages = "com.example.cmi.repository")
public class CmiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CmiApplication.class, args);
    }

}
