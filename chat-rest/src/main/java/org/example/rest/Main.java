package org.example.rest;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "org.example") // Скан всей иерархии org.example (и contract, и rest)
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}