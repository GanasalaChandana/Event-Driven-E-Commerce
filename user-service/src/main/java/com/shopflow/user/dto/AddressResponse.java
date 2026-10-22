package com.shopflow.user.dto;

import com.shopflow.user.entity.Address;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data @Builder
public class AddressResponse {
    private UUID id;
    private String fullName;
    private String line1;
    private String line2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String phone;
    private boolean defaultAddress;

    public static AddressResponse from(Address a) {
        return AddressResponse.builder()
                .id(a.getId())
                .fullName(a.getFullName())
                .line1(a.getLine1())
                .line2(a.getLine2())
                .city(a.getCity())
                .state(a.getState())
                .postalCode(a.getPostalCode())
                .country(a.getCountry())
                .phone(a.getPhone())
                .defaultAddress(a.isDefaultAddress())
                .build();
    }
}
