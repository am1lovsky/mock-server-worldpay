package com.example.mockserver.web;

import com.example.mockserver.dto.request.AccountVerificationRequest;
import com.example.mockserver.dto.response.AccountVerificationResponse;
import com.example.mockserver.outcome.OutcomeResolver;
import com.example.mockserver.outcome.VerificationOutcome;
import com.example.mockserver.validation.AccountVerificationValidator;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Mock of Worldpay's {@code POST /accountVerifications} (Beneficiary Account Verifications). */
@RestController
@RequestMapping("/accountVerifications")
public class AccountVerificationController {

    private final AccountVerificationValidator validator;
    private final OutcomeResolver outcomeResolver;

    public AccountVerificationController(AccountVerificationValidator validator, OutcomeResolver outcomeResolver) {
        this.validator = validator;
        this.outcomeResolver = outcomeResolver;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountVerificationResponse> verify(
            @RequestHeader(value = "WP-Api-Version", required = false) String apiVersion,
            @RequestBody(required = false) AccountVerificationRequest request) {

        validator.validate(apiVersion, request);

        VerificationOutcome outcome = outcomeResolver.resolve(request);
        String actualAccountHolderName = outcomeResolver.resolveActualAccountHolderName(outcome, request);

        AccountVerificationResponse body = new AccountVerificationResponse(
                outcome.code(),
                outcome.defaultMessage(),
                actualAccountHolderName);

        return ResponseEntity.ok(body);
    }
}
