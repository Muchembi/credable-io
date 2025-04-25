package io.credable.lms.controller;

import io.credable.lms.dto.CbsTransactionResponse;
import io.credable.lms.service.CbsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author - <a href="https://github.com/muchembi"> muchembi </a>
 * {@code @date} Thu, 24-Apr-2025, 4:48 pm
 */

@RestController
@RequestMapping("/api/v1/lms")
@RequiredArgsConstructor
@Slf4j
public class ScoringEngineController {
    private final CbsService cbsService;

    // Endpoint for Scoring Engine to fetch transaction data
    // Secured via SecurityConfig (Basic Auth)
    @GetMapping("/transactions/{customerNumber}")
    public ResponseEntity<List<CbsTransactionResponse>> getTransactionsForScoring(
            @PathVariable String customerNumber) {

        if (customerNumber == null || customerNumber.isBlank()) {
            log.warn("Received transaction request with blank customer number");
            return ResponseEntity.badRequest().build();
        }
        log.info("Received transaction data request for customer: {} from Scoring Engine", customerNumber);
        try {
            List<CbsTransactionResponse> transactions = cbsService.getCustomerTransactions(customerNumber);
            if (transactions.isEmpty()) {
                log.info("No transactions found for customer {} (or customer invalid in mock)", customerNumber);
                return ResponseEntity.ok(transactions);
            }
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            log.error("Error fetching transactions for scoring engine for customer {}: {}", customerNumber, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
