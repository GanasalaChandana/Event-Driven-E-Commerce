package com.shopflow.product.service;

import com.shopflow.product.dto.CategoryRequest;
import com.shopflow.product.dto.CategoryResponse;
import com.shopflow.product.entity.Category;
import com.shopflow.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> listAll() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public CategoryResponse create(CategoryRequest request) {
        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return CategoryResponse.from(categoryRepository.save(category));
    }
}
