package com.example.mockserver.dto.request;

import java.util.List;

public record PersonalDetails(
        String type,
        String title,
        String firstName,
        String middleName,
        String lastName,
        String dateOfBirth,
        String companyName,
        String dateOfIncorporation,
        String email,
        List<Phone> phones) {
}
