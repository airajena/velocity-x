package com.velocityx.fraud_detection_service.service;

import com.velocityx.fraud_detection_service.dto.RiskScore;
import com.velocityx.fraud_detection_service.repository.FraudCheckRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeolocationService {
    
    private final FraudCheckRepository fraudCheckRepository;
    
    @Value("${fraud.rules.geo.max-distance-km:500}")
    private Double maxDistanceKm;
    
    @Value("${fraud.rules.geo.time-window-minutes:30}")
    private Integer timeWindowMinutes;
    
    public void checkGeolocationAnomaly(String userId, Double latitude, Double longitude, RiskScore riskScore) {
        if (latitude == null || longitude == null) {
            log.debug("No geolocation data provided for user: {}", userId);
            return;
        }
        
        log.debug("Checking geolocation anomaly for user: {} at ({}, {})", userId, latitude, longitude);
        
        Instant timeWindow = Instant.now().minus(timeWindowMinutes, ChronoUnit.MINUTES);
        var recentChecks = fraudCheckRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                userId, 
                com.velocityx.fraud_detection_service.enums.FraudCheckStatus.APPROVED
        );
        
        if (!recentChecks.isEmpty()) {
            var lastCheck = recentChecks.get(0);
            if (lastCheck.getLatitude() != null && lastCheck.getLongitude() != null) {
                double distance = calculateDistance(
                        latitude, longitude,
                        lastCheck.getLatitude(), lastCheck.getLongitude()
                );
                
                if (distance > maxDistanceKm) {
                    riskScore.addRiskFactor(
                            "GEO_IMPOSSIBLE_TRAVEL",
                            String.format("User traveled %.2f km in %d minutes (max: %.2f km)", 
                                    distance, timeWindowMinutes, maxDistanceKm),
                            35,
                            "HIGH"
                    );
                    log.warn("Impossible travel detected for user {}: {} km in {} minutes", 
                            userId, distance, timeWindowMinutes);
                }
            }
        }
    }
    
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Haversine formula
        final int R = 6371; // Radius of the earth in km
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
}
