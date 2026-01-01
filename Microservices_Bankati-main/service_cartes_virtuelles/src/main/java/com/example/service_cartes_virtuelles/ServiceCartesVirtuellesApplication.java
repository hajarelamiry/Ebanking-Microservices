package com.example.service_cartes_virtuelles;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EntityScan(basePackages = "org.example.entites")
@EnableFeignClients
//@EnableDiscoveryClient
public class ServiceCartesVirtuellesApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceCartesVirtuellesApplication.class, args);
    }

}
