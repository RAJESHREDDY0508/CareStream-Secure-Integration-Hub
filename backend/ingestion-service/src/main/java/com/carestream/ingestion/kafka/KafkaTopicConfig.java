package com.carestream.ingestion.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Creates Kafka topics on startup if they don't already exist.
 * Replication factor = 1 for local dev; override to 3 in production.
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic patientAdmissionTopic() {
        return TopicBuilder.name("patient.admission")
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic patientDischargeTopic() {
        return TopicBuilder.name("patient.discharge")
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic patientTransferTopic() {
        return TopicBuilder.name("patient.transfer")
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic auditEventsTopic() {
        return TopicBuilder.name("audit.events")
                .partitions(12)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic securityAlertsTopic() {
        return TopicBuilder.name("security.alerts")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic vulnerabilityFindingsTopic() {
        return TopicBuilder.name("vulnerability.findings")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic dlqPatientEventsTopic() {
        return TopicBuilder.name("dlq.patient.events")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
