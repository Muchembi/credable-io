package io.credable.lms.service;

import io.credable.lms.dto.CbsKycResponse;
import io.credable.lms.dto.LoanRequest;
import io.credable.lms.exception.LoanApplicationException;
import io.credable.lms.exception.ResourceNotFoundException;
import io.credable.lms.model.LoanApplication;
import io.credable.lms.model.LoanStatus;
import io.credable.lms.repo.LoanApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author - <a href="https://github.com/muchembi"> muchembi </a>
 * {@code @date} Thu, 24-Apr-2025, 9:07 pm
 */
@ExtendWith(MockitoExtension.class)
public class LoanServiceTest {

    @Mock
    private LoanApplicationRepository loanRepo;

    @Mock
    private CbsService cbsService;

    @Mock
    private ScoringService scoringService;

    @InjectMocks
    private LoanService loanService;

    private String testCustomerNumber;
    private LoanRequest validLoanRequest;
    private LoanApplication eligibleApplication;

    @BeforeEach
    void setUp() {
        testCustomerNumber = "234774784";
        validLoanRequest = new LoanRequest();
        validLoanRequest.setCustomerNumber(testCustomerNumber);
        validLoanRequest.setAmount(new BigDecimal("5000"));

        eligibleApplication = new LoanApplication(testCustomerNumber);
        eligibleApplication.setStatus(LoanStatus.ELIGIBLE);
    }

