package com.teslamate.query.api.v1;

import com.teslamate.query.dao.HealthDao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health")
public class HealthController {

    private final HealthDao healthDao;

    public HealthController(HealthDao healthDao) {
        this.healthDao = healthDao;
    }

    @GetMapping
    @Operation(summary = "Service and database health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("timestamp", Instant.now().toString());
        try {
            body.put("database", healthDao.ping() == 1 ? "UP" : "DOWN");
        } catch (Exception e) {
            body.put("status", "DEGRADED");
            body.put("database", "DOWN");
            body.put("databaseError", e.getMessage());
        }
        return body;
    }
}
