package com.shopflow.monolith;

import com.shopflow.inventory.InventoryServiceApplication;
import com.shopflow.order.OrderServiceApplication;
import com.shopflow.product.ProductServiceApplication;
import com.shopflow.user.UserServiceApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EntityScan(basePackages = {
    "com.shopflow.user",
    "com.shopflow.product",
    "com.shopflow.order",
    "com.shopflow.inventory"
})
@EnableJpaRepositories(basePackages = {
    "com.shopflow.user",
    "com.shopflow.product",
    "com.shopflow.order",
    "com.shopflow.inventory"
})
@ComponentScan(
    basePackages = {
        "com.shopflow.user",
        "com.shopflow.product",
        "com.shopflow.order",
        "com.shopflow.inventory",
        "com.shopflow.monolith"
    },
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {
                UserServiceApplication.class,
                ProductServiceApplication.class,
                OrderServiceApplication.class,
                InventoryServiceApplication.class
            }
        ),
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = {
                "com\\.shopflow\\.(user|product|order|inventory)\\.config\\.SecurityConfig",
                "com\\.shopflow\\.(user|product|order|inventory)\\.config\\.KafkaConfig",
                "com\\.shopflow\\.(user|product|order|inventory)\\.exception\\.GlobalExceptionHandler"
            }
        )
    }
)
public class MonolithApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MonolithApplication.class);
        app.setAllowBeanDefinitionOverriding(true);
        app.run(args);
    }
}
