package com.codonopt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CodonOptApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodonOptApplication.class, args);
        System.out.println("Codon Optimization Application Started Successfully!");
    }
}
