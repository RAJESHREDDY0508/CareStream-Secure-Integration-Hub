package com.carestream.patient.kafka;

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
 * Phase 3 — Resilient Kafka consumer configuration.
 *
 * Retry strategy (exponential backoff):
 *   Attempt 1 → wait 1s → Attempt 2 → wait 2s → Attempt 3 → wait 4s → DLQ
 *   Max elapsed time before giving up: 30 seconds
 *
 * After exhausting retries, the message is published to dlq.patient.events
 * with headers containing the original topic, partition, offset, and exception details.
 *
 * Non-retryable exceptions (e.g. JSON parse errors) go directly to DLQ.
 */
@Slf4j
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * A minimal KafkaTemplate used ONLY by DeadLetterPublishingRecoverer.
     * Separate from any application-level producer config.
     */
    @Bean(name = "dlqKafkaTemplate")
    public KafkaTemplate<String, String> dlqKafkaTemplate() {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.ACKS_CONFIG, "all",
                ProducerConfig.RETRIES_CONFIG, 3
        );
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    /**
     * Routes dead-lettered messages to dlq.patient.events.
     * Partition is derived from the original record's partition % 3 (dlq has 3 partitions).
     * Spring Kafka automatically adds diagnostic headers (exception class, message, original topic/partition/offset).
     */
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaTemplate<String, String> dlqKafkaTemplate) {

        return new DeadLetterPublishingRecoverer(
                dlqKafkaTemplate,
                (record, exception) -> {
                    int dlqPartition = Math.abs(record.partition() % 3);
                    log.error("[DLQ] Routing failed message to dlq.patient.events | " +
                                    "originalTopic={} partition={} offset={} key={} error={}",
                            record.topic(), record.partition(), record.offset(),
                            record.key(), exception.getMessage());
                    return new TopicPartition("dlq.patient.events", dlqPartition);
                }
        );
    }

    /**
     * Exponential backoff with 3 retry attempts before routing to DLQ.
     *
     * Timeline:
     *   t=0s   → First attempt fails
     *   t=1s   → Retry 1
     *   t=3s   → Retry 2
     *   t=7s   → Retry 3 (still fails → send to DLQ)
     */
    @Bean
    public DefaultErrorHandler defaultErrorHandler(DeadLetterPublishingRecoverer recoverer) {
        ExponentialBackOff backOff = new ExponentialBackOff(1_000L, 2.0);
        backOff.setMaxAttempts(3);
        backOff.setMaxElapsedTime(30_000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);

        // Skip retries for non-transient deserialization errors — go straight to DLQ
        handler.addNotRetryableExceptions(
                com.fasterxml.jackson.core.JsonParseException.class,
                com.fasterxml.jackson.databind.exc.MismatchedInputException.class,
                IllegalArgumentException.class
        );

        handler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("[RETRY] Attempt {} for topic={} partition={} offset={} error={}",
                        deliveryAttempt, record.topic(), record.partition(),
                        record.offset(), ex.getMessage())
        );

        return handler;
    }
}
