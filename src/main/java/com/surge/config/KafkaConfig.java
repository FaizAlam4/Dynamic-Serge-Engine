package com.surge.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * This class automatically creates the Kafka topics in our Aiven cluster
 * when the Spring Boot application starts.
 */
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic rideRequestsTopic() {
        return TopicBuilder.name("ride-requests")
                .partitions(1)
                .replicas(2)
                .build();
    }
}
