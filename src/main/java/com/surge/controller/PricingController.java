package com.surge.controller;

import com.surge.model.RideRequest;
import com.surge.model.ZonePricing;
import com.surge.repository.ZonePricingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.surge.service.SseService;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allows our frontend HTML file to call this API
public class PricingController {

    private final ZonePricingRepository repository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final SseService sseService;

    // INTERACTIVE FEATURE: Real-time UI updates (No more polling!)
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamPricing() {
        return sseService.createEmitter();
    }

    // This fetches the initial data for the Dashboard on load
    @GetMapping("/pricing")
    public List<ZonePricing> getLivePricing() {
        return repository.findAll();
    }

    // INTERACTIVE FEATURE: Spike the demand to trigger a high-surge
    @PostMapping("/simulate-concert/{zone}")
    public String simulateConcert(@PathVariable String zone) {
        for (int i = 0; i < 25; i++) {
            RideRequest request = new RideRequest(UUID.randomUUID().toString(), zone, Instant.now().toString(), "REQUEST");
            kafkaTemplate.send("ride-requests", zone, request);
        }
        return "Simulated 25 rides requested in " + zone + "!";
    }

    // ORGANIC MAP FEATURE: Single organic ride request
    @PostMapping("/request-ride/{zone}")
    public String requestRide(@PathVariable String zone) {
        // 1. Send the ride request to Kafka
        RideRequest request = new RideRequest(UUID.randomUUID().toString(), zone, Instant.now().toString(), "REQUEST");
        kafkaTemplate.send("ride-requests", zone, request);

        // 2. Simulate a driver automatically accepting the ride 8 seconds later!
        // This causes the demand to naturally decay so the map cools down.
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(8000);
                RideRequest completion = new RideRequest(UUID.randomUUID().toString(), zone, Instant.now().toString(), "COMPLETE");
                kafkaTemplate.send("ride-requests", zone, completion);
            } catch (Exception e) {
                // ignore
            }
        });

        return "Requested 1 ride in " + zone;
    }

    // ORGANIC MAP FEATURE: Single driver pickup
    @PostMapping("/dispatch-driver/{zone}")
    public String dispatchSingleDriver(@PathVariable String zone) {
        RideRequest request = new RideRequest(UUID.randomUUID().toString(), zone, Instant.now().toString(), "COMPLETE");
        kafkaTemplate.send("ride-requests", zone, request);
        return "Dispatched 1 driver to " + zone;
    }

    // INTERACTIVE FEATURE: Send drivers to resolve the surge
    @PostMapping("/dispatch-drivers/{zone}")
    public String dispatchDrivers(@PathVariable String zone) {
        for (int i = 0; i < 25; i++) {
            RideRequest request = new RideRequest(UUID.randomUUID().toString(), zone, Instant.now().toString(), "COMPLETE");
            kafkaTemplate.send("ride-requests", zone, request);
        }
        return "Dispatched 25 drivers to " + zone + "!";
    }

    // EMERGENCY RESET FEATURE
    @PostMapping("/reset")
    public String resetAll() {
        RideRequest request = new RideRequest(UUID.randomUUID().toString(), "ALL", Instant.now().toString(), "RESET");
        kafkaTemplate.send("ride-requests", "ALL", request);
        return "System completely reset!";
    }
}
