package io.credable.lms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * @author - <a href="https://github.com/muchembi"> muchembi </a>
 * {@code @date} Thu, 24-Apr-2025, 4:32 pm
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CbsTransactionResponse {
    private String accountNumber;
    private Double alternativechanneltrnscrAmount;
    private Integer alternativechanneltrnscrNumber; // Assuming integer based on 0 value
    private Double alternativechanneltrnsdebitAmount;
    private Integer alternativechanneltrnsdebitNumber; // Assuming integer based on example
    private Integer atmTransactionsNumber; // Assuming integer
    private Double atmtransactionsAmount;
    private Integer bouncedChequesDebitNumber; // Assuming integer
    private Integer bouncedchequescreditNumber; // Assuming integer
    private Double bouncedchequetransactionscrAmount;
    private Double bouncedchequetransactionsdrAmount;
    private Double chequeDebitTransactionsAmount;
    private Integer chequeDebitTransactionsNumber; // Assuming integer
    private Long createdAt; // Assuming epoch milliseconds based on example
    private Long createdDate; // Assuming epoch milliseconds
    private Double credittransactionsAmount;
    private Double debitcardpostransactionsAmount;
    private Integer debitcardpostransactionsNumber; // Assuming integer
    private Double fincominglocaltransactioncrAmount; // Field name typo likely in prompt? Assuming 'fincoming...'
    private Long id; // Assuming numeric ID
    private Double incominginternationaltrncrAmount;
    private Integer incominginternationaltrncrNumber; // Assuming integer
    private Integer incominglocaltransactioncrNumber; // Assuming integer
    private Double intrestAmount; // Assuming 'interest', numeric
    private Long lastTransactionDate; // Assuming epoch milliseconds
    private String lastTransactionType; // Example shows null, assuming String
    private Integer lastTransactionValue; // Assuming integer based on example
    private Double maxAtmTransactions;
    private Double maxMonthlyBebitTransactions; // Field name typo likely in prompt? Assuming 'maxMonthlyDebit...'
    private Double maxalternativechanneltrnscr;
    private Double maxalternativechanneltrnsdebit;
    private Double maxbouncedchequetransactionscr;
    private Double maxchequedebittransactions;
    private Double maxdebitcardpostransactions; // Example shows large number, keeping Double
    private Double maxincominginternationaltrncr;
    private Double maxincominglocaltransactioncr;
    private Double maxmobilemoneycredittrn;
    private Double maxmobilemoneydebittransaction;
    private Double maxmonthlycredittransactions;
    private Double maxoutgoinginttrndebit;
    private Double maxoutgoinglocaltrndebit;
    private Double maxoverthecounterwithdrawals;
    private Double minAtmTransactions;
    private Double minMonthlyDebitTransactions;
    private Double minalternativechanneltrnscr;
    private Double minalternativechanneltrnsdebit;
    private Double minbouncedchequetransactionscr;
    private Double minchequedebittransactions;
    private Double mindebitcardpostransactions; // Example shows large number, keeping Double
    private Double minincominginternationaltrncr;
    private Double minincominglocaltransactioncr;
    private Double minmobilemoneycredittrn;
    private Double minmobilemoneydebittransaction;
    private Double minmonthlycredittransactions;
    private Double minoutgoinginttrndebit;
    private Double minoutgoinglocaltrndebit;
    private Double minoverthecounterwithdrawals;
    private Double mobilemoneycredittransactionAmount;
    private Integer mobilemoneycredittransactionNumber; // Assuming integer
    private Double mobilemoneydebittransactionAmount;
    private Integer mobilemoneydebittransactionNumber; // Assuming integer
    private Double monthlyBalance;
    private Double monthlydebittransactionsAmount;
    private Double outgoinginttransactiondebitAmount;
    private Integer outgoinginttrndebitNumber; // Assuming integer
    private Double outgoinglocaltransactiondebitAmount;
    private Integer outgoinglocaltransactiondebitNumber; // Assuming integer
    private Double overdraftLimit;
    private Double overthecounterwithdrawalsAmount;
    private Integer overthecounterwithdrawalsNumber; // Assuming integer
    private Double transactionValue;
    private Long updatedAt; // Assuming epoch milliseconds
}
