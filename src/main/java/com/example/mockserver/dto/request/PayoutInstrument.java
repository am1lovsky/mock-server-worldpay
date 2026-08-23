package com.example.mockserver.dto.request;

public record PayoutInstrument(
        String type,
        String currency,
        String iban,
        // Modelled as String (not Long): Jackson happily coerces a JSON number like
        // the spec's 123456 into a String, and real-world account numbers are often
        // alphanumeric outside the UK/US, so a String is the more realistic contract.
        String accountNumber,
        String accountHolderName,
        String accountType,
        String bankCode,
        String bankName,
        String branchCode,
        String swiftBic,
        Address address) {
}