    @Test
    @DisplayName("subscribeCustomer() -> Scenario: New active customer is successfully subscribed")
    void shouldSubscribeCustomerSuccessfully() {
        CbsKycResponse kycResponse = new CbsKycResponse();
        kycResponse.setCustomerId(testCustomerNumber);
        kycResponse.setStatus("ACTIVE");

        when(cbsService.getCustomerKyc(testCustomerNumber)).thenReturn(kycResponse);
        when(loanRepo.findByCustomerNumber(testCustomerNumber)).thenReturn(Optional.empty());

        LoanApplication result = loanService.subscribeCustomer(testCustomerNumber);

        assertNotNull(result);
        assertEquals(LoanStatus.ELIGIBLE, result.getStatus());
        assertEquals(testCustomerNumber, result.getCustomerNumber());

        ArgumentCaptor<LoanApplication> appCaptor = ArgumentCaptor.forClass(LoanApplication.class);
        verify(loanRepo).save(appCaptor.capture());
        assertEquals(LoanStatus.ELIGIBLE, appCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("subscribeCustomer() -> Scenario: Customer not found in KYC should throw exception")
    void shouldThrowWhenKycIsNull() {
        when(cbsService.getCustomerKyc(testCustomerNumber)).thenReturn(null);

        LoanApplicationException exception = assertThrows(LoanApplicationException.class, () -> {
            loanService.subscribeCustomer(testCustomerNumber);
        });

        assertTrue(exception.getMessage().contains("Customer not found"));
        verify(loanRepo, never()).save(any());
    }

    @Test
    @DisplayName("subscribeCustomer() -> Scenario: Inactive customer in KYC should throw exception")
    void shouldThrowWhenKycIsInactive() {
        CbsKycResponse kycResponse = new CbsKycResponse();
        kycResponse.setCustomerId(testCustomerNumber);
        kycResponse.setStatus("INACTIVE");

        when(cbsService.getCustomerKyc(testCustomerNumber)).thenReturn(kycResponse);

        LoanApplicationException exception = assertThrows(LoanApplicationException.class, () -> {
            loanService.subscribeCustomer(testCustomerNumber);
        });

        assertTrue(exception.getMessage().contains("Customer status not ACTIVE"));
        verify(loanRepo, never()).save(any());
    }

    @Test
    @DisplayName("requestLoan() -> Scenario: Eligible customer initiates loan request and scoring")
    void shouldRequestLoanSuccessfully() {
        when(loanRepo.lockCustomerForProcessing(testCustomerNumber)).thenReturn(true);
        when(loanRepo.findByCustomerNumber(testCustomerNumber)).thenReturn(Optional.of(eligibleApplication));

        LoanApplication result = loanService.requestLoan(validLoanRequest);

        assertNotNull(result);
        assertEquals(LoanStatus.PENDING_SCORE, result.getStatus());
        assertEquals(validLoanRequest.getAmount(), result.getRequestedAmount());

        ArgumentCaptor<LoanApplication> appCaptor = ArgumentCaptor.forClass(LoanApplication.class);
        verify(loanRepo).save(appCaptor.capture());
        assertEquals(LoanStatus.PENDING_SCORE, appCaptor.getValue().getStatus());
        assertNull(appCaptor.getValue().getScoringToken());

        verify(scoringService).initiateAndQueryScoreAsync(testCustomerNumber);
        verify(loanRepo, never()).clearActiveProcess(anyString());
    }

    @Test
    @DisplayName("requestLoan() -> Scenario: Loan request while another is in progress throws exception")
    void shouldThrowOnConcurrentLoanRequest() {
        when(loanRepo.lockCustomerForProcessing(testCustomerNumber)).thenReturn(false);

        LoanApplication pendingApp = new LoanApplication(testCustomerNumber);
        pendingApp.setStatus(LoanStatus.PENDING_SCORE);

        when(loanRepo.findByCustomerNumber(testCustomerNumber)).thenReturn(Optional.of(pendingApp));

        LoanApplicationException exception = assertThrows(LoanApplicationException.class, () -> {
            loanService.requestLoan(validLoanRequest);
        });

        assertTrue(exception.getMessage().contains("An active loan or pending application exists"));
        assertEquals(LoanStatus.FAILED_CONCURRENT, exception.getStatusHint());

        verify(loanRepo, never()).save(any());
        verify(scoringService, never()).initiateAndQueryScoreAsync(anyString());
        verify(loanRepo, never()).clearActiveProcess(anyString());
    }

    @Test
    @DisplayName("requestLoan() -> Scenario: Loan amount is invalid")
    void shouldThrowOnInvalidAmount() {
        LoanRequest invalidRequest = new LoanRequest();
        invalidRequest.setCustomerNumber(testCustomerNumber);
        invalidRequest.setAmount(BigDecimal.ZERO);

        when(loanRepo.lockCustomerForProcessing(testCustomerNumber)).thenReturn(true);

        LoanApplicationException exception = assertThrows(LoanApplicationException.class, () -> {
            loanService.requestLoan(invalidRequest);
        });

        assertTrue(exception.getMessage().contains("Invalid loan amount"));
        verify(loanRepo).clearActiveProcess(testCustomerNumber);
        verify(loanRepo, never()).save(any());
        verify(scoringService, never()).initiateAndQueryScoreAsync(anyString());
    }

    @Test
    @DisplayName("requestLoan() -> Scenario: Customer state is not eligible")
    void shouldThrowWhenCustomerIsNotEligible() {
        when(loanRepo.lockCustomerForProcessing(testCustomerNumber)).thenReturn(true);

        LoanApplication nonEligibleApp = new LoanApplication(testCustomerNumber);
        nonEligibleApp.setStatus(LoanStatus.PENDING_SUBSCRIPTION);

        when(loanRepo.findByCustomerNumber(testCustomerNumber)).thenReturn(Optional.of(nonEligibleApp));

        LoanApplicationException exception = assertThrows(LoanApplicationException.class, () -> {
            loanService.requestLoan(validLoanRequest);
        });

        assertTrue(exception.getMessage().contains("State conflict"));
        assertEquals(LoanStatus.FAILED_CONCURRENT, exception.getStatusHint());

        verify(loanRepo).clearActiveProcess(testCustomerNumber);
        verify(loanRepo, never()).save(any());
        verify(scoringService, never()).initiateAndQueryScoreAsync(anyString());
    }

    @Test
    @DisplayName("getLoanStatus() -> Scenario: Loan status successfully retrieved for existing customer")
    void shouldReturnLoanStatusIfFound() {
        LoanApplication existingApp = new LoanApplication(testCustomerNumber);
        existingApp.setStatus(LoanStatus.APPROVED);

        when(loanRepo.findByCustomerNumber(testCustomerNumber)).thenReturn(Optional.of(existingApp));

        LoanApplication result = loanService.getLoanStatus(testCustomerNumber);

        assertNotNull(result);
        assertEquals(LoanStatus.APPROVED, result.getStatus());
        assertEquals(testCustomerNumber, result.getCustomerNumber());
    }

    @Test
    @DisplayName("getLoanStatus() -> Scenario: Loan status retrieval fails for non-existing customer")
    void shouldThrowIfLoanStatusNotFound() {
        when(loanRepo.findByCustomerNumber(testCustomerNumber)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            loanService.getLoanStatus(testCustomerNumber);
        });
    }
}
