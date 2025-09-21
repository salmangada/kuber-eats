package com.kubereats;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    @GetMapping("/")
    public String home() {
        return "Order Service is running!";
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}