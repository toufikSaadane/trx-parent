package com.toufik.trxalertservice.controller;

import com.toufik.trxalertservice.dto.AnalyticsDto;
import com.toufik.trxalertservice.service.AnalyticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Autowired
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/data")
    public ResponseEntity<AnalyticsDto> getAnalyticsData() {
        try {
            log.info("Fetching analytics data");
            AnalyticsDto analytics = analyticsService.getAnalyticsData();
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            log.error("Error fetching analytics data: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}