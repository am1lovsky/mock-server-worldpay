package com.example.mockserver.dto.response;

public record AccountVerificationResponse(
        String outcome,
        String message,
        String actualAccountHolderName) {
}
