package com.surge.producer;

import com.surge.model.RideRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;

/**
 * @Component tells Spring: "Please create this class and run it in the background."
 * @EnableScheduling allows us to use @Scheduled to run tasks on a timer.
 * @Slf4j gives us a 'log' variable so we can print messages to the console easily.
 */
@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class RideSimulator {

    // KafkaTemplate is Spring's built-in tool for sending messages to Kafka.
    // The @RequiredArgsConstructor (from Lombok) automatically wires this up for us.
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    
    // The name of our Kafka topic
    private static final String TOPIC = "ride-requests";
    
    private final Random random = new Random();
    private final String[] ZONES = {"North", "South", "East", "West"};

    /**
     * @Scheduled(fixedRate = 2000) tells Spring to run this method every 2000 milliseconds (2 seconds).
     * This acts as our "fake users" requesting rides.
     */
    @Scheduled(fixedRate = 2000)
    public void generateFakeRideRequest() {
        // 1. Pick a random zone
        String randomZone = ZONES[random.nextInt(ZONES.length)];
        
        // 2. Create the RideRequest object
        RideRequest request = new RideRequest(
                UUID.randomUUID().toString(), // Generate a random unique ID
                randomZone,
                Instant.now().toString(), // Current time as String to fix JSON error
                "REQUEST" // Default action
        );

        // 3. Send it to Kafka!
        // We use the zone as the "Key" so all rides for the same zone go to the same Kafka partition.
        kafkaTemplate.send(TOPIC, request.getZone(), request);
        
        // 4. Print it to the console so we can see it working
        log.info("🚗 Generated and sent Ride Request to Kafka: {}", request);

        // 5. AUTO-DECAY: Just like the interactive map, simulate a driver picking up the rider 8 seconds later!
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(8000);
                RideRequest completion = new RideRequest(UUID.randomUUID().toString(), request.getZone(), Instant.now().toString(), "COMPLETE");
                kafkaTemplate.send(TOPIC, request.getZone(), completion);
            } catch (Exception e) {}
        });
    }
}
