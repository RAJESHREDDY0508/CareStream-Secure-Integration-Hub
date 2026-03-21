package com.carestream.ingestion.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Creates all Kafka topics on startup (idempotent — skips if already exists).
 *
 * Phase 3 additions:
 *  - Retry topics for non-blocking retry (patient.admission-retry-0/1/2)
 *  - DLQ topic for permanently failed messages
 *
 * Partition strategy:
 *  - patient.* topics: 6 partitions, key=patientId → ordered processing per patient
 *  - retry topics: 3 partitions (lower throughput expected)
 *  - dlq: 3 partitions
 *  - audit.events: 12 partitions (high volume — every patient event produces an audit)
 */
@Configuration
public class KafkaTopicConfig {

    // ─── Core patient event topics ────────────────────────────

    @Bean public NewTopic patientAdmissionTopic() {
        return TopicBuilder.name("patient.admission").partitions(6).replicas(1).build();
    }

    @Bean public NewTopic patientDischargeTopic() {
        return TopicBuilder.name("patient.discharge").partitions(6).replicas(1).build();
    }

    @Bean public NewTopic patientTransferTopic() {
        return TopicBuilder.name("patient.transfer").partitions(6).replicas(1).build();
    }

    // ─── Retry topics (non-blocking retries, Phase 3) ─────────
    // Spring Kafka's @RetryableTopic creates these automatically,
    // but we declare them explicitly for visibility and pre-creation.

    @Bean public NewTopic patientAdmissionRetry0() {
        return TopicBuilder.name("patient.admission-retry-0").partitions(3).replicas(1).build();
    }

    @Bean public NewTopic patientAdmissionRetry1() {
        return TopicBuilder.name("patient.admission-retry-1").partitions(3).replicas(1).build();
    }

    @Bean public NewTopic patientAdmissionRetry2() {
        return TopicBuilder.name("patient.admission-retry-2").partitions(3).replicas(1).build();
    }

    @Bean public NewTopic patientDischargeRetry0() {
        return TopicBuilder.name("patient.discharge-retry-0").partitions(3).replicas(1).build();
    }

    @Bean public NewTopic patientTransferRetry0() {
        return TopicBuilder.name("patient.transfer-retry-0").partitions(3).replicas(1).build();
    }

    // ─── Dead Letter Queue ────────────────────────────────────

    @Bean public NewTopic dlqPatientEvents() {
        return TopicBuilder.name("dlq.patient.events").partitions(3).replicas(1).build();
    }

    // ─── SecOps topics ────────────────────────────────────────

    @Bean public NewTopic auditEventsTopic() {
        return TopicBuilder.name("audit.events").partitions(12).replicas(1).build();
    }

    @Bean public NewTopic securityAlertsTopic() {
        return TopicBuilder.name("security.alerts").partitions(3).replicas(1).build();
    }

    @Bean public NewTopic vulnerabilityFindingsTopic() {
        return TopicBuilder.name("vulnerability.findings").partitions(3).replicas(1).build();
    }
}
