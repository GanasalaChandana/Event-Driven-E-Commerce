package com.shopflow.order.dto;

import com.shopflow.order.entity.ShippingAddress;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Locale;

/** Delivery address on an order — used for the request and the response. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingAddressDto {

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Address line 1 is required")
    @Size(max = 150)
    private String line1;

    @Size(max = 150)
    private String line2;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @NotBlank(message = "State / region is required")
    @Size(max = 100)
    private String state;

    @NotBlank(message = "Postal code is required")
    @Size(max = 20)
    private String postalCode;

    @NotBlank(message = "Country is required")
    @Pattern(regexp = "^[A-Za-z]{2}$", message = "Country must be a 2-letter code")
    private String country;

    @Size(max = 30)
    private String phone;

    public ShippingAddress toEntity() {
        return ShippingAddress.builder()
                .fullName(fullName.trim())
                .line1(line1.trim())
                .line2(blankToNull(line2))
                .city(city.trim())
                .state(state.trim())
                .postalCode(postalCode.trim())
                .country(country.trim().toUpperCase(Locale.ROOT))
                .phone(blankToNull(phone))
                .build();
    }

    /** Null for orders placed before addresses were collected. */
    public static ShippingAddressDto from(ShippingAddress a) {
        if (a == null || a.getLine1() == null) {
            return null;
        }
        return new ShippingAddressDto(a.getFullName(), a.getLine1(), a.getLine2(), a.getCity(), a.getState(),
                a.getPostalCode(), a.getCountry(), a.getPhone());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
