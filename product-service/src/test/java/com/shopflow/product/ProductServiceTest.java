package com.shopflow.product;

import com.shopflow.product.dto.ProductRequest;
import com.shopflow.product.exception.DuplicateSkuException;
import com.shopflow.product.repository.CategoryRepository;
import com.shopflow.product.repository.ProductRepository;
import com.shopflow.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @Mock CategoryRepository categoryRepository;

    @InjectMocks ProductService productService;

    @Test
    void create_throwsOnDuplicateSku() {
        ProductRequest req = new ProductRequest();
        req.setName("iPhone");
        req.setPrice(BigDecimal.valueOf(999));
        req.setSku("IPHONE-15");

        when(productRepository.existsBySku("IPHONE-15")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(req))
                .isInstanceOf(DuplicateSkuException.class)
                .hasMessageContaining("IPHONE-15");
    }
}
