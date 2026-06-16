package com.shopflow.monolith.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean public NewTopic userRegisteredTopic()    { return TopicBuilder.name("user.registered").partitions(1).replicas(1).build(); }
    @Bean public NewTopic orderCreatedTopic()      { return TopicBuilder.name("order.created").partitions(1).replicas(1).build(); }
    @Bean public NewTopic orderConfirmedTopic()    { return TopicBuilder.name("order.confirmed").partitions(1).replicas(1).build(); }
    @Bean public NewTopic orderCancelledTopic()    { return TopicBuilder.name("order.cancelled").partitions(1).replicas(1).build(); }
    @Bean public NewTopic inventoryReservedTopic() { return TopicBuilder.name("inventory.reserved").partitions(1).replicas(1).build(); }
    @Bean public NewTopic inventoryFailedTopic()   { return TopicBuilder.name("inventory.failed").partitions(1).replicas(1).build(); }
}
