package io.credable.lms.config;

import io.credable.lms.dto.ClientRegistrationRequest;
import io.credable.lms.dto.ClientRegistrationResponse;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

/**
 * @author - <a href="https://github.com/muchembi"> muchembi </a>
 * {@code @date} Thu, 24-Apr-2025, 5:01 pm
 */
@Configuration
@Slf4j
public class AppConfig {

    @Value("${lms.security.username}")
    private String lmsUsername;

    @Value("${lms.security.password}")
    private String lmsPassword;

    @Value("${scoring.api.base-url}")
    private String scoringApiBaseUrl;

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }


    // --- BEAN FOR MANUAL CLIENT REGISTRATION ---
    @Bean
    @Profile("!test") // Don't run in test profile
    public ApplicationRunner registerClientRunner(RestClient restClient) {
        return args -> {
            String myPublicUrl = System.getenv("LMS_PUBLIC_URL");
            String serverPort = System.getenv("SERVER_PORT");
            if (serverPort == null || serverPort.isBlank()) {
                serverPort = "8080";
            }

            if (myPublicUrl == null || myPublicUrl.isBlank()) {
                myPublicUrl = "http://localhost:" + serverPort;
                log.warn("LMS_PUBLIC_URL environment variable not set. Using default: {}. " +
                        "Manual client registration is recommended for deployed environments.", myPublicUrl);
            }
            String transactionDataEndpoint = myPublicUrl + "/api/v1/lms/transactions/{customerNumber}";

            String registrationUrl = scoringApiBaseUrl + "/client/createClient";
            ClientRegistrationRequest request = new ClientRegistrationRequest();
            request.setUrl(transactionDataEndpoint);
            request.setName("MyLmsService-" + System.currentTimeMillis());
            request.setUsername(lmsUsername);
            request.setPassword(lmsPassword);

            log.info("Attempting to register LMS endpoint '{}' with Scoring Engine at {}", transactionDataEndpoint, registrationUrl);

            try {
                ClientRegistrationResponse response = restClient.post()
                        .uri(registrationUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(ClientRegistrationResponse.class);

                if (response != null && response.getToken() != null) {
                    String receivedToken = response.getToken();
                    log.info("Successfully registered with Scoring Engine. Received client-token: {}", receivedToken);
                    log.warn("IMPORTANT: Store this token securely (e.g., in Vault or secured config) and configure it as 'scoring.api.client-token' for subsequent calls.");
                } else {
                    log.error("Failed to register client. Response body or token was null.");
                }
            } catch (Exception e) {
                log.error("Error during client registration: {}. Manual registration required.", e.getMessage(), e);
                // Don't stop startup, but log error prominently
            }
        };
    }


}
