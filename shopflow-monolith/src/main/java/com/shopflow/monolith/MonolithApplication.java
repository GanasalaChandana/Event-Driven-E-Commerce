package com.shopflow.monolith;

import com.shopflow.inventory.InventoryServiceApplication;
import com.shopflow.order.OrderServiceApplication;
import com.shopflow.product.ProductServiceApplication;
import com.shopflow.user.UserServiceApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(
    basePackages = {
        "com.shopflow.user",
        "com.shopflow.product",
        "com.shopflow.order",
        "com.shopflow.inventory",
        "com.shopflow.monolith"
    },
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {
            UserServiceApplication.class,
            ProductServiceApplication.class,
            OrderServiceApplication.class,
            InventoryServiceApplication.class
        }
    )
)
public class MonolithApplication {
    public static void main(String[] args) {
        SpringApplication.run(MonolithApplication.class, args);
    }
}
