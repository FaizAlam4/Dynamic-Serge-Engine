package com.surge.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * This class represents a single Ride Request (like someone opening the Uber app and requesting a car).
 * 
 * We use Lombok annotations (@Data, @AllArgsConstructor, @NoArgsConstructor) 
 * so we don't have to write boring getters, setters, and constructors manually!
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RideRequest {
    private String id;          // A unique ID for the ride
    private String zone;        // The area they are in (e.g., "North", "South")
    private String timestamp;   // The exact time they requested the ride
    private String action;      // "REQUEST" (adds load) or "COMPLETE" (removes load)
}
