package com.shopflow.inventory.controller;

import com.shopflow.inventory.dto.InventoryAdjustRequest;
import com.shopflow.inventory.dto.InventoryRequest;
import com.shopflow.inventory.dto.InventoryResponse;
import com.shopflow.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // Admin-only: paginated stock list (used by the MCP server's low-stock report)
    @GetMapping
    public ResponseEntity<Page<InventoryResponse>> list(
            @PageableDefault(size = 100, sort = "productName") Pageable pageable,
            Authentication auth) {
        if (!isAdmin(auth)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(inventoryService.list(pageable));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getStock(@PathVariable UUID productId) {
        return ResponseEntity.ok(inventoryService.getByProductId(productId));
    }

    // Admin-only: set stock to an absolute quantity
    @PutMapping("/{productId}")
    public ResponseEntity<InventoryResponse> adjustStock(
            @PathVariable UUID productId,
            @Valid @RequestBody InventoryAdjustRequest request,
            Authentication auth) {
        if (!isAdmin(auth)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(inventoryService.adjustStock(productId, request));
    }

    // Admin-only: add stock to a product
    @PostMapping
    public ResponseEntity<InventoryResponse> addStock(
            @Valid @RequestBody InventoryRequest request,
            Authentication auth) {
        if (!isAdmin(auth)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(inventoryService.addStock(request));
    }

    private static boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.equals(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
}
