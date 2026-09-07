package com.retailco.bankrag.security;

import java.util.List;

// CONCEPT: Response formatting / serialization -- turning domain objects
// into JSON strings, hand-written (no Jackson) so it works in the
// pure-JDK demo (Main.java) as well as inside Spring.
// PURPOSE: Produces the exact JSON shape a REST client would receive for
// account balances, transaction history, and loan data -- always using
// the already-MASKED record types (MaskedAccount/MaskedTransaction/
// MaskedLoan from BankingToolService), never the raw ones from
// BankingDataStore.
// WHY this exists separately from Spring's DTOs: Spring controllers
// normally return a DTO object and let Spring's built-in Jackson
// integration serialize it to JSON automatically. This class exists so
// the pure-JDK demo (which has no Spring, no Jackson) can produce and
// verify the identical response shape without needing a web framework at
// all -- useful for testing the exact wire format independent of Spring.
public final class StructuredResponseFormatter {

    private StructuredResponseFormatter() {
    }

    public static String accountBalanceResponse(String customerId, List<BankingToolService.MaskedAccount> accounts) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"customerId\":\"").append(customerId).append("\",\"accounts\":[");
        for (int i = 0; i < accounts.size(); i++) {
            if (i > 0) sb.append(",");
            BankingToolService.MaskedAccount a = accounts.get(i);
            sb.append("{\"accountNumber\":\"").append(a.accountNumberMasked()).append("\",")
                    .append("\"accountType\":\"").append(a.accountType()).append("\",")
                    .append("\"balance\":").append(a.balance()).append(",")
                    .append("\"currency\":\"").append(a.currency()).append("\"}");
        }
        sb.append("]}");
        return sb.toString();
    }

    public static String transactionHistoryResponse(String customerId, String accountNumberMasked,
                                                      List<BankingToolService.MaskedTransaction> transactions) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"customerId\":\"").append(customerId).append("\",")
                .append("\"accountNumber\":\"").append(accountNumberMasked).append("\",\"transactions\":[");
        for (int i = 0; i < transactions.size(); i++) {
            if (i > 0) sb.append(",");
            BankingToolService.MaskedTransaction t = transactions.get(i);
            sb.append("{\"transactionId\":\"").append(t.transactionId()).append("\",")
                    .append("\"date\":\"").append(t.date()).append("\",")
                    .append("\"description\":\"").append(escape(t.description())).append("\",")
                    .append("\"amount\":").append(t.amount()).append(",")
                    .append("\"type\":\"").append(t.type()).append("\"}");
        }
        sb.append("]}");
        return sb.toString();
    }

    public static String loanOutstandingResponse(String customerId, List<BankingToolService.MaskedLoan> loans) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"customerId\":\"").append(customerId).append("\",\"loans\":[");
        for (int i = 0; i < loans.size(); i++) {
            if (i > 0) sb.append(",");
            BankingToolService.MaskedLoan l = loans.get(i);
            sb.append("{\"loanId\":\"").append(l.loanId()).append("\",")
                    .append("\"loanType\":\"").append(l.loanType()).append("\",")
                    .append("\"principal\":").append(l.principal()).append(",")
                    .append("\"outstanding\":").append(l.outstanding()).append(",")
                    .append("\"interestRatePercent\":").append(l.interestRate()).append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    public static String errorResponse(int status, String error, String message) {
        return "{\"status\":" + status + ",\"error\":\"" + escape(error) + "\",\"message\":\"" + escape(message) + "\"}";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
