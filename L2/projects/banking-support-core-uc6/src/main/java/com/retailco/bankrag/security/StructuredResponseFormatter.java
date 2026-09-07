package com.retailco.bankrag.security;

import java.util.List;

/**
 * Deliverable: "Return structured JSON responses" (L2 HLD UseCase3 System
 * Responsibilities) / "Structured response examples" (Deliverables list).
 *
 * Hand-rolled JSON serialization, same reasoning as every other module in
 * this submission that avoids Jackson/Gson (Maven Central blocked --
 * secure-banking-core is pure-JDK specifically so it can be compiled and
 * run for real here). secure-banking-service's Spring controllers use
 * Spring's built-in Jackson integration instead (see its dto/ classes) --
 * this class exists so the SAME response shape can be demonstrated and
 * tested from the pure-JDK demo without needing Spring at all.
 */
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
