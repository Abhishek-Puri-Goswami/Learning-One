package com.retailco.bankrag.security.controller;

import com.retailco.bankrag.security.BankingToolService;
import com.retailco.bankrag.security.dto.AccountBalanceResponse;
import com.retailco.bankrag.security.dto.LoanOutstandingResponse;
import com.retailco.bankrag.security.dto.TransactionHistoryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Deliverable: "Secure API endpoint." L2 HLD UseCase3's three requested
 * data types (account balance, transaction history, loan outstanding) as
 * three GET endpoints, each delegating auth + authorization + data
 * retrieval + masking entirely to BankingToolService (already actually run
 * in secure-banking-core -- this controller adds no business logic of its
 * own, only HTTP binding).
 *
 * Note: JwtAuthenticationFilter has already verified the token's signature
 * and expiry by the time a request reaches here (Spring Security rejects
 * an unauthenticated request before this controller is invoked). This
 * controller still passes the raw Authorization header through to
 * BankingToolService, which re-verifies it AND performs the
 * customer-id-match authorization check -- deliberately not trusting the
 * filter's authentication alone, since "is this token allowed to see THIS
 * customerId" is a per-request authorization decision the filter doesn't
 * have enough context to make generically.
 */
@RestController
@RequestMapping("/api/v1/banking")
public class BankingDataController {

    private final BankingToolService bankingToolService;

    public BankingDataController(BankingToolService bankingToolService) {
        this.bankingToolService = bankingToolService;
    }

    @GetMapping("/{customerId}/balance")
    public ResponseEntity<AccountBalanceResponse> getBalance(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String customerId) {
        var accounts = bankingToolService.getAccountBalance(authorization, customerId);
        return ResponseEntity.ok(new AccountBalanceResponse(customerId, accounts));
    }

    @GetMapping("/{customerId}/transactions")
    public ResponseEntity<TransactionHistoryResponse> getTransactions(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String customerId,
            @RequestParam String accountNumber,
            @RequestParam(defaultValue = "10") int limit) {
        var transactions = bankingToolService.getTransactionHistory(authorization, customerId, accountNumber, limit);
        return ResponseEntity.ok(new TransactionHistoryResponse(customerId, accountNumber, transactions));
    }

    @GetMapping("/{customerId}/loans")
    public ResponseEntity<LoanOutstandingResponse> getLoans(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String customerId) {
        var loans = bankingToolService.getLoanOutstanding(authorization, customerId);
        return ResponseEntity.ok(new LoanOutstandingResponse(customerId, loans));
    }
}
