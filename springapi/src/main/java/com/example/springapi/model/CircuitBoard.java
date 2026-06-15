package com.example.springapi.model;

public record CircuitBoard(
    Long id,
    String barcode, // Mã vạch dán trên bo mạch
    String boardModel, // Mã loại vạch
    String status, // Trạng thái: "PENDING", "PASS", "FAIL"
    String defectReason, //Lý do lỗi (Nếu có): "Chạm chập nguồn", "Lỗi IC", "Hở chân hàn"
    String testedAt // Thời gia kiểm tra
) { }
