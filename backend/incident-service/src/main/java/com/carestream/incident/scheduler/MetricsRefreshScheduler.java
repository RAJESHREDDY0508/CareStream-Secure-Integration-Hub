package com.carestream.incident.scheduler;

import com.carestream.incident.metrics.IncidentMetrics;
import com.carestream.incident.repository.IncidentRepository;
import com.carestream.incident.entity.IncidentSeverity;
import com.carestream.incident.entity.IncidentStatus;
import com.carestream.incident.service.IncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Periodically refreshes Prometheus gauges from the database.
 * Runs every 2 minutes — frequent enough for dashboard responsiveness.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsRefreshScheduler {

    private final IncidentRepository incidentRepository;
    private final IncidentMetrics    metrics;
    private final IncidentService    incidentService;

    @Scheduled(fixedRateString = "${metrics.refresh.interval-ms:120000}")
    public void refreshMetrics() {
        try {
            long open     = incidentRepository.countByStatus(IncidentStatus.OPEN);
            long critical = incidentRepository.countBySeverity(IncidentSeverity.CRITICAL);

            Map<String, Object> stats = incidentService.getStats();
            double mttr = stats.get("mttrHours") instanceof Number n ? n.doubleValue() : 0.0;

            metrics.refreshGauges(open, critical, mttr);
            log.debug("[METRICS-REFRESH] open={} critical={} mttrHours={}", open, critical, mttr);
        } catch (Exception e) {
            log.warn("[METRICS-REFRESH] Failed to refresh gauges: {}", e.getMessage());
        }
    }
}
