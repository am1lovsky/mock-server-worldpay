package com.example.mockserver.validation;

import com.example.mockserver.dto.response.ValidationError;
import lombok.Getter;

import java.util.List;

@Getter
public class BadRequestException extends RuntimeException {

    private final List<ValidationError> errors;

    public BadRequestException(List<ValidationError> errors) {
        super("Request failed validation: " + errors);
        this.errors = errors;
    }

}
