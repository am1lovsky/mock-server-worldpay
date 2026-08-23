# BAV Mock Server

A local mock of Worldpay's **Beneficiary Account Verifications** API —
`POST /accountVerifications` — matching the documented request/response shapes,
status codes, and headers, so deposit/payout flows can be developed and tested
without touching Worldpay's real (rate-limited, credentialed) sandbox.

Source of truth: [Worldpay BAV API docs](https://docs.worldpay.com/access/products/account-verifications/openapi/other/postaccountverifications).

## Running it

```
./mvnw spring-boot:run
```

Starts on `http://localhost:8080`. Run the tests with `./mvnw test`.

## Auth

HTTP Basic, checked on every call to `/accountVerifications`. Configured in
`src/main/resources/application.properties`:

```
mock.security.username=demo-merchant
mock.security.password=demo-secret
```

Missing/malformed `Authorization` header → `401 {"errorName":"unauthorized",...}`.
Wrong credentials → `401 {"errorName":"invalidCredentials",...}`.

## Choosing an outcome deterministically

The mock is driven by a **magic value** convention, the same idea Worldpay's own
sandbox uses for testing: whatever name you submit as the beneficiary is checked
against the list below (case-insensitive, whitespace-normalised). A match returns
that exact `outcome`; anything else — including a real name like `"John Smith"` —
falls back to `fullMatch`. This needs no non-standard fields, so every request stays
a valid, spec-shaped payload.

Set it via **any one** of:
- `instructions[0].party.payoutInstrument.accountHolderName`
- `instructions[0].party.personalDetails.firstName` + `lastName` (concatenated)
- `instructions[0].party.personalDetails.companyName`

| Magic value (any case)         | `outcome`                     | Meaning |
|---------------------------------|--------------------------------|---------|
| *(anything unrecognised)*       | `fullMatch`                   | Account details matched (default) |
| `FULL MATCH`                     | `fullMatch`                   | Account details matched |
| `BUSINESS ACCOUNT NAME MATCHED`  | `businessAccountNameMatched`  | Name matched, business account |
| `PARTIAL MATCH`                  | `partialMatch`                | Close match found |
| `BUSINESS ACCOUNT CLOSE MATCH`   | `businessAccountCloseMatch`   | Close match, business account |
| `NO MATCH`                       | `noMatch`                     | Name/type do not match |
| `ACCOUNT DOES NOT EXIST`         | `accountDoesNotExist`         | Account does not exist |
| `NO RESPONSE`                    | `noResponse`                  | Bank did not respond, retry later |
| `SYSTEM ERROR`                   | `""` (empty)                  | Unexpected system error, retry later |
| `ACCOUNT NOT SUPPORTED`          | `accountNotSupported`         | Account doesn't support name checks |
| `ACCOUNT SWITCHED`               | `accountSwitched`             | Switched via Current Account Switching Service |
| `NOT ENROLLED`                   | `notEnrolled`                 | Bank doesn't accept name check requests |
| `WRONG PARTICIPANT`              | `wrongParticipant`            | Cannot complete for this account/sort code |
| `SECONDARY ACCOUNT ID NOT FOUND` | `secondaryAccountIdNotFound`  | Secondary account ID invalid |
| `PERSONAL ACCOUNT NAME MATCHED`  | `personalAccountNameMatched`  | Name matched, personal account |
| `PERSONAL ACCOUNT CLOSE MATCH`   | `personalAccountCloseMatch`   | Close match, personal account |
| `ACCOUNT ACTIVE`                 | `accountActive`               | Active, but name match unavailable |
| `CANNOT VALIDATE`                | `cannotValidate`              | Unable to validate account details |
| `ACCOUNT CLOSED`                 | `accountClosed`               | Closed / unavailable for payments |

The full enum lives in [`VerificationOutcome`](src/main/java/com/example/mockserver/outcome/VerificationOutcome.java);
the lookup logic is in [`OutcomeResolver`](src/main/java/com/example/mockserver/outcome/OutcomeResolver.java).

For the four "close/no match" outcomes (`partialMatch`, `businessAccountCloseMatch`,
`personalAccountCloseMatch`, `noMatch`) the mock also fills in `actualAccountHolderName`
with a deterministic variant of the submitted name, since the real API only
populates that field when it differs from what you sent.

## Examples

All examples assume `demo-merchant` / `demo-secret` and header
`WP-Api-Version: 2025-01-01`.

### 200 — full match (default)

```bash
curl -s -u demo-merchant:demo-secret \
  -H "WP-Api-Version: 2025-01-01" -H "Content-Type: application/json" \
  -X POST http://localhost:8080/accountVerifications -d '{
    "merchant": { "entity": "default" },
    "instructions": [{
      "party": {
        "type": "beneficiary",
        "payoutInstrument": {
          "type": "bankAccount", "currency": "GBP",
          "accountHolderName": "John Smith", "accountNumber": 123456,
          "accountType": "checking",
          "address": { "countryCode": "GB" }
        },
        "personalDetails": { "firstName": "John", "lastName": "Smith" }
      }
    }]
  }'
# { "outcome": "fullMatch", "message": "Account Details Matched", "actualAccountHolderName": "" }
```

### 200 — requesting a specific outcome (partial match)

Only the `accountHolderName` line changes:

```bash
curl -s -u demo-merchant:demo-secret \
  -H "WP-Api-Version: 2025-01-01" -H "Content-Type: application/json" \
  -X POST http://localhost:8080/accountVerifications -d '{
    "merchant": { "entity": "default" },
    "instructions": [{
      "party": {
        "type": "beneficiary",
        "payoutInstrument": {
          "type": "bankAccount", "currency": "GBP",
          "accountHolderName": "PARTIAL MATCH", "accountNumber": 123456,
          "accountType": "checking",
          "address": { "countryCode": "GB" }
        },
        "personalDetails": { "firstName": "John", "lastName": "Smith" }
      }
    }]
  }'
# { "outcome": "partialMatch", "message": "Close match found", "actualAccountHolderName": "PARTIAL MATCH (BANK RECORD)" }
```

Swap `"PARTIAL MATCH"` for any magic value from the table above to get any other outcome.

### 401 — missing/invalid credentials

```bash
curl -s -H "WP-Api-Version: 2025-01-01" -H "Content-Type: application/json" \
  -X POST http://localhost:8080/accountVerifications -d '{...}'
# 401 { "errorName": "unauthorized", "message": "An Authorization header with HTTP Basic credentials is required" }
```

### 400 — validation error (missing required field)

```bash
curl -s -u demo-merchant:demo-secret \
  -H "WP-Api-Version: 2025-01-01" -H "Content-Type: application/json" \
  -X POST http://localhost:8080/accountVerifications -d '{ "merchant": { "entity": "default" }, "instructions": [] }'
# 400
# {
#   "validationErrors": [
#     { "queryParameter": "instructions", "errorName": "required", "message": "instructions must contain exactly one entry" }
#   ],
#   "errorName": "validationFailure",
#   "message": "The request failed validation. See validationErrors for details."
# }
```

Every documented required field is validated the same way — merchant.entity,
party.type, payoutInstrument.type/currency/accountType, an iban-or-accountNumber,
address.countryCode, and a beneficiary name (accountHolderName, or
firstName+lastName, or companyName). All violations on a request are returned
together in one response instead of stopping at the first one.

## Design decisions

- **Magic-value outcome selection** instead of a custom header/query param: it
  mirrors how Worldpay's own sandbox is documented to work, keeps every request a
  spec-valid payload, and needs no client-side wiring beyond "use this test name."
- **No Spring Security dependency** for Basic auth: a single `HandlerInterceptor`
  gives full control over the exact `{errorName, message}` 401 body the docs
  specify, with far less configuration than a `SecurityFilterChain`.
- **Manual validation over Bean Validation annotations**: lets every error map to
  the exact documented `queryParameter`/`errorName`/`message` shape (including the
  JSON-path-style field names) and collects *all* violations per request, not just
  the first — more useful for a caller iterating on a broken payload.
- **`accountNumber` modelled as `String`**, not `Long`: the spec example uses a
  JSON number, but real account numbers are frequently alphanumeric outside
  GB/US. Jackson coerces the numeric example into the String field transparently.
- **Not implemented** (out of scope for a 60-minute mock): merchant-entity lookup
  against a real onboarding record, IBAN/sort-code checksum validation, and
  enforcing a specific `WP-Api-Version` value (only presence is checked).

## Project layout

```
src/main/java/com/example/mockserver/
  dto/request/    Request payload records (mirrors the documented schema)
  dto/response/   Response payload records (200 / 400 / 401 shapes)
  outcome/        VerificationOutcome enum + magic-value resolver
  validation/     Request validation, collected into validationErrors[]
  security/       HTTP Basic auth interceptor
  web/            Controller + global exception handler
```
