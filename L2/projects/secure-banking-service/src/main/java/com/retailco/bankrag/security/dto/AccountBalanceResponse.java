package com.retailco.bankrag.security.dto;

import com.retailco.bankrag.security.BankingToolService;

import java.util.List;

public record AccountBalanceResponse(String customerId, List<BankingToolService.MaskedAccount> accounts) {
}
