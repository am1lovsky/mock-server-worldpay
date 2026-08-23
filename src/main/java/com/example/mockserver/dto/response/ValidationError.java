package com.example.mockserver.dto.response;

public record ValidationError(String queryParameter, String errorName, String message) {
}
