package io.credable.lms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.credable.lms.dto.CbsKycResponse;
import io.credable.lms.dto.CbsTransactionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * @author - <a href="https://github.com/muchembi"> muchembi </a>
 * {@code @date} Thu, 24-Apr-2025, 4:30 pm
 */
@Service
@Slf4j
public class CbsService {
    // In a real app, use generated SOAP client beans here
    // private final KycSoapClient kycClient;
    // private final TransactionSoapClient transactionClient;

    @Value("${cbs.api.username}")
    private String cbsUsername;
    @Value("${cbs.api.password}")
    private String cbsPassword;

    private final ObjectMapper objectMapper = new ObjectMapper();


    // Mock implementation for KYC
    public CbsKycResponse getCustomerKyc(String customerNumber) {
        log.info("MOCK: Calling CBS KYC for customer: {}", customerNumber);
        // Simulate checking if customer exists based on test data
        List<String> testCustomers = List.of("234774784", "318411216", "340397370", "366585630", "397178638");
        if (!testCustomers.contains(customerNumber)) {
            log.warn("MOCK: Customer {} not found in test data.", customerNumber);
            // Returning null or throwing exception depends on desired error handling
            return null; // Simulate customer not found
        }

        // Simulate successful response
        CbsKycResponse response = new CbsKycResponse();
        response.setCustomerId(customerNumber);
        response.setFullName("Mock Customer Name");
        response.setStatus("ACTIVE");
        response.setAddress("Mock Address");
        log.info("MOCK: Returning mock KYC data for customer: {}", customerNumber);
        return response;
    }

    // Mock implementation for Transactions
    public List<CbsTransactionResponse> getCustomerTransactions(String customerNumber) {
        log.info("MOCK: Calling CBS Transactions for customer: {}", customerNumber);
        // Simulate checking if customer exists based on test data (optional redundancy)
        List<String> testCustomers = List.of("234774784", "318411216", "340397370", "366585630", "397178638");
        if (!testCustomers.contains(customerNumber)) {
            log.warn("MOCK: Customer {} not found in test data for transactions.", customerNumber);
            return Collections.emptyList();
        }

        // Load the sample JSON from the classpath (acts as our mock data source)
        // Assumes a file named 'mock_transactions.json' exists in src/main/resources
        // Containing the JSON array provided in the prompt.
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("mock_transactions.json")) {
            if (is == null) {
                log.error("MOCK: Could not find mock_transactions.json");
                return Collections.emptyList();
            }
            List<CbsTransactionResponse> transactions = objectMapper.readValue(is, new TypeReference<List<CbsTransactionResponse>>() {});
            log.info("MOCK: Returning {} mock transaction records for customer: {}", transactions.size(), customerNumber);
            // In a real scenario, you might filter or return specific data per customer
            // For this mock, we return the full sample list for any valid test customer.
            return transactions;
        } catch (IOException e) {
            log.error("MOCK: Failed to read or parse mock_transactions.json", e);
            return Collections.emptyList();
        }
    }
}
