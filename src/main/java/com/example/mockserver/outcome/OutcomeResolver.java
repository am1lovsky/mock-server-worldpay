package com.example.mockserver.outcome;

import com.example.mockserver.dto.request.AccountVerificationRequest;
import com.example.mockserver.dto.request.PersonalDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Deterministically maps a request to a {@link VerificationOutcome}.
 * <p>
 * The mock is driven by a "magic value" convention: whatever name the caller submits
 * as the beneficiary's name is upper-cased and looked up against the outcome list.
 * A recognized name (e.g. {@code "PARTIAL MATCH"}, {@code "ACCOUNT CLOSED"}) returns
 * that exact outcome; any other name — including a real customer's name like
 * {@code "John Smith"} — falls back to {@code fullMatch}. This keeps the happy path
 * realistic while giving tests a reliable knob for every documented edge case,
 * without adding any non-standard fields to the request contract.
 */
@Component
public class OutcomeResolver {

    private static final Map<String, VerificationOutcome> MAGIC_VALUES = Arrays.stream(VerificationOutcome.values())
            .collect(Collectors.toMap(VerificationOutcome::magicValue, o -> o));

    private static final VerificationOutcome DEFAULT_OUTCOME = VerificationOutcome.FULL_MATCH;

    public VerificationOutcome resolve(AccountVerificationRequest request) {
        String magicValue = normalize(extractSubmittedName(request));
        return MAGIC_VALUES.getOrDefault(magicValue, DEFAULT_OUTCOME);
    }

    /**
     * The name the bank would have "on file", returned in {@code actualAccountHolderName}.
     * Per the docs this field is only populated when it differs from the submitted name
     * (the {@code fullMatch} example returns an empty string), so the mock only fabricates
     * a value for the outcomes where a differing-but-known name makes sense.
     */
    public String resolveActualAccountHolderName(VerificationOutcome outcome, AccountVerificationRequest request) {
        return switch (outcome) {
            case PARTIAL_MATCH, BUSINESS_ACCOUNT_CLOSE_MATCH, PERSONAL_ACCOUNT_CLOSE_MATCH, NO_MATCH -> {
                String submitted = extractSubmittedName(request);
                yield StringUtils.hasText(submitted) ? submitted.trim() + " (BANK RECORD)" : "";
            }
            default -> "";
        };
    }

    private String extractSubmittedName(AccountVerificationRequest request) {
        var party = request.instructions().getFirst().party();
        var payoutInstrument = party.payoutInstrument();
        if (payoutInstrument != null && StringUtils.hasText(payoutInstrument.accountHolderName())) {
            return payoutInstrument.accountHolderName();
        }
        PersonalDetails personalDetails = party.personalDetails();
        if (personalDetails != null) {
            if (StringUtils.hasText(personalDetails.companyName())) {
                return personalDetails.companyName();
            }
            String full = (nullToEmpty(personalDetails.firstName()) + " " + nullToEmpty(personalDetails.lastName())).trim();
            if (StringUtils.hasText(full)) {
                return full;
            }
        }
        return "";
    }

    private String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
