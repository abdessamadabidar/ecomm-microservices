package com.ecomm.notification_service.observation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {
    private final EventStore eventStore;

    @GetMapping
    public List<ObservedEvent> findAll(@RequestParam(required = false) UUID orderId) {
        return eventStore.findAll(orderId);
    }
}
