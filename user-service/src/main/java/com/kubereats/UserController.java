package com.kubereats;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @GetMapping("/")
    public String home() {
        return "User Service is running!";
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}