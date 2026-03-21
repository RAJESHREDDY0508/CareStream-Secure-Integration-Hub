package com.carestream.audit.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;

/**
 * Wires the auditErrorHandler into a named container factory.
 * The AuditEventConsumer references this factory by name.
 */
@Configuration
@RequiredArgsConstructor
public class AuditKafkaListenerContainerFactoryConfig {

    private final ConsumerFactory<String, String> consumerFactory;
    private final DefaultErrorHandler auditErrorHandler;

    @Bean(name = "auditKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> auditKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(auditErrorHandler);
        return factory;
    }
}
