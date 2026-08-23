package com.example.mockserver.outcome;

import com.example.mockserver.dto.request.AccountVerificationRequest;
import com.example.mockserver.dto.request.Party;
import com.example.mockserver.dto.request.PayoutInstrument;
import com.example.mockserver.dto.request.PersonalDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Deterministically maps a request to a {@link VerificationOutcome}.
 * <p>
 * The mock is driven by a "magic value" convention: {@link #submittedBeneficiaryName}
 * reads whatever name the caller filled in for the beneficiary, normalizes it, and
 * looks it up against {@link VerificationOutcome#magicValue()} for every outcome.
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
        String magicValue = normalize(submittedBeneficiaryName(request));
        return MAGIC_VALUES.getOrDefault(magicValue, DEFAULT_OUTCOME);
    }

    /**
     * The name the bank would have "on file", returned in {@code actualAccountHolderName}.
     * Per the docs this field is only populated when it differs from the submitted name
     * (the {@code fullMatch} example returns an empty string), so the mock only fabricates
     * a value for the outcomes where a differing-but-known name makes sense.
     */
    public String resolveActualAccountHolderName(VerificationOutcome outcome, AccountVerificationRequest request) {
        boolean bankKnowsADifferentName = switch (outcome) {
            case PARTIAL_MATCH, BUSINESS_ACCOUNT_CLOSE_MATCH, PERSONAL_ACCOUNT_CLOSE_MATCH, NO_MATCH -> true;
            default -> false;
        };
        if (!bankKnowsADifferentName) {
            return "";
        }
        String submitted = submittedBeneficiaryName(request);
        return StringUtils.hasText(submitted) ? submitted + " (BANK RECORD)" : "";
    }

    /**
     * The name the caller is asserting for the beneficiary, read from whichever
     * field they actually filled in. Checked in priority order:
     * {@code payoutInstrument.accountHolderName}, then {@code personalDetails.companyName},
     * then {@code personalDetails.firstName + lastName}. Returns {@code ""} if none are set.
     */
    private String submittedBeneficiaryName(AccountVerificationRequest request) {
        Party party = request.instructions().getFirst().party();
        return accountHolderName(party)
                .or(() -> companyName(party))
                .or(() -> fullPersonalName(party))
                .orElse("");
    }

    private Optional<String> accountHolderName(Party party) {
        PayoutInstrument payoutInstrument = party.payoutInstrument();
        return payoutInstrument == null ? Optional.empty() : nonBlank(payoutInstrument.accountHolderName());
    }

    private Optional<String> companyName(Party party) {
        PersonalDetails personalDetails = party.personalDetails();
        return personalDetails == null ? Optional.empty() : nonBlank(personalDetails.companyName());
    }

    private Optional<String> fullPersonalName(Party party) {
        PersonalDetails personalDetails = party.personalDetails();
        if (personalDetails == null) {
            return Optional.empty();
        }
        String firstName = personalDetails.firstName() == null ? "" : personalDetails.firstName();
        String lastName = personalDetails.lastName() == null ? "" : personalDetails.lastName();
        return nonBlank((firstName + " " + lastName).trim());
    }

    private Optional<String> nonBlank(String value) {
        return StringUtils.hasText(value) ? Optional.of(value.trim()) : Optional.empty();
    }

    private String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ").toUpperCase();
    }
}
