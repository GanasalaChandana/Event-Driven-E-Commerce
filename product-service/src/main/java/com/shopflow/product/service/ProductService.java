package com.shopflow.product.service;

import com.shopflow.product.dto.ProductRequest;
import com.shopflow.product.dto.ProductResponse;
import com.shopflow.product.entity.Category;
import com.shopflow.product.entity.Product;
import com.shopflow.product.exception.DuplicateSkuException;
import com.shopflow.product.exception.ProductNotFoundException;
import com.shopflow.product.repository.CategoryRepository;
import com.shopflow.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public Page<ProductResponse> listAll(Pageable pageable) {
        return productRepository.findAllByActiveTrue(pageable).map(ProductResponse::from);
    }

    public Page<ProductResponse> listByCategory(UUID categoryId, Pageable pageable) {
        return productRepository.findAllByCategoryIdAndActiveTrue(categoryId, pageable).map(ProductResponse::from);
    }

    public Page<ProductResponse> search(String query, Pageable pageable) {
        return productRepository.search(query, pageable).map(ProductResponse::from);
    }

    public ProductResponse getById(UUID id) {
        return productRepository.findByIdAndActiveTrue(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateSkuException(request.getSku());
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId()).orElse(null);
        }

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .sku(request.getSku())
                .category(category)
                .imageUrl(request.getImageUrl())
                .build();

        product = productRepository.save(product);
        log.info("Product created: {} (SKU: {})", product.getId(), product.getSku());
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        // SKU uniqueness check — only if SKU actually changed
        if (!product.getSku().equals(request.getSku()) && productRepository.existsBySku(request.getSku())) {
            throw new DuplicateSkuException(request.getSku());
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId()).orElse(null);
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setSku(request.getSku());
        product.setCategory(category);
        product.setImageUrl(request.getImageUrl());

        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public void delete(UUID id) {
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        // Soft delete — keeps order history intact
        product.setActive(false);
        productRepository.save(product);
        log.info("Product soft-deleted: {}", id);
    }
}
