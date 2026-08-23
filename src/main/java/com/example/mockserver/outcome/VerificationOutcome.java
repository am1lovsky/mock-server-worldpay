package com.example.mockserver.outcome;

/**
 * The full set of {@code outcome} values documented for
 * {@code POST /accountVerifications}. The enum constant name, with underscores
 * turned into spaces, doubles as the "magic value" a caller puts in
 * {@code accountHolderName} (or {@code firstName}/{@code lastName}) to deterministically
 * request that outcome from the mock — see {@link OutcomeResolver}.
 */
public enum VerificationOutcome {

    FULL_MATCH("fullMatch", "Account Details Matched"),
    BUSINESS_ACCOUNT_NAME_MATCHED("businessAccountNameMatched", "Account name matched, but this is a business account"),
    PARTIAL_MATCH("partialMatch", "Close match found"),
    BUSINESS_ACCOUNT_CLOSE_MATCH("businessAccountCloseMatch", "Close match found, but this is a business account"),
    NO_MATCH("noMatch", "Account name and type do not match"),
    ACCOUNT_DOES_NOT_EXIST("accountDoesNotExist", "Account does not exist"),
    NO_RESPONSE("noResponse", "The bank did not respond, try again later"),
    SYSTEM_ERROR("", "An unexpected error occurred, try again later"),
    ACCOUNT_NOT_SUPPORTED("accountNotSupported", "Account does not support name check requests"),
    ACCOUNT_SWITCHED("accountSwitched", "Account was switched via the Current Account Switching Service"),
    NOT_ENROLLED("notEnrolled", "Bank does not accept name check requests"),
    WRONG_PARTICIPANT("wrongParticipant", "Cannot complete the check for the provided account/sort code"),
    SECONDARY_ACCOUNT_ID_NOT_FOUND("secondaryAccountIdNotFound", "Secondary account ID is invalid"),
    PERSONAL_ACCOUNT_NAME_MATCHED("personalAccountNameMatched", "Account name matched, but this is a personal account"),
    PERSONAL_ACCOUNT_CLOSE_MATCH("personalAccountCloseMatch", "Close match found, but this is a personal account"),
    ACCOUNT_ACTIVE("accountActive", "Account is active, but a name match is unavailable"),
    CANNOT_VALIDATE("cannotValidate", "Unable to validate the account details"),
    ACCOUNT_CLOSED("accountClosed", "Account is closed or unavailable for payments");

    private final String code;
    private final String defaultMessage;

    VerificationOutcome(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    /** The magic-value keyword that selects this outcome, e.g. {@code "PARTIAL MATCH"}. */
    public String magicValue() {
        return name().replace('_', ' ');
    }
}
