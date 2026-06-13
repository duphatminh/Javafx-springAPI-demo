package com.example.javafxclient;

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
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class MainApplication extends Application {

    private static final String API_URL = "http://localhost:8080/api/products";
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Nguồn dữ liệu (State) cho TableView
    private final ObservableList<Product> productList = FXCollections.observableArrayList();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("JavaFX Client - Quản lý Menu");

        // --- PHẦN 1: TABLE VIEW (Hiển thị) ---
        TableView<Product> table = new TableView<>();
        table.setPrefHeight(200);

        TableColumn<Product, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().id()));

        TableColumn<Product, String> nameCol = new TableColumn<>("Tên Món");
        nameCol.setPrefWidth(180);
        nameCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().name()));

        TableColumn<Product, Double> priceCol = new TableColumn<>("Giá (VND)");
        priceCol.setPrefWidth(120);
        priceCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().price()));

        table.getColumns().addAll(idCol, nameCol, priceCol);
        table.setItems(productList);

        // --- PHẦN 2: FORM NHẬP LIỆU (Thêm mới) ---
        TextField txtName = new TextField();
        txtName.setPromptText("Nhập tên món...");

        TextField txtPrice = new TextField();
        txtPrice.setPromptText("Nhập giá...");

        Button btnAdd = new Button("Thêm món");
        Button btnUpdate = new Button("Cập nhật");
        Button btnDelete = new Button("Xóa");
        Button btnClear = new Button("Hủy chọn");

        // Trạng thái ban đầu: Vô hiệu hóa nút Cập nhật và Xóa khi chưa chọn dòng nào
        btnUpdate.setDisable(true);
        btnDelete.setDisable(true);
        btnClear.setDisable(true);

        HBox formBox = new HBox(10); // Căn hàng ngang
        formBox.setAlignment(Pos.CENTER);
        formBox.getChildren().addAll(txtName, txtPrice, btnAdd, btnUpdate, btnDelete, btnClear);

        // --- XỬ LÝ SỰ KIỆN ---
        // Sự kiện: Khi Click chọn 1 dòng trên bảng:
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                //Đổ dữ liệu lên textfield
                txtName.setText(newSelection.name());
                txtPrice.setText(String.valueOf(newSelection.price()));
                //Đổi trạng thái các nút
                btnAdd.setDisable(true);
                btnUpdate.setDisable(false);
                btnDelete.setDisable(false);
                btnClear.setDisable(false);
            }
        });

        //Sự kiện: Click vào dòng đã được chọn thì lập tức hủy chọn
        table.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getClickCount() == 2) {
                table.getSelectionModel().clearSelection();
                btnAdd.setDisable(false);
                event.consume(); // Ngăn event lan truyền
            }
        });

        // Sự kện: Nút hủy chọn
        btnClear.setOnAction(e -> {
            table.getSelectionModel().clearSelection(); // Bỏ chọn trên bảng
            txtName.clear();
            txtPrice.clear();
            btnAdd.setDisable(false);
            btnUpdate.setDisable(true);
            btnDelete.setDisable(true);
            btnClear.setDisable(true);
        });

        // CALL APIs

        // POST (Thêm mới)
        btnAdd.setOnAction(e -> {
            String name = txtName.getText().trim();
            String priceStr = txtPrice.getText().trim();

            if (!name.isEmpty() && !priceStr.isEmpty()) {
                try {
                    double price = Double.parseDouble(txtPrice.getText());
                    Product newProduct = new Product(null, txtName.getText(), price);

                    postProductToServer(newProduct);

                    btnClear.fire(); // Giả lập click nut hủy chọn để xóa form
                } catch (NumberFormatException ex) {
                    System.out.println("Giá tiền không hợp lệ!");
                }
            }
        });

        // PUT (Cập nhật)
        btnUpdate.setOnAction(e -> {
            Product selected = table.getSelectionModel().getSelectedItem();
            if (selected != null && !txtName.getText().isEmpty() && !txtPrice.getText().isEmpty()) {
                try {
                    double price = Double.parseDouble(txtPrice.getText());
                    Product updatedProduct = new Product(selected.id(), txtName.getText(), price);
                    putProductToServer(selected.id(), updatedProduct);
                    btnClear.fire();
                } catch (NumberFormatException ex) {
                    System.out.println("Giá tiền không hợp lệ!");
                }
            }
        });

        // DELETE (Xóa)
        btnDelete.setOnAction(e -> {
            Product selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                deleteProductToServer(selected.id());
                btnClear.fire();
            }
        });

        // Bố cục tổng thể
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(table, formBox);

        primaryStage.setScene(new Scene(root, 700, 400));
        primaryStage.show();

        // Tự động tải dữ liệu khi vừa mở app
        fetchProducts();
    }
    // ----- CÁC HÀM XỬ LÝ HTTP REQUEST -----

    // Hàm gọi GET request
    private void fetchProducts() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(responseBody -> {
                    try {
                        List<Product> products = objectMapper.readValue(responseBody, new TypeReference<List<Product>>() {});
                        Platform.runLater(() -> productList.setAll(products));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
    }

    // Hàm gọi POST request
    private void postProductToServer(Product product) {
        try {
            // Chuyển Object thành chuỗi JSON
            String jsonPayload = objectMapper.writeValueAsString(product);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json") // Báo cho server biết đây là JSON
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200 || response.statusCode() == 201) Platform.runLater(this::fetchProducts);
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void putProductToServer(Long id, Product product) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(product);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/" + id))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonPayload)) // Đổi thành PUT
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) Platform.runLater(this::fetchProducts);
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteProductToServer(Long id) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/" + id)) // Gọi API kèm ID
                .DELETE() // Đổi thành DELETE, không cn Body
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) Platform.runLater(this::fetchProducts);
                });
    }

    public static void main(String[] args) {
        launch(args);
    }
}