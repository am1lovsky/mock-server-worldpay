package com.example.mockserver.dto.request;

import java.util.Map;

public record Instruction(
        Party party,
        Map<String, Object> expandableKeyValuePairs) {
}
