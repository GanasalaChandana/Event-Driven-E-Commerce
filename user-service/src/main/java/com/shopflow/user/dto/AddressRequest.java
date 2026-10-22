package com.shopflow.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddressRequest {

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

    /** Make this the default address. The first address a user saves is always the default. */
    private Boolean defaultAddress;
}
