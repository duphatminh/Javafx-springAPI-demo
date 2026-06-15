package com.example.javafxclient;

import com.example.javafxclient.model.CircuitBoard;
import com.example.javafxclient.model.Product;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class MainApplication extends Application {
    private static final String API_URL = "http://localhost:8080/api/boards";
    private final HttpClient httpClient =  HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ObservableList<CircuitBoard> boardList = FXCollections.observableArrayList();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Test Station");

        // Scanning Area
        Label lblScanner = new Label("QUÉT MÃ VẠCH BO MẠCH:");
        lblScanner.setFont(Font.font("System", FontWeight.BOLD, 16));

        TextField txtBarcode = new TextField();
        txtBarcode.setPromptText("Click vào đây và dùng súng bắn mã vạch...");
        txtBarcode.setFont(Font.font(18));
        txtBarcode.setPrefWidth(200);

        HBox scannerBox = new HBox(15,lblScanner, txtBarcode);
        scannerBox.setAlignment(Pos.CENTER);
        scannerBox.setPadding(new Insets(10));
        scannerBox.setStyle("-fx-background-color: #e9ecef; -fx-background-radius: 5;");

        // Data Table Area
        TableView<CircuitBoard> table = new TableView<>();
        table.setPrefHeight(300);

        TableColumn<CircuitBoard, String> barcodeCol = new TableColumn<>("Mã Vạch");
        barcodeCol.setPrefWidth(150);
        barcodeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().barcode()));

        TableColumn<CircuitBoard, String> modelCol = new TableColumn<>("Mã SP");
        modelCol.setPrefWidth(120);
        modelCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().boardModel()));

        TableColumn<CircuitBoard, String> statusCol = new TableColumn<>("Trạng Thái");
        statusCol.setPrefWidth(100);
        statusCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().status()));

        TableColumn<CircuitBoard, String> reasonCol = new TableColumn<>("Lý Do Lỗi");
        reasonCol.setPrefWidth(180);
        reasonCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().defectReason()));

        TableColumn<CircuitBoard, String> timeCol = new TableColumn<>("Thời Gian");
        timeCol.setPrefWidth(150);
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().testedAt()));

        table.getColumns().addAll(barcodeCol, modelCol, statusCol, reasonCol, timeCol);
        table.setItems(boardList);

        //ĐỔi màu theo trạng thái
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(CircuitBoard board, boolean empty) {
                super.updateItem(board, empty);
                if (board == null || empty) {
                    setStyle("");
                } else {
                    switch (board.status()) {
                        case "PASS" -> setStyle("-fx-background-color: #d4edda;");
                        case "FAIL" -> setStyle("-fx-background-color: #f8d7da;");
                        case "PENDING" -> setStyle("-fx-background-color: #fff3cd;");
                        default -> setStyle("");
                    }
                }
            }
        });

        // Controll Area
        Button btnPass = new Button("PASS (ĐẠT)");
        btnPass.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        btnPass.setPrefSize(150, 50);

        Button btnFail = new Button("FAIL (LỖI)");
        btnFail.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        btnFail.setPrefSize(150, 50);

        TextField txtReason = new TextField();
        txtReason.setPromptText("Nhập lý do lỗi nếu FAIL...");
        txtReason.setPrefWidth(250);
        txtReason.setPrefHeight(40);

        HBox actionBox = new HBox(20, btnPass, btnFail, txtReason);
        actionBox.setAlignment(Pos.CENTER);

        btnPass.setDisable(true);
        btnFail.setDisable(true);
        txtReason.setDisable(true);

        // Processing Event

        // Sự kiện: Súng bắn mã vạch quét xong (Tương đương phím Enter)
        txtBarcode.setOnAction(e -> {
            String barcode = txtBarcode.getText().trim();
            if (!barcode.isEmpty()) {
                CircuitBoard newBoard = new CircuitBoard(null, barcode, "ESP32-Controller-V2", null, null, null);
                scanBoardToServer(newBoard, txtBarcode);
            }
        });

        // Sự kiện: Chọn 1 bo mạch trển bảng để Test
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
           if (newVal != null && newVal.status().equals("PENDING")){
               btnPass.setDisable(false);
               btnFail.setDisable(false);
               txtReason.setDisable(false);
           } else {
               btnPass.setDisable(true);
               btnFail.setDisable(true);
               txtReason.setDisable(true);
           }
        });

        // Event: click Pass button
        btnPass.setOnAction(e -> {
            CircuitBoard selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                updateBoardResult(selected.id(), "PASS", "");
                txtReason.clear();
            }
        });

        // Event: Click Fail Button
        btnFail.setOnAction(e -> {
            CircuitBoard selected = table.getSelectionModel().getSelectedItem();
            if (selected != null)  {
                String reason = txtReason.getText().trim();
                if(reason.isEmpty()) reason = "Không rõ lỗi";
                updateBoardResult(selected.id(), "FAIL", reason);
                txtReason.clear();
            }
        });

        // Bố cục tổng thể
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.getChildren().addAll(scannerBox, actionBox, table);

        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();

        fetchBoards();

        // Tự động focus vào ô quét mã vạch mở app
        Platform.runLater(txtBarcode::requestFocus);
    }

    // HTTP REQUEST
    private void fetchBoards() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    try {
                        List<CircuitBoard> boards = objectMapper.readValue(response.body(), new TypeReference<>() {});
                        Platform.runLater(() -> boardList.setAll(boards));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    private void scanBoardToServer(CircuitBoard board, TextField txtBarcode) {
        try {
            String json = objectMapper.writeValueAsString(board);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/scan"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            Platform.runLater(() -> {
                                fetchBoards();
                                txtBarcode.clear(); // Xóa trắng để quét mã tiếp theo
                            });
                        }
                    });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateBoardResult(Long id, String status, String reason) {
        // Encode URL parameters (Cho trường hợp lý do lỗi có dấu cách/tiếng việt)
        String params =  "?status=" + status + "&defectReason=" + java.net.URLEncoder.encode(reason, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/" + id + "/result" + params))
                .PUT(HttpRequest.BodyPublishers.noBody()) // API dùng để @RequestParams nên không cần Body Json
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) Platform.runLater(this::fetchBoards);                });
    }


    public static void main(String[] args) {
        launch(args);
    }
}