package com.ecomm.order_service.controller;

import com.ecomm.order_service.dto.OrderRequest;
import com.ecomm.order_service.dto.OrderResponse;
import com.ecomm.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(
            @Valid @RequestBody OrderRequest request
    ) {
        return orderService.createOrder(request);
    }

    @GetMapping
    public List<OrderResponse> findAll() {
        return orderService.findAll();
    }

    @GetMapping("/{id}")
    public OrderResponse findById(
            @PathVariable UUID id
    ) {
        return orderService.findById(id);
    }

    @PatchMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelOrder(
            @PathVariable UUID id
    ) {
        orderService.cancelOrder(id);
    }

    @PostMapping("/{id}/pay")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OrderResponse requestPayment(@PathVariable UUID id) {
        return orderService.requestPayment(id);
    }
}
