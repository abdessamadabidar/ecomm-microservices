package com.ecomm.product_service.service;

import com.ecomm.product_service.dto.ProductRequest;
import com.ecomm.product_service.dto.ProductResponse;
import com.ecomm.product_service.entity.Product;
import com.ecomm.product_service.entity.StockReservation;
import com.ecomm.product_service.exception.InsufficientStockException;
import com.ecomm.product_service.exception.ProductNotFoundException;
import com.ecomm.product_service.repository.ProductRepository;
import com.ecomm.product_service.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final StockReservationRepository reservationRepository;

    @Transactional
    public void reserve(UUID productId, UUID orderId, int quantity) {
        Product product = productRepository.lockById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + productId));

        // The product row lock serializes competing orders and repeated requests.
        if (reservationRepository.findById(orderId).isPresent()) {
            return;
        }
        if (product.getQuantity() < quantity) {
            throw new InsufficientStockException("Not enough stock for product: " + productId);
        }

        product.setQuantity(product.getQuantity() - quantity);
        reservationRepository.save(StockReservation.builder()
                .orderId(orderId).productId(productId).quantity(quantity).released(false).build());
    }

    @Transactional
    public void release(UUID productId, UUID orderId) {
        Product product = productRepository.lockById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + productId));
        StockReservation reservation = reservationRepository.findById(orderId).orElse(null);
        if (reservation == null || reservation.isReleased()) {
            return;
        }
        if (!reservation.getProductId().equals(productId)) {
            throw new IllegalArgumentException("Reservation belongs to a different product");
        }
        product.setQuantity(Math.addExact(product.getQuantity(), reservation.getQuantity()));
        reservation.setReleased(true);
    }

    public ProductResponse create(ProductRequest request) {
        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .quantity(request.quantity())
                .build();

        Product savedProduct = productRepository.save(product);

        return mapToResponse(savedProduct);
    }

    public List<ProductResponse> findAll() {
        return productRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ProductResponse findById(UUID id) {
        Product product = findProductById(id);

        return mapToResponse(product);
    }

    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = findProductById(id);

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setQuantity(request.quantity());

        Product updatedProduct = productRepository.save(product);

        return mapToResponse(updatedProduct);
    }

    public void delete(UUID id) {
        Product product = findProductById(id);

        productRepository.delete(product);
    }

    private Product findProductById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id: " + id
                        )
                );
    }

    private ProductResponse mapToResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getQuantity()
        );
    }
}
