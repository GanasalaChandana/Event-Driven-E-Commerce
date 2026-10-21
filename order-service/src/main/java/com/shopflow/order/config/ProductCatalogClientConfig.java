package com.shopflow.order.config;

import com.shopflow.order.catalog.ProductCatalog;
import com.shopflow.order.catalog.RemoteProductCatalog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Microservices only — the monolith excludes this class and uses an in-process catalog.
 */
@Configuration
public class ProductCatalogClientConfig {

    @Bean
    public ProductCatalog productCatalog(
            @Value("${services.product.url:http://product-service:8082}") String productServiceUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(5000);

        RestClient client = RestClient.builder()
                .baseUrl(productServiceUrl)
                .requestFactory(requestFactory)
                .build();
        return new RemoteProductCatalog(client);
    }
}
