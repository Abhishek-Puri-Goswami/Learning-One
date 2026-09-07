# Structured Response Examples

Deliverable: "Structured response examples," per L2 HLD UseCase3. Every example below is **real output**, copied verbatim from `reports/secure-banking-demo-run-log.txt` (`secure-banking-core`'s actual run) — not hand-written mockups.

## 1. Account balance (own-customer access, success)

Request: `GET /api/v1/banking/CUST1001/balance` with `Authorization: Bearer <CUST1001's token>`

```json
{"customerId":"CUST1001","accounts":[{"accountNumber":"XXXXXXXX0456","accountType":"SAVINGS","balance":184250.75,"currency":"INR"},{"accountNumber":"XXXXXXXX0789","accountType":"CURRENT","balance":52310.00,"currency":"INR"}]}
```

Note the account numbers: `XXXXXXXX0456` and `XXXXXXXX0789` — the real underlying values (`100200300456`, `100200300789`) are never present in the response, per `PiiMasking.maskAccountNumber`.

## 2. Transaction history (success)

Request: `GET /api/v1/banking/CUST1001/transactions?accountNumber=100200300456&limit=3`

```json
{"customerId":"CUST1001","accountNumber":"XXXXXXXX0456","transactions":[{"transactionId":"TXN9001","date":"2026-08-25","description":"Salary Credit","amount":95000.00,"type":"CREDIT"},{"transactionId":"TXN9002","date":"2026-08-24","description":"Electricity Bill Payment","amount":-3200.50,"type":"DEBIT"},{"transactionId":"TXN9003","date":"2026-08-20","description":"ATM Withdrawal","amount":-10000.00,"type":"DEBIT"}]}
```

## 3. Loan outstanding (success)

Request: `GET /api/v1/banking/CUST1001/loans`

```json
{"customerId":"CUST1001","loans":[{"loanId":"LOAN5001","loanType":"HOME_LOAN","principal":4500000.00,"outstanding":3120450.00,"interestRatePercent":8.75}]}
```

## 4. Cross-customer access attempt (authorization failure — real rejection)

Request: `GET /api/v1/banking/CUST1002/balance` with `Authorization: Bearer <CUST1001's token>`

Actual thrown message (secure-banking-core demo, Scenario 3):
```
Correctly rejected: Token subject 'CUST1001' is not authorized to access customer 'CUST1002'
```

`secure-banking-service`'s `GlobalExceptionHandler` maps this to:
```json
{"timestamp":"2026-08-30T...","status":401,"error":"Unauthorized","message":"Token subject 'CUST1001' is not authorized to access customer 'CUST1002'"}
```
(See `security/security-validation-checklist.md` item 14 for the disclosed residual risk in this message text.)

## 5. Forged/tampered token (real cryptographic rejection)

Actual thrown message (demo Scenario 4, attacker signed a token with a different secret and self-granted `ADMIN`):
```
Correctly rejected forged token: Authentication failed: Signature verification failed -- token was tampered with or signed by a different key
```

## 6. Expired token (real clock-based rejection)

Actual thrown message (demo Scenario 5):
```
Correctly rejected expired token: Authentication failed: Token expired at 2026-08-30T19:10:25Z
```

## 7. ADMIN role, cross-customer access (legitimate privileged access)

Request: `GET /api/v1/banking/CUST1002/balance` with `Authorization: Bearer <SUPPORT_AGENT_7's ADMIN token>`

```json
{"customerId":"CUST1002","accounts":[{"accountNumber":"XXXXXXXX0999","accountType":"SAVINGS","balance":12800.00,"currency":"INR"}]}
```
