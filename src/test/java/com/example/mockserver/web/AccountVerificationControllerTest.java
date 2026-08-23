package com.example.mockserver.web;

import com.example.mockserver.dto.request.AccountVerificationRequest;
import com.example.mockserver.dto.request.Address;
import com.example.mockserver.dto.request.Instruction;
import com.example.mockserver.dto.request.Merchant;
import com.example.mockserver.dto.request.Party;
import com.example.mockserver.dto.request.PayoutInstrument;
import com.example.mockserver.dto.request.PersonalDetails;
import com.example.mockserver.outcome.VerificationOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountVerificationControllerTest {

    private static final String API_VERSION = "2025-01-01";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${mock.security.username}")
    private String username;

    @Value("${mock.security.password}")
    private String password;

    @Test
    void acceptsTheExampleRequestFromTheAssignmentDocAndReturnsFullMatch() throws Exception {
        String body = """
                {
                  "merchant": { "entity": "default" },
                  "instructions": [
                    {
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
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/accountVerifications")
                        .header("Authorization", basicAuthHeader())
                        .header("WP-Api-Version", API_VERSION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("fullMatch"))
                .andExpect(jsonPath("$.message").value("Account Details Matched"))
                .andExpect(jsonPath("$.actualAccountHolderName").value(""));
    }

    @ParameterizedTest
    @EnumSource(VerificationOutcome.class)
    void everyDocumentedOutcomeIsReachableViaTheMagicAccountHolderName(VerificationOutcome outcome) throws Exception {
        String requestJson = objectMapper.writeValueAsString(requestWithAccountHolderName(outcome.magicValue()));

        mockMvc.perform(post("/accountVerifications")
                        .header("Authorization", basicAuthHeader())
                        .header("WP-Api-Version", API_VERSION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value(outcome.code()))
                .andExpect(jsonPath("$.message").value(outcome.defaultMessage()));
    }

    @Test
    void missingAuthorizationHeaderReturns401() throws Exception {
        String requestJson = objectMapper.writeValueAsString(requestWithAccountHolderName("John Smith"));

        mockMvc.perform(post("/accountVerifications")
                        .header("WP-Api-Version", API_VERSION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorName").value("unauthorized"));
    }

    @Test
    void wrongCredentialsReturn401() throws Exception {
        String requestJson = objectMapper.writeValueAsString(requestWithAccountHolderName("John Smith"));
        String badAuth = "Basic " + Base64.getEncoder().encodeToString("wrong:credentials".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(post("/accountVerifications")
                        .header("Authorization", badAuth)
                        .header("WP-Api-Version", API_VERSION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorName").value("invalidCredentials"));
    }

    @Test
    void missingApiVersionHeaderReturns400WithValidationError() throws Exception {
        String requestJson = objectMapper.writeValueAsString(requestWithAccountHolderName("John Smith"));

        mockMvc.perform(post("/accountVerifications")
                        .header("Authorization", basicAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorName").value("validationFailure"))
                .andExpect(jsonPath("$.validationErrors[?(@.queryParameter == 'WP-Api-Version')]").exists());
    }

    @Test
    void missingRequiredFieldReturns400WithFieldPath() throws Exception {
        String body = """
                { "merchant": { "entity": "default" }, "instructions": [] }
                """;

        mockMvc.perform(post("/accountVerifications")
                        .header("Authorization", basicAuthHeader())
                        .header("WP-Api-Version", API_VERSION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[?(@.queryParameter == 'instructions')]").exists());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/accountVerifications")
                        .header("Authorization", basicAuthHeader())
                        .header("WP-Api-Version", API_VERSION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorName").value("validationFailure"));
    }

    private String basicAuthHeader() {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    private AccountVerificationRequest requestWithAccountHolderName(String accountHolderName) {
        PayoutInstrument instrument = new PayoutInstrument(
                "bankAccount", "GBP", null, "123456", accountHolderName, "checking",
                null, null, null, null, new Address("home", "1 Main St", null, "London", "SW1A 1AA", "GB"));
        Party party = new Party("beneficiary", instrument,
                new PersonalDetails(null, null, "John", null, "Smith", null, null, null, null, null));
        return new AccountVerificationRequest(new Merchant("default"), List.of(new Instruction(party, null)));
    }
}
