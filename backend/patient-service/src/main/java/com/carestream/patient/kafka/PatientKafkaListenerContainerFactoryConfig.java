package com.carestream.patient.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;

/**
 * Wires the DefaultErrorHandler (with DLQ routing + exponential backoff)
 * into the default Kafka listener container factory used by PatientEventConsumer.
 */
@Configuration
@RequiredArgsConstructor
public class PatientKafkaListenerContainerFactoryConfig {

    private final ConsumerFactory<String, String> consumerFactory;
    private final DefaultErrorHandler defaultErrorHandler;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(defaultErrorHandler);
        return factory;
    }
}
