package com.example.mockserver.dto.request;

public record Address(
        String type,
        String address1,
        String address2,
        String city,
        String postalCode,
        String countryCode) {
}
