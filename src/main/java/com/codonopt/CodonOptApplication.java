package com.codonopt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
public class CodonOptApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodonOptApplication.class, args);
        System.out.println("========================================");
        System.out.println("Codon Optimization Application Started!");
        System.out.println("Access Swagger UI: http://localhost:8008/swagger-ui.html");
        System.out.println("========================================");
    }
}
