package com.example.mybooks.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiInfoController {

    @RequestMapping(value = "/books", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> booksOptions() {
        return ResponseEntity
                .ok()
                .header("Allow", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
                .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
                .header("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .build();
    }

    @RequestMapping(value = "/books/{id}", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> bookByIdOptions() {
        return ResponseEntity
                .ok()
                .header("Allow", "GET, PUT, DELETE, OPTIONS, HEAD")
                .header("Access-Control-Allow-Methods", "GET, PUT, DELETE, OPTIONS, HEAD")
                .header("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .build();
    }

    @RequestMapping(value = "/auth/**", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> authOptions() {
        return ResponseEntity
                .ok()
                .header("Allow", "POST, OPTIONS")
                .header("Access-Control-Allow-Methods", "POST, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type")
                .build();
    }

    @RequestMapping(value = "/books", method = RequestMethod.HEAD)
    public ResponseEntity<?> booksHead() {
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header("X-Total-Count", "0")
                .build();
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> apiInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "MyBooks API");
        info.put("version", "1.0.0");
        info.put("description", "Book tracking and management API");

        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("health", "GET /hello");
        endpoints.put("register", "POST /api/auth/register");
        endpoints.put("login", "POST /api/auth/login");
        endpoints.put("logout", "POST /api/auth/logout");
        endpoints.put("books", "GET, POST /api/books");
        endpoints.put("book", "GET, PUT, DELETE /api/books/{id}");
        endpoints.put("upload", "POST /api/upload/book-cover");

        info.put("endpoints", endpoints);
        info.put("documentation", "/api-docs");

        return ResponseEntity.ok(info);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());

        Map<String, String> components = new HashMap<>();
        components.put("database", "UP");
        components.put("api", "UP");

        health.put("components", components);

        return ResponseEntity.ok(health);
    }
}