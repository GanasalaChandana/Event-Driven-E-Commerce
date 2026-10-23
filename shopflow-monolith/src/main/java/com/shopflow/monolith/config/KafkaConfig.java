package com.shopflow.monolith.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.JsonMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    /**
     * Topic auto-creation makes the Kafka admin client connect at startup, so it is only registered when a broker is
     * actually configured. Without one (e.g. the Render demo) startup no longer waits on a broker that is not there.
     */
    @Configuration
    @ConditionalOnExpression("'${SPRING_KAFKA_BOOTSTRAP_SERVERS:}' != ''")
    static class TopicConfig {

        @Bean public NewTopic userRegisteredTopic()    { return TopicBuilder.name("user.registered").partitions(1).replicas(1).build(); }
        @Bean public NewTopic orderCreatedTopic()      { return TopicBuilder.name("order.created").partitions(1).replicas(1).build(); }
        @Bean public NewTopic orderConfirmedTopic()    { return TopicBuilder.name("order.confirmed").partitions(1).replicas(1).build(); }
        @Bean public NewTopic orderCancelledTopic()    { return TopicBuilder.name("order.cancelled").partitions(1).replicas(1).build(); }
        @Bean public NewTopic inventoryReservedTopic() { return TopicBuilder.name("inventory.reserved").partitions(1).replicas(1).build(); }
        @Bean public NewTopic inventoryFailedTopic()   { return TopicBuilder.name("inventory.failed").partitions(1).replicas(1).build(); }
        @Bean public NewTopic inventoryReleaseTopic()  { return TopicBuilder.name("inventory.release").partitions(1).replicas(1).build(); }
    }

    // Map plain-JSON payloads onto @KafkaListener parameter types (used if listeners are enabled)
    @Bean
    public RecordMessageConverter kafkaMessageConverter() {
        return new JsonMessageConverter();
    }

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> kafkaOperations) {
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(kafkaOperations), new FixedBackOff(1000L, 3L));
    }
}
