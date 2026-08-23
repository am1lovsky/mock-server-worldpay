package com.example.mockserver.dto.response;

import java.util.List;

public record BadRequestResponse(
        List<ValidationError> validationErrors,
        String errorName,
        String message) {
}
