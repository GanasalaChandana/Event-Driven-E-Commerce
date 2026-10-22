package com.shopflow.user.controller;

import com.shopflow.user.dto.AddressRequest;
import com.shopflow.user.dto.AddressResponse;
import com.shopflow.user.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** The signed-in user's saved delivery addresses. */
@RestController
@RequestMapping("/api/v1/users/me/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public List<AddressResponse> list(@AuthenticationPrincipal UserDetails user) {
        return addressService.list(user.getUsername());
    }

    @PostMapping
    public ResponseEntity<AddressResponse> create(@AuthenticationPrincipal UserDetails user,
                                                  @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.create(user.getUsername(), request));
    }

    @PutMapping("/{id}")
    public AddressResponse update(@AuthenticationPrincipal UserDetails user, @PathVariable UUID id,
                                  @Valid @RequestBody AddressRequest request) {
        return addressService.update(user.getUsername(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserDetails user, @PathVariable UUID id) {
        addressService.delete(user.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
