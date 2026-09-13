package com.surge.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * @Entity tells Spring Data JPA to automatically create a MySQL table for this class!
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZonePricing {
    
    @Id
    private String zone;           // The Primary Key (e.g., "North"). Ensures only 1 row per zone!
    
    private int activeRides;       // How many rides are happening right now
    
    private double surgeMultiplier; // The calculated price
    
    private String lastUpdated;     // When this was last calculated
}
