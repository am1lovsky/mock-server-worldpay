package com.example.mockserver.dto.request;

import java.util.List;

public record AccountVerificationRequest(
        Merchant merchant,
        List<Instruction> instructions) {
}
