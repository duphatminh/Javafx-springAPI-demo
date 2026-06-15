package com.example.springapi.controller;

import com.example.springapi.model.CircuitBoard;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/boards")
public class BoardController {

    private final List<CircuitBoard> boardList = new ArrayList<>();
    private final AtomicLong nextId = new AtomicLong(1);
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 1. Công nhân dùng súng bắn mã vạch quét bo mạch (Thêm mới)
    @PostMapping("/scan")
    public CircuitBoard scanBoard(@RequestBody CircuitBoard newBoard) {
        String currentTime = LocalDateTime.now().format(formatter);
        CircuitBoard savedBoard = new CircuitBoard(
                nextId.getAndIncrement(),
                newBoard.barcode(),
                newBoard.boardModel(),
                "PENDING", // Vừa quét xong, chờ test
                "",
                currentTime
        );
        boardList.add(savedBoard);
        return savedBoard;
    }

    // 2. Cập nhật kết quả sau khi Test (PASS hoặc FAIL)
    @PutMapping("/{id}/result")
    public CircuitBoard updateTestResult(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false, defaultValue = "") String defectReason) {

        for (int i = 0; i < boardList.size(); i++) {
            if (boardList.get(i).id().equals(id)) {
                CircuitBoard old = boardList.get(i);
                CircuitBoard updated = new CircuitBoard(
                        old.id(), old.barcode(), old.boardModel(),
                        status.toUpperCase(), defectReason, old.testedAt()
                );
                boardList.set(i, updated);
                return updated;
            }
        }
        return null;
    }

    // 3. Lấy danh sách để hiển thị lên bảng điều khiển JavaFX
    @GetMapping
    public List<CircuitBoard> getAllBoards() {
        return boardList;
    }
}