package com.ecomm.product_service.controller;

import com.ecomm.product_service.dto.ProductRequest;
import com.ecomm.product_service.dto.ProductResponse;
import com.ecomm.product_service.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<ProductResponse> findAll() {
        return productService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(
            @Valid @RequestBody ProductRequest request) {

        return productService.create(request);
    }

    @GetMapping("/{id}")
    public ProductResponse findById(
            @PathVariable UUID id) {

        return productService.findById(id);
    }
}
