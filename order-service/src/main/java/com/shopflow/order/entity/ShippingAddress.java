package com.shopflow.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/** Delivery address copied onto the order when it is placed, so later edits to the address book don't change it. */
@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShippingAddress {

    @Column(name = "ship_name", length = 100)
    private String fullName;

    @Column(name = "ship_line1", length = 150)
    private String line1;

    @Column(name = "ship_line2", length = 150)
    private String line2;

    @Column(name = "ship_city", length = 100)
    private String city;

    @Column(name = "ship_state", length = 100)
    private String state;

    @Column(name = "ship_postal_code", length = 20)
    private String postalCode;

    @Column(name = "ship_country", length = 2)
    private String country;

    @Column(name = "ship_phone", length = 30)
    private String phone;
}
