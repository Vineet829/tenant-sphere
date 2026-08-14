package com.tenantsphere;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class TenantSphereApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenantSphereApplication.class, args);
    }
}
