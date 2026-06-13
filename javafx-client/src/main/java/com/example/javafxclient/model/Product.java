package com.example.javafxclient.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Bỏ qua các trường dư thừa từ API (nếu có) để không bị lỗi
@JsonIgnoreProperties(ignoreUnknown = true)
public record Product(Long id, String name, double price) {
}