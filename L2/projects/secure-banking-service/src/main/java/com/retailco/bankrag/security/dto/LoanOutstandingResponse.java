package com.retailco.bankrag.security.dto;

import com.retailco.bankrag.security.BankingToolService;

import java.util.List;

public record LoanOutstandingResponse(String customerId, List<BankingToolService.MaskedLoan> loans) {
}
