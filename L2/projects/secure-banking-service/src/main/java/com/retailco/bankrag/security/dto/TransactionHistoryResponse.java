package com.retailco.bankrag.security.dto;

import com.retailco.bankrag.security.BankingToolService;

import java.util.List;

public record TransactionHistoryResponse(String customerId, String accountNumber,
                                           List<BankingToolService.MaskedTransaction> transactions) {
}
