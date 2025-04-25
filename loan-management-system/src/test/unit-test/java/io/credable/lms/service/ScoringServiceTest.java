package io.credable.lms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.credable.lms.dto.ScoreQueryResponse;
import io.credable.lms.exception.LoanApplicationException;
import io.credable.lms.model.LoanApplication;
import io.credable.lms.model.LoanStatus;
import io.credable.lms.repo.LoanApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @author - <a href="https://github.com/muchembi"> muchembi </a>
 * {@code @date} Thu, 24-Apr-2025, 9:10 pm
 */
@ExtendWith(MockitoExtension.class)
class ScoringServiceTest {

    // Mocks for dependencies
    @Mock
    private RestClient restClient;
    @Mock
    private LoanApplicationRepository loanRepo;
    @Mock
    private ObjectMapper objectMapper;

    // Mocks for RestClient fluent API chain
    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    // Inject mocks into the service under test
    @InjectMocks
    private ScoringService scoringService;

    // Captor to verify saved LoanApplication state
    @Captor
    ArgumentCaptor<LoanApplication> loanAppCaptor;

    // Test constants and setup data
    private final String customerNumber = "318411216";
    private final String scoringToken = "test-score-token";
    private final String clientToken = "test-client-token";
    private final String scoringApiBaseUrl = "http://mock-scoring.com/api/v1";
    private LoanApplication pendingApplication;
    private final BigDecimal defaultRequestedAmount = new BigDecimal("10000");

