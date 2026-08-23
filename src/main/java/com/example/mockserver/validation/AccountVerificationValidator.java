package com.example.mockserver.validation;

import com.example.mockserver.dto.request.AccountVerificationRequest;
import com.example.mockserver.dto.request.Address;
import com.example.mockserver.dto.request.Instruction;
import com.example.mockserver.dto.request.Party;
import com.example.mockserver.dto.request.PayoutInstrument;
import com.example.mockserver.dto.request.PersonalDetails;
import com.example.mockserver.dto.response.ValidationError;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validates a request against the documented contract and collects every
 * violation instead of failing on the first one, so a caller can fix a
 * malformed request in a single round trip — the same experience the real
 * Worldpay API's {@code validationErrors[]} shape is designed for.
 */
@Component
public class AccountVerificationValidator {

    private static final Set<String> VALID_ACCOUNT_TYPES =
            Set.of("checking", "savings", "moneyMarket", "certificateOfDeposit", "vista", "other");

    /**
     * @param apiVersion the {@code WP-Api-Version} header value, or {@code null} if absent
     * @throws BadRequestException if the header or body violates the contract
     */
    public void validate(String apiVersion, AccountVerificationRequest request) {
        List<ValidationError> errors = new ArrayList<>();

        if (!StringUtils.hasText(apiVersion)) {
            errors.add(new ValidationError("WP-Api-Version", "required", "The WP-Api-Version header is required"));
        }

        if (request == null || request.merchant() == null || !StringUtils.hasText(request.merchant().entity())) {
            errors.add(new ValidationError("merchant.entity", "required", "merchant.entity is required"));
        }

        if (request == null || request.instructions() == null || request.instructions().isEmpty()) {
            errors.add(new ValidationError("instructions", "required", "instructions must contain exactly one entry"));
        } else {
            if (request.instructions().size() > 1) {
                errors.add(new ValidationError("instructions", "tooMany", "Only one instruction is currently supported"));
            }
            validateInstruction(request.instructions().getFirst(), errors);
        }

        if (!errors.isEmpty()) {
            throw new BadRequestException(errors);
        }
    }

    private void validateInstruction(Instruction instruction, List<ValidationError> errors) {
        Party party = instruction == null ? null : instruction.party();
        if (party == null) {
            errors.add(new ValidationError("instructions[0].party", "required", "party is required"));
            return;
        }
        if (!"beneficiary".equals(party.type())) {
            errors.add(new ValidationError("instructions[0].party.type", "invalid", "party.type must be \"beneficiary\""));
        }

        PayoutInstrument payoutInstrument = party.payoutInstrument();
        if (payoutInstrument == null) {
            errors.add(new ValidationError("instructions[0].party.payoutInstrument", "required", "payoutInstrument is required"));
        } else {
            validatePayoutInstrument(payoutInstrument, errors);
        }

        boolean hasHolderName = payoutInstrument != null && StringUtils.hasText(payoutInstrument.accountHolderName());
        PersonalDetails personalDetails = party.personalDetails();
        boolean hasFirstAndLast = personalDetails != null
                && StringUtils.hasText(personalDetails.firstName())
                && StringUtils.hasText(personalDetails.lastName());
        boolean hasCompanyName = personalDetails != null && StringUtils.hasText(personalDetails.companyName());
        if (!hasHolderName && !hasFirstAndLast && !hasCompanyName) {
            errors.add(new ValidationError(
                    "instructions[0].party",
                    "required",
                    "Provide payoutInstrument.accountHolderName, or personalDetails.firstName + lastName, or personalDetails.companyName"));
        }
    }

    private void validatePayoutInstrument(PayoutInstrument payoutInstrument, List<ValidationError> errors) {
        if (!"bankAccount".equals(payoutInstrument.type())) {
            errors.add(new ValidationError("instructions[0].party.payoutInstrument.type", "invalid", "payoutInstrument.type must be \"bankAccount\""));
        }
        if (!StringUtils.hasText(payoutInstrument.currency())) {
            errors.add(new ValidationError("instructions[0].party.payoutInstrument.currency", "required", "currency (ISO 4217) is required"));
        }
        if (!StringUtils.hasText(payoutInstrument.iban()) && !StringUtils.hasText(payoutInstrument.accountNumber())) {
            errors.add(new ValidationError("instructions[0].party.payoutInstrument", "required", "Either iban or accountNumber must be provided"));
        }
        if (!StringUtils.hasText(payoutInstrument.accountType()) || !VALID_ACCOUNT_TYPES.contains(payoutInstrument.accountType())) {
            errors.add(new ValidationError(
                    "instructions[0].party.payoutInstrument.accountType",
                    "invalid",
                    "accountType must be one of " + VALID_ACCOUNT_TYPES));
        }

        Address address = payoutInstrument.address();
        if (address == null || !StringUtils.hasText(address.countryCode())) {
            errors.add(new ValidationError(
                    "instructions[0].party.payoutInstrument.address.countryCode",
                    "required",
                    "address.countryCode (ISO 3166-1 alpha-2) is required"));
        } else if (!address.countryCode().matches("[A-Za-z]{2}")) {
            errors.add(new ValidationError(
                    "instructions[0].party.payoutInstrument.address.countryCode",
                    "invalid",
                    "address.countryCode must be a 2-letter ISO 3166-1 alpha-2 code"));
        }
    }
}
