package com.surge.consumer;

import com.surge.service.SseService;
import com.surge.model.RideRequest;
import com.surge.model.ZonePricing;
import com.surge.repository.ZonePricingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Service tells Spring this class holds our core business logic.
 * @Slf4j gives us a logger to print out data.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SurgeCalculator {

    // Automatically injected by Spring
    private final ZonePricingRepository repository;
    private final SseService sseService;

    // In-Memory map to track active rides per zone.
    private final Map<String, Integer> zoneRideCounts = new ConcurrentHashMap<>();

    // Initialize the in-memory cache from the database when the server starts!
    @jakarta.annotation.PostConstruct
    public void initCache() {
        for (ZonePricing zone : repository.findAll()) {
            zoneRideCounts.put(zone.getZone(), zone.getActiveRides());
        }
        // If the DB was empty (first run ever), initialize all 4 zones to 0
        if (zoneRideCounts.isEmpty()) {
            zoneRideCounts.put("North", 0);
            zoneRideCounts.put("South", 0);
            zoneRideCounts.put("East", 0);
            zoneRideCounts.put("West", 0);
        }
    }

    @KafkaListener(topics = "ride-requests", groupId = "surge-calculator-group")
    public void processRideRequest(RideRequest request) {
        
        // --- EMERGENCY RESET ---
        if ("RESET".equals(request.getAction())) {
            zoneRideCounts.put("North", 0);
            zoneRideCounts.put("South", 0);
            zoneRideCounts.put("East", 0);
            zoneRideCounts.put("West", 0);
            sseService.sendRealTimeUpdate(generateCurrentState());
            return;
        }

        // 1. Update the active rides for this zone IN MEMORY ONLY (Super Fast!)
        int modifier = ("COMPLETE".equals(request.getAction())) ? -1 : 1;
        
        int currentRides = zoneRideCounts.merge(request.getZone(), modifier, (oldVal, newVal) -> {
            int result = oldVal + modifier;
            return Math.max(0, result); // Never go below 0
        });

        // 2. We DO NOT save to the database here anymore! 
        // 3. Instead, we instantly push the new state to all connected web browsers!
        List<ZonePricing> currentState = generateCurrentState();
        sseService.sendRealTimeUpdate(currentState);
    }

    // Helper method to generate the current exact state of all zones
    private List<ZonePricing> generateCurrentState() {
        List<ZonePricing> stateList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : zoneRideCounts.entrySet()) {
            double surgeMultiplier = 1.0;
            if (entry.getValue() > 10) surgeMultiplier = 2.0;
            else if (entry.getValue() > 5) surgeMultiplier = 1.5;
            stateList.add(new ZonePricing(entry.getKey(), entry.getValue(), surgeMultiplier, Instant.now().toString()));
        }
        // Sort alphabetically so the UI dashboard cards don't jump around!
        stateList.sort((a, b) -> a.getZone().compareTo(b.getZone()));
        return stateList;
    }

    /**
     * BATCH FLUSHER: Runs every 1 second (1000ms).
     * It takes the current in-memory state of all zones and does ONE batch save to TiDB.
     * This makes our system capable of handling millions of events per second!
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 1000)
    public void flushStateToDatabase() {
        if (zoneRideCounts.isEmpty()) return;

        for (Map.Entry<String, Integer> entry : zoneRideCounts.entrySet()) {
            String zone = entry.getKey();
            int currentRides = entry.getValue();

            // Calculate Surge
            double surgeMultiplier = 1.0;
            if (currentRides > 10) {
                surgeMultiplier = 2.0;
            } else if (currentRides > 5) {
                surgeMultiplier = 1.5;
            }

            // Upsert to TiDB
            ZonePricing state = new ZonePricing(
                    zone,
                    currentRides,
                    surgeMultiplier,
                    Instant.now().toString()
            );
            repository.save(state);
            
            log.info("💾 FLUSHED TO DB | Zone: {} | Active Rides: {} | Multiplier: {}x", zone, currentRides, surgeMultiplier);
        }
    }
}
