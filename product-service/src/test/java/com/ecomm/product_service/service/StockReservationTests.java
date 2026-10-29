package com.ecomm.product_service.service;

import com.ecomm.product_service.entity.Product;
import com.ecomm.product_service.entity.StockReservation;
import com.ecomm.product_service.exception.InsufficientStockException;
import com.ecomm.product_service.repository.ProductRepository;
import com.ecomm.product_service.repository.StockReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockReservationTests {
    @Mock ProductRepository products;
    @Mock StockReservationRepository reservations;
    @InjectMocks ProductService service;

    @Test
    void reserveDecrementsStockAndRepeatedOrderDoesNotDecrementAgain() {
        UUID productId = UUID.randomUUID(), orderId = UUID.randomUUID();
        Product product = Product.builder().id(productId).quantity(5).build();
        when(products.lockById(productId)).thenReturn(Optional.of(product));
        when(reservations.findById(orderId)).thenReturn(Optional.empty(),
                Optional.of(StockReservation.builder().orderId(orderId).productId(productId).quantity(2).build()));

        service.reserve(productId, orderId, 2);
        service.reserve(productId, orderId, 2);

        assertThat(product.getQuantity()).isEqualTo(3);
        verify(reservations, times(1)).save(any(StockReservation.class));
    }

    @Test
    void reserveRejectsInsufficientStockWithoutChangingIt() {
        UUID productId = UUID.randomUUID(), orderId = UUID.randomUUID();
        Product product = Product.builder().id(productId).quantity(1).build();
        when(products.lockById(productId)).thenReturn(Optional.of(product));
        when(reservations.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reserve(productId, orderId, 2))
                .isInstanceOf(InsufficientStockException.class);
        assertThat(product.getQuantity()).isEqualTo(1);
        verify(reservations, never()).save(any());
    }

    @Test
    void releaseRestoresStockOnlyOnce() {
        UUID productId = UUID.randomUUID(), orderId = UUID.randomUUID();
        Product product = Product.builder().id(productId).quantity(3).build();
        StockReservation reservation = StockReservation.builder()
                .orderId(orderId).productId(productId).quantity(2).released(false).build();
        when(products.lockById(productId)).thenReturn(Optional.of(product));
        when(reservations.findById(orderId)).thenReturn(Optional.of(reservation));

        service.release(productId, orderId);
        service.release(productId, orderId);

        assertThat(product.getQuantity()).isEqualTo(5);
        assertThat(reservation.isReleased()).isTrue();
    }
}
