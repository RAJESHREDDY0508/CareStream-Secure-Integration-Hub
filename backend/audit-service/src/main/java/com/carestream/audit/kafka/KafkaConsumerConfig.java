package com.carestream.audit.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.Map;

/**
 * Phase 3 — Resilient Kafka consumer config for Audit Service.
 * Same retry strategy as patient-service; shares the same DLQ topic.
 */
@Slf4j
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean(name = "auditDlqKafkaTemplate")
    public KafkaTemplate<String, String> dlqKafkaTemplate() {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,       bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,    StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,  StringSerializer.class,
                ProducerConfig.ACKS_CONFIG, "all",
                ProducerConfig.RETRIES_CONFIG, 3
        );
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Bean(name = "auditEventsKafkaTemplate")
    public KafkaTemplate<String, String> auditEventsKafkaTemplate() {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,       bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,    StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,  StringSerializer.class,
                ProducerConfig.ACKS_CONFIG, "all",
                ProducerConfig.RETRIES_CONFIG, 3
        );
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Bean
    public DeadLetterPublishingRecoverer auditDeadLetterRecoverer(
            KafkaTemplate<String, String> auditDlqKafkaTemplate) {
        return new DeadLetterPublishingRecoverer(
                auditDlqKafkaTemplate,
                (record, exception) -> {
                    int dlqPartition = Math.abs(record.partition() % 3);
                    log.error("[AUDIT-DLQ] Routing to dlq | topic={} partition={} offset={} error={}",
                            record.topic(), record.partition(), record.offset(), exception.getMessage());
                    return new TopicPartition("dlq.patient.events", dlqPartition);
                }
        );
    }

    @Bean
    public DefaultErrorHandler auditErrorHandler(DeadLetterPublishingRecoverer auditDeadLetterRecoverer) {
        ExponentialBackOff backOff = new ExponentialBackOff(1_000L, 2.0);
        backOff.setMaxAttempts(3);
        backOff.setMaxElapsedTime(30_000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(auditDeadLetterRecoverer, backOff);
        handler.addNotRetryableExceptions(
                com.fasterxml.jackson.core.JsonParseException.class,
                IllegalArgumentException.class
        );
        handler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("[AUDIT-RETRY] Attempt {} for topic={} offset={} error={}",
                        deliveryAttempt, record.topic(), record.offset(), ex.getMessage())
        );
        return handler;
    }
}