    @BeforeEach
    void setUp() throws JsonProcessingException {
        // Set non-mocked fields using ReflectionTestUtils
        ReflectionTestUtils.setField(scoringService, "scoringApiBaseUrl", scoringApiBaseUrl);
        ReflectionTestUtils.setField(scoringService, "clientToken", clientToken);
        ReflectionTestUtils.setField(scoringService, "maxAttempts", 3);
        ReflectionTestUtils.setField(scoringService, "mockEnabled", false); // Default to real calls

        // Prepare a default LoanApplication state for tests
        pendingApplication = new LoanApplication(customerNumber, defaultRequestedAmount);
        pendingApplication.setStatus(LoanStatus.PENDING_SCORE);
        pendingApplication.setScoringToken(scoringToken);

        // --- Common mocking setup for RestClient fluent API ---
        // Mock the chain: restClient.get() -> .uri() -> .header()/.accept() -> .retrieve() -> .onStatus()/.body()
        lenient().when(restClient.get()).thenReturn(requestHeadersUriSpec); // Use lenient for flexibility across tests
        lenient().when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.accept(any(MediaType.class))).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

    }

    // --- initiateScoreQuery Tests ---
    @Nested
    @DisplayName("initiateScoreQuery() -> Scenarios")
    class InitiateScoreQueryTests {

        @Test
        @DisplayName("Success: Returns Token")
        void successReturnsToken() {
            // Arrange
            String expectedToken = "real-token-123";
            // Mock the final step: retrieve().body(String.class)
            when(responseSpec.body(String.class)).thenReturn(expectedToken);

            // Act
            // Use ReflectionTestUtils ONLY because the method is private
            String actualToken = ReflectionTestUtils.invokeMethod(scoringService, "initiateScoreQuery", customerNumber);

            // Assert
            assertEquals(expectedToken, actualToken);
            // Verify the chain of calls was made correctly
            verify(restClient.get()).uri(scoringApiBaseUrl + "/scoring/initiateQueryScore/{customerNumber}", customerNumber);
            verify(requestHeadersSpec).header("client-token", clientToken);
            verify(requestHeadersSpec).retrieve();
            verify(responseSpec).body(String.class);
        }

        @Test
        @DisplayName("HTTP Error: Throws LoanApplicationException")
        void httpErrorThrowsLoanApplicationException() {
            // Arrange
            RestClientResponseException httpError = new RestClientResponseException(
                    "Error", HttpStatus.BAD_REQUEST.value(), "Bad Request", null, null, null);
            // Mock retrieve() to throw the error
            when(requestHeadersSpec.retrieve()).thenThrow(httpError);

            // Act & Assert
            LoanApplicationException ex = assertThrows(LoanApplicationException.class, () -> {
                ReflectionTestUtils.invokeMethod(scoringService, "initiateScoreQuery", customerNumber);
            });
            assertTrue(ex.getMessage().contains("Failed to initiate scoring: 400 BAD_REQUEST"));
        }

        @Test
        @DisplayName("Network Error: Throws LoanApplicationException")
        void networkErrorThrowsLoanApplicationException() {
            // Arrange
            ResourceAccessException networkError = new ResourceAccessException("Connection refused");
            // Mock retrieve() to throw the error
            when(requestHeadersSpec.retrieve()).thenThrow(networkError);

            // Act & Assert
            LoanApplicationException ex = assertThrows(LoanApplicationException.class, () -> {
                ReflectionTestUtils.invokeMethod(scoringService, "initiateScoreQuery", customerNumber);
            });
            assertTrue(ex.getMessage().contains("Failed to initiate scoring: Network error."));
        }

    }

    // --- initiateAndQueryScoreAsync Tests ---
    @Nested
    @DisplayName("initiateAndQueryScoreAsync() -> Scenarios")
    class InitiateAndQueryScoreAsyncTests {

        @Test
        @DisplayName("Success: Saves Token and Triggers Query")
        void successSavesTokenAndTriggersQuery() {
            // Arrange
            String realToken = "initiate-token-success";
            when(loanRepo.findByCustomerNumber(customerNumber)).thenReturn(Optional.of(pendingApplication));

            // Use a Spy to test interaction with the real object while mocking some methods
            // InjectMocks creates the instance, Spy wraps it
            ScoringService spyService = spy(scoringService);

            doReturn(realToken).when(spyService).initiateScoreQuery(customerNumber); // Use the helper
            doNothing().when(spyService).queryScoreWithRetry(anyString(), anyString()); // Prevent async execution

            // Act
            spyService.initiateAndQueryScoreAsync(customerNumber);

            // Assert
            verify(loanRepo).save(loanAppCaptor.capture());
            LoanApplication savedApp = loanAppCaptor.getValue();
            assertEquals(realToken, savedApp.getScoringToken());
            assertEquals(LoanStatus.SCORING_IN_PROGRESS, savedApp.getStatus());
            assertEquals(0, savedApp.getScoreQueryRetries());

            // Verify the async method was called on the spy
            verify(spyService).queryScoreWithRetry(customerNumber, realToken);
        }

        @Test
        @DisplayName("Initiate Failure: Sets Status to Failed and Clears Lock")
        void initiateFailureSetsStatusToFailedAndClearsLock() {
            // Arrange
            LoanApplicationException initiateException = new LoanApplicationException("Failed to initiate");
            when(loanRepo.findByCustomerNumber(customerNumber)).thenReturn(Optional.of(pendingApplication));

            ScoringService spyService = spy(scoringService);
            // Mock the helper method to throw the exception
            doThrow(initiateException).when(spyService).initiateScoreQuery(customerNumber);

            // Act
            spyService.initiateAndQueryScoreAsync(customerNumber);

            // Assert
            verify(loanRepo).save(loanAppCaptor.capture());
            LoanApplication savedApp = loanAppCaptor.getValue();
            assertEquals(LoanStatus.SCORING_FAILED, savedApp.getStatus());
            assertTrue(savedApp.getFailureMessage().contains("Failed to initiate scoring"));

            verify(loanRepo).clearActiveProcess(customerNumber);
            verify(spyService, never()).queryScoreWithRetry(anyString(), anyString());
        }
    }

    // --- recoverQueryScore Tests ---
    @Nested
    @DisplayName("recoverQueryScore() -> Scenarios")
    class RecoverQueryScoreTests {

        @Test
        @DisplayName("Recovers: Sets Failed Status and Clears Lock")
        void recoversSetsFailedStatusAndClearsLock() {
            // Arrange
            ResourceAccessException finalException = new ResourceAccessException("Timeout");
            pendingApplication.setStatus(LoanStatus.SCORING_IN_PROGRESS);
            pendingApplication.setScoreQueryRetries(3);
            when(loanRepo.findByCustomerNumber(customerNumber)).thenReturn(Optional.of(pendingApplication));

            // Act
            scoringService.recoverQueryScore(finalException, customerNumber, scoringToken);

            // Assert
            verify(loanRepo).save(loanAppCaptor.capture());
            LoanApplication recoveredApp = loanAppCaptor.getValue();
            assertEquals(LoanStatus.SCORING_FAILED, recoveredApp.getStatus());
            assertTrue(recoveredApp.getFailureMessage().contains("Timeout"));
            verify(loanRepo).clearActiveProcess(customerNumber);
        }

        @Test
        @DisplayName("Recovers: Skips if Status Already Terminal")
        void recoversSkipsIfStatusAlreadyTerminal() {
            // Arrange
            ResourceAccessException finalException = new ResourceAccessException("Timeout");
            pendingApplication.setStatus(LoanStatus.APPROVED);
            pendingApplication.setScoreQueryRetries(2);
            when(loanRepo.findByCustomerNumber(customerNumber)).thenReturn(Optional.of(pendingApplication));

            // Act
            scoringService.recoverQueryScore(finalException, customerNumber, scoringToken);

            // Assert
            verify(loanRepo, never()).save(any());
            verify(loanRepo, never()).clearActiveProcess(anyString());
        }
    }

}

