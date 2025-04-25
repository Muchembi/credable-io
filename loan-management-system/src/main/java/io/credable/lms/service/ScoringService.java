package io.credable.lms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.credable.lms.dto.ScoreQueryResponse;
import io.credable.lms.exception.LoanApplicationException;
import io.credable.lms.model.LoanApplication;
import io.credable.lms.model.LoanStatus;
import io.credable.lms.repo.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.http.HttpHeaders;

/**
 * @author - <a href="https://github.com/muchembi"> muchembi </a>
 * {@code @date} Thu, 24-Apr-2025, 4:37 pm
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ScoringService {

    private final RestClient restClient;
    private final LoanApplicationRepository loanApplicationRepository;
    private final ObjectMapper objectMapper;

    @Value("${scoring.api.base-url}")
    private String scoringApiBaseUrl;

    @Value("${scoring.api.client-token}")
    private String clientToken;

    @Value("${retry.score-query.max-attempts}")
    private int maxAttempts;

    @Value("${scoring.api.mock-enabled:false}")
    private boolean mockEnabled;


    // Step 1: Initiate Score Query (called synchronously within loan request)
    protected String initiateScoreQuery(String customerNumber) {
        if (mockEnabled) {
            log.info("Mock enabled: Generating mock scoring token for customer {}", customerNumber);
            return "MOCK-TOKEN-" + customerNumber; // Example mock token
        }

        String url = scoringApiBaseUrl + "/scoring/initiateQueryScore/{customerNumber}";
        log.info("Initiating score query for customer: {}", customerNumber);

        try {
            String scoringToken = restClient.get()
                    .uri(url, customerNumber)
                    .header("client-token", clientToken)
                    .retrieve()
                    .body(String.class);

            if (scoringToken == null || scoringToken.isBlank()) {
                log.error("Received empty or null scoring token for customer {}", customerNumber);
                throw new LoanApplicationException("Failed to initiate scoring: No token received.");
            }
            log.info("Received scoring token: {} for customer: {}", scoringToken, customerNumber);
            return scoringToken;

        } catch (RestClientResponseException e) {
            log.error("HTTP error initiating score query for customer {}: {} - {}", customerNumber, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new LoanApplicationException("Failed to initiate scoring: " + e.getStatusCode());
        } catch (ResourceAccessException e) {
            log.error("Network error initiating score query for customer {}: {}", customerNumber, e.getMessage(), e);
            throw new LoanApplicationException("Failed to initiate scoring: Network error.");
        } catch (Exception e) {
            log.error("Unexpected error initiating score query for customer {}: {}", customerNumber, e.getMessage(), e);
            throw new LoanApplicationException("Unexpected error during scoring initiation.");
        }
    }

    // Called from LoanService after creating the initial LoanApplication entry
    @Async
    public void initiateAndQueryScoreAsync(String customerNumber) {
        LoanApplication application = loanApplicationRepository.findByCustomerNumber(customerNumber)
                .orElseThrow(() -> new LoanApplicationException("Application not found for async scoring: " + customerNumber));

        try {
            String scoringToken = initiateScoreQuery(customerNumber);
            application.setScoringToken(scoringToken);
            application.setStatus(LoanStatus.SCORING_IN_PROGRESS);
            application.setScoreQueryRetries(0);
            loanApplicationRepository.save(application);

            queryScoreWithRetry(customerNumber, scoringToken);

        } catch (LoanApplicationException e) {
            log.error("Failed to initiate scoring process for customer {}: {}", customerNumber, e.getMessage());
            application.setStatus(LoanStatus.SCORING_FAILED);
            application.setFailureMessage("Failed to initiate scoring: " + e.getMessage());
            loanApplicationRepository.save(application);
            loanApplicationRepository.clearActiveProcess(customerNumber);
        } catch (Exception e) {
            log.error("Unexpected error during async scoring initiation for customer {}: {}", customerNumber, e.getMessage(), e);
            application.setStatus(LoanStatus.SCORING_FAILED);
            application.setFailureMessage("Unexpected system error during scoring initiation.");
            loanApplicationRepository.save(application);
            loanApplicationRepository.clearActiveProcess(customerNumber);
        }
    }


    // Step 2: Query Score (with retry logic)
    @Retryable(
            retryFor = { ScoreNotReadyException.class, ResourceAccessException.class, RestClientResponseException.class },
            maxAttemptsExpression = "${retry.score-query.max-attempts}",
            backoff = @Backoff(delayExpression = "${retry.score-query.delay-ms}")
    )
    public void queryScoreWithRetry(String customerNumber, String scoringToken) {
        LoanApplication application = loanApplicationRepository.findByCustomerNumber(customerNumber)
                .orElseThrow(() -> {
                    log.error("Application disappeared during retry for customer {} token {}", customerNumber, scoringToken);
                    return new IllegalStateException("Application missing mid-retry");
                });

        if (mockEnabled) {
            log.info("Mock enabled: Generating mock score response for customer {}", customerNumber);
            ScoreQueryResponse mockResponse = createMockScoreResponse(customerNumber);
            processScoreResponse(customerNumber, mockResponse);
            return; // Skip actual API call
        }

        if (application.getStatus() != LoanStatus.SCORING_IN_PROGRESS && application.getStatus() != LoanStatus.PENDING_SCORE) {
            log.warn("Scoring query for customer {} token {} skipped, status is already {}",
                    customerNumber, scoringToken, application.getStatus());
            return;
        }

        application.setScoreQueryRetries(application.getScoreQueryRetries() + 1);
        loanApplicationRepository.save(application);

        String url = scoringApiBaseUrl + "/scoring/queryScore/{token}";
        log.info("Attempt {}/{}: Querying score for customer: {} with token: {}",
                application.getScoreQueryRetries(), maxAttempts, customerNumber, scoringToken);

        try {
            restClient.get()
                    .uri(url, scoringToken)
                    .header("client-token", clientToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    // --- Status Code Handling ---
                    .onStatus(HttpStatusCode::is2xxSuccessful, (request, response) -> {
                        // Handle 200 OK, 201 Created, etc.
                        HttpStatusCode status = response.getStatusCode();
                        if (status == HttpStatus.OK) {
                            // FIX: Manually deserialize body from InputStream
                            try (InputStream bodyStream = response.getBody()) {
                                ScoreQueryResponse scoreData = objectMapper.readValue(bodyStream, ScoreQueryResponse.class);
                                if (scoreData != null) {
                                    log.info("Successfully retrieved score for customer {}: Score={}, Limit={}, Exclusion={}",
                                            customerNumber, scoreData.getScore(), scoreData.getLimitAmount(), scoreData.getExclusion());
                                    processScoreResponse(customerNumber, scoreData);
                                } else {
                                    log.error("Received OK status but deserialized body was null for score query, customer {}", customerNumber);
                                    throw new LoanApplicationException("Received null score data despite OK status.");
                                }
                            } catch (IOException e) {
                                log.error("Failed to read or deserialize score response body for customer {}: {}", customerNumber, e.getMessage(), e);
                                throw new LoanApplicationException("Failed to process score response body.", e);
                            }
                        } else if (status == HttpStatus.ACCEPTED || status == HttpStatus.NO_CONTENT) {
                            log.warn("Score not ready yet for customer {} (token: {}), status: {}. Will retry.",
                                    customerNumber, scoringToken, status);
                            throw new ScoreNotReadyException("Score not ready, status: " + status);
                        } else {
                            log.warn("Received unexpected success status {} for score query, customer {}", status, customerNumber);
                            throw new LoanApplicationException("Unexpected success status: " + status);
                        }
                    })
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        log.error("Client error querying score for customer {}: {} - {}",
                                customerNumber, response.getStatusCode(), response.getStatusText());
                        // Throw exception to prevent retry
                        throw new RestClientResponseException( "Client error during score query", response.getStatusCode().value(), response.getStatusText(), response.getHeaders(), null, null);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        log.error("Server error querying score for customer {}: {} - {}",
                                customerNumber, response.getStatusCode(), response.getStatusText());
                        // Throw exception configured for retry
                        throw new RestClientResponseException("Server error during score query", response.getStatusCode().value(), response.getStatusText(), response.getHeaders(), null, null);
                    })
                    .body(Void.class); // Consume body (handled in onStatus)


        } catch (RestClientResponseException e) {
            // Catch errors thrown from onStatus handlers for 4xx/5xx
            if (e.getStatusCode().is4xxClientError()) {
                log.warn("Caught 4xx error from score query for customer {}, handling failure.", customerNumber);
                handleScoringClientError(customerNumber, e);
                throw new LoanApplicationException("Client error during score query: " + e.getStatusCode());
            } else {
                log.warn("Caught 5xx error from score query for customer {}, propagating for retry.", customerNumber);
                throw e; // Propagate 5xx to be caught by @Retryable
            }
        }
        // Let ScoreNotReadyException and ResourceAccessException propagate to trigger retry
        catch (Exception e) {
            log.error("Unexpected error during score query processing for customer {}: {}", customerNumber, e.getMessage(), e);
            throw new LoanApplicationException("Unexpected system error during score query processing.");
        }
    }

    // Recovery method called when @Retryable fails after all attempts
    @Recover
    public void recoverQueryScore(Exception e, String customerNumber, String scoringToken) {
        Throwable cause = (e instanceof ResourceAccessException || e instanceof ScoreNotReadyException || e instanceof RestClientResponseException) ? e : e.getCause();
        String finalErrorMessage = (cause != null) ? cause.getMessage() : e.getMessage();

        log.error("Failed to query score for customer {} (token: {}) after {} attempts. Final Error: {}",
                customerNumber, scoringToken, maxAttempts, finalErrorMessage, e);

        LoanApplication application = loanApplicationRepository.findByCustomerNumber(customerNumber)
                .orElse(null);
        if (application != null) {
            if (application.getStatus() == LoanStatus.SCORING_IN_PROGRESS || application.getStatus() == LoanStatus.PENDING_SCORE) {
                application.setStatus(LoanStatus.SCORING_FAILED);
                application.setFailureMessage("Failed to retrieve score after multiple retries: " + finalErrorMessage);
                loanApplicationRepository.save(application);
                loanApplicationRepository.clearActiveProcess(customerNumber);
            } else {
                log.warn("Recovery skipped for customer {}, already in status {}", customerNumber, application.getStatus());
            }
        } else {
            log.error("Cannot recover score query, application not found for customer {}", customerNumber);
            loanApplicationRepository.clearActiveProcess(customerNumber);
        }
    }


    // Process the successful scoring response (No changes needed here)
    private void processScoreResponse(String customerNumber, ScoreQueryResponse scoreData) {
        LoanApplication application = loanApplicationRepository.findByCustomerNumber(customerNumber)
                .orElseThrow(() -> new LoanApplicationException("Application not found for processing score response: " + customerNumber));

        if (application.getStatus() != LoanStatus.SCORING_IN_PROGRESS) {
            log.warn("Skipping score processing for customer {}, status changed to {} during query.", customerNumber, application.getStatus());
            return;
        }

        application.setScore(scoreData.getScore());
        application.setLimitAmount(scoreData.getLimitAmount());
        application.setExclusionReason(scoreData.getExclusionReason());

        if (scoreData.getExclusion() != null && !scoreData.getExclusion().equalsIgnoreCase("No Exclusion")) {
            application.setStatus(LoanStatus.REJECTED_EXCLUSION);
            application.setFailureMessage("Rejected due to exclusion: " + scoreData.getExclusionReason());
        } else if (scoreData.getLimitAmount() != null && application.getRequestedAmount() != null &&
                scoreData.getLimitAmount().compareTo(application.getRequestedAmount()) < 0) {
            application.setStatus(LoanStatus.REJECTED_LIMIT);
            application.setFailureMessage("Rejected due to insufficient limit.");
        } else {
            application.setStatus(LoanStatus.APPROVED);
        }
        loanApplicationRepository.save(application);

        if (application.getStatus() != LoanStatus.ACTIVE) {
            loanApplicationRepository.clearActiveProcess(customerNumber);
        }
        log.info("Processed score for customer {}. Final status: {}", customerNumber, application.getStatus());
    }

    // Handle 4xx client errors during score query (No changes needed here)
    private void handleScoringClientError(String customerNumber, RestClientResponseException e) {
        LoanApplication application = loanApplicationRepository.findByCustomerNumber(customerNumber)
                .orElse(null);
        if (application != null) {
            if (application.getStatus() == LoanStatus.SCORING_IN_PROGRESS || application.getStatus() == LoanStatus.PENDING_SCORE) {
                application.setStatus(LoanStatus.SCORING_FAILED);
                application.setFailureMessage("Scoring failed: Client Error " + e.getStatusCode() + ". Check token or request.");
                loanApplicationRepository.save(application);
                loanApplicationRepository.clearActiveProcess(customerNumber);
            } else {
                log.warn("Client error occurred for customer {}, but status was already {}. No state change.", customerNumber, application.getStatus());
            }
        } else {
            log.error("Received client error for score query, but application not found for customer {}", customerNumber);
            loanApplicationRepository.clearActiveProcess(customerNumber);
        }
    }

    // Custom exception to represent the "score not ready" scenario for retry
    static class ScoreNotReadyException extends RuntimeException {
        public ScoreNotReadyException(String message) {
            super(message);
        }
    }

    private ScoreQueryResponse createMockScoreResponse(String customerNumber) {
        // Example mock data - customize as needed
        int score = 650 + (int)(Math.random() * 100); // Random score between 650-750
        BigDecimal limit = BigDecimal.valueOf(5000 + (Math.random() * 10000)); // Limit between 5k-15k
        String exclusion = Math.random() < 0.2 ? "Mock Exclusion Reason" : null; // 20% chance of exclusion

        ScoreQueryResponse scoreResponse = new ScoreQueryResponse();

        scoreResponse.setCustomerNumber(customerNumber);
        scoreResponse.setScore(score);
        scoreResponse.setLimitAmount(limit);
        scoreResponse.setExclusion(exclusion != null ? "Excluded" : "No Exclusion");
        scoreResponse.setExclusionReason(exclusion);

        return scoreResponse;
    }

}
