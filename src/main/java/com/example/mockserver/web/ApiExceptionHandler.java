package com.example.mockserver.web;

import com.example.mockserver.dto.response.BadRequestResponse;
import com.example.mockserver.dto.response.ValidationError;
import com.example.mockserver.validation.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<BadRequestResponse> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new BadRequestResponse(
                ex.getErrors(),
                "validationFailure",
                "The request failed validation. See validationErrors for details."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BadRequestResponse> handleMalformedJson() {
        ValidationError error = new ValidationError("body", "malformedJson", "Request body is missing or is not valid JSON");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new BadRequestResponse(
                List.of(error),
                "validationFailure",
                "The request body could not be parsed."));
    }
}
