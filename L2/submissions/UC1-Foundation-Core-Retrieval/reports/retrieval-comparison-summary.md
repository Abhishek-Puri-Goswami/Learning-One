# Retrieval Comparison Summary — Keyword vs. Semantic vs. Hybrid

Deliverable: "Retrieval comparison summary," per L2 HLD UseCase1's functional scope: "Comparing retrieval approaches (keyword vs semantic vs hybrid)." This is **real, executed output** from `rag-core`'s `Main.java`, run against the actual Secure Bank policy text extracted from `L2/_Reference-Docs/Secure_Bank_policy_Manual_input-docs.pdf` (7 documents, 37 chunks, config = 180 tokens / 40 overlap). Full raw output is captured in `retrieval-demo-run-log.txt` in this folder.

## Result 1: Semantic Search Correctly Beat Keyword Search on a Term-Overlap Trap

**Query:** *"What is the interest rate range for a home loan?"*

| Method | Top result | Why |
|---|---|---|
| Keyword | `fixed_deposit_policy.txt#0` (score 0.600) | The FD policy chunk happens to literally contain more of the query's individual words ("interest," "rate") than the actual home loan chunk does, even though it's about the wrong product entirely. |
| Semantic | `loan_processing_policy.txt#0` (score 0.406) | Correctly ranked first — captures that "home loan" + "interest rate" co-occur meaningfully in the loan policy section. |
| Hybrid | `fixed_deposit_policy.txt#0` (score 0.414), loan chunk 3rd (0.404) | The 0.6/0.4 semantic/keyword blend was not enough to fully overcome keyword's strong (and wrong) signal here — see "Tuning Note" below. |

**Takeaway:** this is a textbook example of exactly the problem RAG's reference guide describes keyword search having (matching literal terms regardless of topical relevance). Semantic search got it right; the default hybrid weighting (0.6/0.4) did not fully correct for it in this specific case, which is a real, actionable tuning finding, not a hypothetical one.

## Result 2: All Three Methods Agreed on Precise, Distinctive Queries

**Query:** *"What is the minimum monthly income required for a personal loan?"* — all three methods correctly surfaced `loan_processing_policy.txt#2` (the exact chunk containing "Minimum monthly n[et income]... Personal Loan: ₹20,000 – ₹30,000") as a top-2 result. When a query uses distinctive domain terms with low collision risk against other sections, keyword, semantic, and hybrid converge — the comparison only diverges when there's term overlap across unrelated topics (Result 1) or when the query is genuinely out-of-corpus (see `hallucination-risk-analysis.md`).

**Query:** *"How do I file a complaint and what is the escalation process?"* — all three again correctly surfaced `customer_grievance_policy.txt#0` first, with scores 0.500 (keyword) / 0.401 (semantic) / 0.440 (hybrid).

## Result 3: Weak Signal on a Legitimately Answerable but Loosely-Worded Query

**Query:** *"What happens if I withdraw my fixed deposit before maturity?"* — semantic scores were noticeably lower across the board (top score 0.199, `fixed_deposit_policy.txt#3`) even though the corpus does contain the answer (`fixed_deposit_policy.txt#2`: "FCNR premature withdrawals follow RBI lock-in conditions..."). Keyword search did better here (0.400) because "withdraw"/"premature"/"fixed deposit" are fairly literal matches. This is the flip side of Result 1 — a case where keyword outperformed the (limited) local embedding model. See `hallucination-risk-analysis.md` for why this specific score range (0.199) is uncomfortably close to a genuinely out-of-scope query's score, which is the central finding of that report.

## Recommendation

Use **hybrid retrieval as the default**, but do not treat the 0.6/0.4 default weighting as final — Result 1 shows it needs tuning (or a reranking step) for cases where keyword overlap is a false-positive trap. This recommendation, and the specific numbers behind it, come directly from the executed run in this folder, not from general RAG literature.
