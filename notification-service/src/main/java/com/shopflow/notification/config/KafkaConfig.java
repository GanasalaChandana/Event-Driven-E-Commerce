package com.shopflow.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.JsonMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableAsync   // Allows @Async on EmailService so Kafka threads are never blocked
public class KafkaConfig {

    /**
     * Producers send plain JSON without type headers. Values are consumed as Strings and this
     * converter maps them onto the @KafkaListener parameter type. Spring Boot applies it to the
     * auto-configured "kafkaListenerContainerFactory".
     */
    @Bean
    public RecordMessageConverter kafkaMessageConverter() {
        return new JsonMessageConverter();
    }

    /** Retry a failing record 3 times, then park it on <topic>.DLT instead of blocking the partition. */
    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> kafkaOperations) {
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(kafkaOperations), new FixedBackOff(1000L, 3L));
    }
}
