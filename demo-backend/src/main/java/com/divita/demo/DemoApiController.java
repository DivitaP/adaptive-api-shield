package com.divita.demo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DemoApiController {

    @GetMapping("/products")
    public ResponseEntity<?> products() {
        return ResponseEntity.ok(Map.of(
                "message", "Products fetched successfully",
                "items", new String[] {"Laptop", "Phone", "Keyboard"}
        ));
    }

    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "message", "Order created successfully",
                "order", body != null ? body : Map.of("productId", 1, "quantity", 2)
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "");
        String password = body.getOrDefault("password", "");

        if("admin".equals(username) && "password".equals(password)) {
            return ResponseEntity.ok(Map.of(
                    "message", "Login successful")
            );
        }

        return ResponseEntity.status(401).body(Map.of(
                "message", "Invalid credentials")
        );
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "demo-backend running",
                "timestamp", System.currentTimeMillis()
        ));
    }

}
