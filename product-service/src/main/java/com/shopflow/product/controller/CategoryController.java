package com.shopflow.product.controller;

import com.shopflow.product.dto.CategoryRequest;
import com.shopflow.product.dto.CategoryResponse;
import com.shopflow.product.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> list() {
        return ResponseEntity.ok(categoryService.listAll());
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(
            @Valid @RequestBody CategoryRequest request,
            Authentication auth) {
        if (auth == null || auth.getAuthorities().stream()
                .noneMatch(a -> a.equals(new SimpleGrantedAuthority("ROLE_ADMIN")))) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }
}
