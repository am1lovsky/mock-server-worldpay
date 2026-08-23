package com.example.mockserver.outcome;

import com.example.mockserver.dto.request.AccountVerificationRequest;
import com.example.mockserver.dto.request.Address;
import com.example.mockserver.dto.request.Instruction;
import com.example.mockserver.dto.request.Merchant;
import com.example.mockserver.dto.request.Party;
import com.example.mockserver.dto.request.PayoutInstrument;
import com.example.mockserver.dto.request.PersonalDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OutcomeResolverTest {

    private final OutcomeResolver resolver = new OutcomeResolver();

    private static final Set<VerificationOutcome> OUTCOMES_WITH_ACTUAL_NAME = Set.of(
            VerificationOutcome.PARTIAL_MATCH,
            VerificationOutcome.BUSINESS_ACCOUNT_CLOSE_MATCH,
            VerificationOutcome.PERSONAL_ACCOUNT_CLOSE_MATCH,
            VerificationOutcome.NO_MATCH);

    @ParameterizedTest
    @EnumSource(VerificationOutcome.class)
    void resolvesEveryOutcomeFromItsMagicAccountHolderName(VerificationOutcome outcome) {
        AccountVerificationRequest request = requestWithAccountHolderName(outcome.magicValue());

        assertThat(resolver.resolve(request)).isEqualTo(outcome);
    }

    @Test
    void magicValueLookupIsCaseInsensitiveAndTrims() {
        AccountVerificationRequest request = requestWithAccountHolderName("  partial match  ");

        assertThat(resolver.resolve(request)).isEqualTo(VerificationOutcome.PARTIAL_MATCH);
    }

    @Test
    void unrecognisedNameDefaultsToFullMatch() {
        AccountVerificationRequest request = requestWithAccountHolderName("John Smith");

        assertThat(resolver.resolve(request)).isEqualTo(VerificationOutcome.FULL_MATCH);
    }

    @Test
    void fallsBackToFirstAndLastNameWhenAccountHolderNameIsAbsent() {
        AccountVerificationRequest request = requestWithFirstAndLastName("No", "Match");

        assertThat(resolver.resolve(request)).isEqualTo(VerificationOutcome.NO_MATCH);
    }

    @ParameterizedTest
    @EnumSource(VerificationOutcome.class)
    void actualAccountHolderNameIsOnlyPopulatedForCloseOrNoMatchOutcomes(VerificationOutcome outcome) {
        AccountVerificationRequest request = requestWithAccountHolderName(outcome.magicValue());

        String actual = resolver.resolveActualAccountHolderName(outcome, request);

        if (OUTCOMES_WITH_ACTUAL_NAME.contains(outcome)) {
            assertThat(actual).isNotBlank();
        } else {
            assertThat(actual).isEmpty();
        }
    }

    private AccountVerificationRequest requestWithAccountHolderName(String accountHolderName) {
        PayoutInstrument instrument = new PayoutInstrument(
                "bankAccount", "GBP", null, "123456", accountHolderName, "checking",
                null, null, null, null, new Address("home", "1 Main St", null, "London", "SW1A 1AA", "GB"));
        Party party = new Party("beneficiary", instrument, null);
        return new AccountVerificationRequest(new Merchant("default"), List.of(new Instruction(party, null)));
    }

    private AccountVerificationRequest requestWithFirstAndLastName(String firstName, String lastName) {
        PayoutInstrument instrument = new PayoutInstrument(
                "bankAccount", "GBP", null, "123456", null, "checking",
                null, null, null, null, new Address("home", "1 Main St", null, "London", "SW1A 1AA", "GB"));
        PersonalDetails personalDetails = new PersonalDetails(null, null, firstName, null, lastName, null, null, null, null, null);
        Party party = new Party("beneficiary", instrument, personalDetails);
        return new AccountVerificationRequest(new Merchant("default"), List.of(new Instruction(party, null)));
    }
}
