package com.carestream.incident.metrics;

import io.micrometer.core.instrument.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom Micrometer metrics for the Incident Response service.
 *
 * Exposes Prometheus-scrapeable metrics at /actuator/prometheus:
 *   carestream_incidents_created_total
 *   carestream_incidents_resolved_total
 *   carestream_incidents_open          (gauge)
 *   carestream_incidents_critical_open (gauge)
 *   carestream_threat_events_received_total
 *   carestream_alerts_sent_total
 *   carestream_incident_mttr_hours     (gauge — avg mean-time-to-resolve)
 *   carestream_attack_simulations_total
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentMetrics {

    private final MeterRegistry meterRegistry;

    // ── Counters ──────────────────────────────────────────────────────────
    private Counter incidentsCreatedCounter;
    private Counter incidentsResolvedCounter;
    private Counter threatEventsReceivedCounter;
    private Counter alertsSentCounter;
    private Counter attackSimulationsCounter;
    private Counter falsePositivesCounter;

    // ── Gauges ────────────────────────────────────────────────────────────
    private final AtomicLong openIncidents     = new AtomicLong(0);
    private final AtomicLong criticalOpen      = new AtomicLong(0);
    private final AtomicLong mttrHundredths    = new AtomicLong(0); // stored as hours * 100

    @PostConstruct
    public void registerMetrics() {
        incidentsCreatedCounter = Counter.builder("carestream.incidents.created.total")
                .description("Total incidents created (auto + manual)")
                .tag("service", "incident-service")
                .register(meterRegistry);

        incidentsResolvedCounter = Counter.builder("carestream.incidents.resolved.total")
                .description("Total incidents resolved")
                .tag("service", "incident-service")
                .register(meterRegistry);

        threatEventsReceivedCounter = Counter.builder("carestream.threat.events.received.total")
                .description("Total threat events received from Kafka security.alerts")
                .tag("service", "incident-service")
                .register(meterRegistry);

        alertsSentCounter = Counter.builder("carestream.alerts.sent.total")
                .description("Total simulated alert notifications dispatched")
                .tag("service", "incident-service")
                .register(meterRegistry);

        attackSimulationsCounter = Counter.builder("carestream.attack.simulations.total")
                .description("Total attack simulations triggered via demo endpoint")
                .tag("service", "incident-service")
                .register(meterRegistry);

        falsePositivesCounter = Counter.builder("carestream.incidents.false.positives.total")
                .description("Total incidents marked as false positive")
                .tag("service", "incident-service")
                .register(meterRegistry);

        Gauge.builder("carestream.incidents.open", openIncidents, AtomicLong::doubleValue)
                .description("Current number of open incidents")
                .tag("service", "incident-service")
                .register(meterRegistry);

        Gauge.builder("carestream.incidents.critical.open", criticalOpen, AtomicLong::doubleValue)
                .description("Current number of open CRITICAL incidents")
                .tag("service", "incident-service")
                .register(meterRegistry);

        Gauge.builder("carestream.incident.mttr.hours", mttrHundredths,
                    v -> v.doubleValue() / 100.0)
                .description("Mean time to resolve incidents (hours)")
                .tag("service", "incident-service")
                .register(meterRegistry);

        log.info("[METRICS] IncidentMetrics registered with Micrometer");
    }

    // ── Public update methods ─────────────────────────────────────────────

    public void recordIncidentCreated(String severity) {
        incidentsCreatedCounter.increment();
        Counter.builder("carestream.incidents.created.by.severity.total")
                .description("Incidents created by severity")
                .tag("severity", severity)
                .tag("service", "incident-service")
                .register(meterRegistry)
                .increment();
    }

    public void recordIncidentResolved() {
        incidentsResolvedCounter.increment();
    }

    public void recordThreatEventReceived() {
        threatEventsReceivedCounter.increment();
    }

    public void recordAlertSent(String channel) {
        alertsSentCounter.increment();
        Counter.builder("carestream.alerts.sent.by.channel.total")
                .description("Alerts sent by notification channel")
                .tag("channel", channel)
                .tag("service", "incident-service")
                .register(meterRegistry)
                .increment();
    }

    public void recordAttackSimulation(String scenario) {
        attackSimulationsCounter.increment();
        Counter.builder("carestream.attack.simulations.by.scenario.total")
                .description("Attack simulations by scenario type")
                .tag("scenario", scenario)
                .tag("service", "incident-service")
                .register(meterRegistry)
                .increment();
    }

    public void recordFalsePositive() {
        falsePositivesCounter.increment();
    }

    /** Refresh point-in-time gauges — called periodically from a scheduler. */
    public void refreshGauges(long open, long critical, double mttrHours) {
        openIncidents.set(open);
        criticalOpen.set(critical);
        mttrHundredths.set((long)(mttrHours * 100));
    }
}
