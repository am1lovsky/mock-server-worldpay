package com.example.mockserver.validation;

import com.example.mockserver.dto.request.AccountVerificationRequest;
import com.example.mockserver.dto.request.Address;
import com.example.mockserver.dto.request.Instruction;
import com.example.mockserver.dto.request.Merchant;
import com.example.mockserver.dto.request.Party;
import com.example.mockserver.dto.request.PayoutInstrument;
import com.example.mockserver.dto.request.PersonalDetails;
import com.example.mockserver.dto.response.ValidationError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AccountVerificationValidatorTest {

    private final AccountVerificationValidator validator = new AccountVerificationValidator();

    @Test
    void acceptsAFullyValidRequest() {
        assertThatCode(() -> validator.validate("2025-01-01", validRequest())).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingApiVersionHeader() {
        assertThat(errorsFor(validator, null, validRequest()))
                .anySatisfy(e -> assertThat(e.queryParameter()).isEqualTo("WP-Api-Version"));
    }

    @Test
    void rejectsMissingMerchantEntity() {
        AccountVerificationRequest request = new AccountVerificationRequest(new Merchant(""), validRequest().instructions());

        assertThat(errorsFor(validator, "2025-01-01", request))
                .anySatisfy(e -> assertThat(e.queryParameter()).isEqualTo("merchant.entity"));
    }

    @Test
    void rejectsMissingAccountIdentifier() {
        PayoutInstrument instrument = new PayoutInstrument(
                "bankAccount", "GBP", null, null, "John Smith", "checking",
                null, null, null, null, new Address("home", "1 Main St", null, "London", "SW1A 1AA", "GB"));
        AccountVerificationRequest request = requestWithInstrument(instrument);

        assertThat(errorsFor(validator, "2025-01-01", request))
                .anySatisfy(e -> assertThat(e.queryParameter()).contains("payoutInstrument"));
    }

    @Test
    void rejectsInvalidAccountType() {
        PayoutInstrument instrument = new PayoutInstrument(
                "bankAccount", "GBP", null, "123456", "John Smith", "not-a-real-type",
                null, null, null, null, new Address("home", "1 Main St", null, "London", "SW1A 1AA", "GB"));
        AccountVerificationRequest request = requestWithInstrument(instrument);

        assertThat(errorsFor(validator, "2025-01-01", request))
                .anySatisfy(e -> assertThat(e.queryParameter()).endsWith("accountType"));
    }

    @Test
    void rejectsMissingCountryCode() {
        PayoutInstrument instrument = new PayoutInstrument(
                "bankAccount", "GBP", null, "123456", "John Smith", "checking",
                null, null, null, null, new Address("home", "1 Main St", null, "London", "SW1A 1AA", null));
        AccountVerificationRequest request = requestWithInstrument(instrument);

        assertThat(errorsFor(validator, "2025-01-01", request))
                .anySatisfy(e -> assertThat(e.queryParameter()).endsWith("countryCode"));
    }

    @Test
    void rejectsMissingBeneficiaryName() {
        PayoutInstrument instrument = new PayoutInstrument(
                "bankAccount", "GBP", null, "123456", null, "checking",
                null, null, null, null, new Address("home", "1 Main St", null, "London", "SW1A 1AA", "GB"));
        Party party = new Party("beneficiary", instrument, null);
        AccountVerificationRequest request = new AccountVerificationRequest(
                new Merchant("default"), List.of(new Instruction(party, null)));

        assertThat(errorsFor(validator, "2025-01-01", request))
                .anySatisfy(e -> assertThat(e.queryParameter()).isEqualTo("instructions[0].party"));
    }

    @Test
    void collectsMultipleErrorsInOnePass() {
        AccountVerificationRequest request = new AccountVerificationRequest(new Merchant(""), List.of());

        assertThat(errorsFor(validator, null, request)).hasSizeGreaterThanOrEqualTo(3);
    }

    private List<ValidationError> errorsFor(AccountVerificationValidator validator, String apiVersion, AccountVerificationRequest request) {
        try {
            validator.validate(apiVersion, request);
            return List.of();
        } catch (BadRequestException e) {
            return e.getErrors();
        }
    }

    private AccountVerificationRequest requestWithInstrument(PayoutInstrument instrument) {
        Party party = new Party("beneficiary", instrument, new PersonalDetails(null, null, "John", null, "Smith", null, null, null, null, null));
        return new AccountVerificationRequest(new Merchant("default"), List.of(new Instruction(party, null)));
    }

    private AccountVerificationRequest validRequest() {
        return requestWithInstrument(new PayoutInstrument(
                "bankAccount", "GBP", null, "123456", "John Smith", "checking",
                null, null, null, null, new Address("home", "1 Main St", null, "London", "SW1A 1AA", "GB")));
    }
}
