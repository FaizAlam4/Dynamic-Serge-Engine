package com.surge.repository;

import com.surge.model.ZonePricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Just by extending JpaRepository, Spring automatically generates all the 
 * INSERT, UPDATE, SELECT, and DELETE SQL queries for us! We don't write any SQL.
 */
@Repository
public interface ZonePricingRepository extends JpaRepository<ZonePricing, String> {
}
