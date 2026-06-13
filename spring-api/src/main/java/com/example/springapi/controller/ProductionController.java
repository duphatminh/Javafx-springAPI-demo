package com.example.springapi.controller;

import com.example.springapi.model.Product;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/products")
public class ProductionController {

    // Sử dụng ArrayList để có thể thêm dữ liệu mới
    private final List<Product> productList = new ArrayList<>(List.of(
            new Product(1L, "Trà đào cam sả", 45000),
            new Product(2L, "Trà vải Phúc Long", 55000),
            new Product(3L, "Lẩu Thái 2 người", 250000)
    ));

    // Dùng để tự động tăng ID khi thêm món mới
    private final AtomicLong nextId = new AtomicLong(4);

    @GetMapping
    public List<Product> getAllProducts() {
        return productList;
    }

    // API nhận dữ liệu POST từ JavaFX
    @PostMapping
    public Product addProduct(@RequestBody Product newProduct) {
        // Tạo ra một Product mới với ID tự động tăng
        Product savedProduct = new Product(nextId.getAndIncrement(), newProduct.name(), newProduct.price());
        productList.add(savedProduct);
        return savedProduct; // Trả về món vừa thêm thành công
    }

    // API Cập nhật dữ liệu (PUT)
    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable Long id, @RequestBody Product updateProduct) {
        // Tìm sản phẩm trong danh sách theo ID
        for (int i = 0; i < productList.size(); i++) {
            if (productList.get(i).id().equals(id)) {
                //Tạo 1 bản ghi Product mới với ID cũ nhưng Tên và Giá mới
                Product product = new Product(id, updateProduct.name(), updateProduct.price());
                 productList.set(i, product); // Ghi đè vào vị trí cũ
                return product;
            }
        }
        return null;
    }

    // APi Xóa dữ liệu (DELETE)
    @DeleteMapping("/{id}")
    public String deleteProduct(@PathVariable Long id) {
        // Dùng biểu thức Lambda để xóa sản phẩm có ID trùng khớp
        productList.removeIf(product -> product.id().equals(id));
        return "Đã xóa thành công";
    }
}