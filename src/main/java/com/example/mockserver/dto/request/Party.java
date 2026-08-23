package com.example.mockserver.dto.request;

public record Party(
        String type,
        PayoutInstrument payoutInstrument,
        PersonalDetails personalDetails) {
}
