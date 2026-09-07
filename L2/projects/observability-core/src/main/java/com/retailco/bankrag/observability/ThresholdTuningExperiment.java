package com.retailco.bankrag.observability;

import com.retailco.bankrag.core.HybridSearcher;
import com.retailco.bankrag.core.KeywordSearcher;
import com.retailco.bankrag.core.ScoredChunk;
import com.retailco.bankrag.core.VectorStore;

import java.util.List;

/**
 * Deliverable: L2 HLD UseCase4 Implementation Approach: "Tune similarity
 * threshold." Directly answers the open recommendation from L2/UC2's
 * reports/rag-assistant-evaluation-summary.md's "Tuning Tension" section:
 * the 0.03 margin guardrail correctly blocked one wrong answer (home loan
 * interest rate query) but also incorrectly blocked one correct answer
 * (personal loan income query) in the SAME demo run. That section
 * explicitly handed this exact tuning question to UC4.
 *
 * This is a real, runnable sweep over candidate margin thresholds against a
 * small labeled query set (labels are the actual correct top chunk id for
 * each query, independently confirmed by reading the real corpus text in
 * L2/UC1 and UC2 -- not guessed). For each candidate threshold, it counts:
 *   - correctlyAnswered: top chunk was right AND margin cleared the threshold (query answered, correctly)
 *   - correctlyBlocked: top chunk was WRONG AND margin did NOT clear the threshold (query blocked, correctly)
 *   - incorrectlyBlocked: top chunk was right but margin did NOT clear the threshold (false negative -- a legitimate answer needlessly declined)
 *   - incorrectlyAnswered: top chunk was WRONG but margin cleared the threshold (false positive -- the dangerous case: a wrong answer delivered with false confidence)
 *
 * This lets the threshold be chosen by counting real trade-offs across a
 * query set, rather than by intuition -- while being explicit that 6
 * labeled queries is a small sample or (see reports/) and a real tuning
 * exercise should grow this set substantially before treating its
 * recommendation as final.
 */
public class ThresholdTuningExperiment {

    public record LabeledQuery(String query, String expectedCorrectChunkId) {
    }

    public record ThresholdResult(double threshold, int correctlyAnswered, int correctlyBlocked,
                                   int incorrectlyBlocked, int incorrectlyAnswered) {
        public double falsePositiveRate(int total) {
            return total == 0 ? 0 : (double) incorrectlyAnswered / total;
        }

        public double falseNegativeRate(int total) {
            return total == 0 ? 0 : (double) incorrectlyBlocked / total;
        }
    }

    private final VectorStore vectorStore;
    private final double semanticWeight;
    private final double keywordWeight;
    private final double similarityThreshold;

    public ThresholdTuningExperiment(VectorStore vectorStore, double semanticWeight, double keywordWeight,
                                      double similarityThreshold) {
        this.vectorStore = vectorStore;
        this.semanticWeight = semanticWeight;
        this.keywordWeight = keywordWeight;
        this.similarityThreshold = similarityThreshold;
    }

    /**
     * The labeled query set. First two entries are the exact pair that
     * produced L2/UC2's "Tuning Tension" finding; the rest are additional
     * real corpus queries added to broaden the sample beyond that one
     * pair, all independently checkable against rag-core's corpus/*.txt.
     */
    public static List<LabeledQuery> defaultLabeledQueries() {
        return List.of(
                new LabeledQuery("What is the interest rate range for a home loan?", "loan_processing_policy.txt#0"),
                new LabeledQuery("What is the minimum monthly income required for a personal loan?", "loan_processing_policy.txt#2"),
                new LabeledQuery("How do I file a complaint and what is the escalation process?", "customer_grievance_policy.txt#0"),
                new LabeledQuery("What happens if I withdraw my fixed deposit before maturity?", "fixed_deposit_policy.txt#2"),
                // NOTE: both labels below were corrected after the first
                // real run of this experiment: the initial labels (#0 for
                // each) assumed the answer would be near the start of its
                // document, but inspecting the actual corpus text showed
                // chunk #0 of kyc_onboarding_policy.txt and
                // card_issuance_fraud_policy.txt are both generic manual
                // front-matter/section-header boilerplate, not the answer
                // content. Verified by manually walking the real chunk
                // boundaries (chunk_size=180, overlap=40) against the raw
                // corpus text -- see reports/observability-demo-run-log.txt's
                // accompanying analysis in observability/observability-metrics-report.md
                // for the walkthrough. This is disclosed rather than quietly
                // fixed, because it is itself a real finding: a hand-labeled
                // "ground truth" is only as good as the person labeling it,
                // and this one was wrong on the first attempt.
                new LabeledQuery("What documents are required for KYC verification?", "kyc_onboarding_policy.txt#4"),
                new LabeledQuery("What should I do if my debit card is lost or stolen?", "card_issuance_fraud_policy.txt#2")
        );
    }

    public record QueryOutcome(String query, String expectedChunkId, String actualTopChunkId,
                                boolean topChunkCorrect, double topScore, double margin) {
    }

    public List<QueryOutcome> runQueries(List<LabeledQuery> queries) {
        HybridSearcher searcher = new HybridSearcher(vectorStore, new KeywordSearcher(), semanticWeight, keywordWeight);
        return queries.stream().map(lq -> {
            List<ScoredChunk> results = searcher.search(lq.query(), 3, 0.0);
            if (results.isEmpty()) {
                return new QueryOutcome(lq.query(), lq.expectedCorrectChunkId(), "(none)", false, 0.0, 0.0);
            }
            String actualTop = results.get(0).chunk().id();
            double topScore = results.get(0).score();
            double margin = results.size() >= 2 ? results.get(0).score() - results.get(1).score() : topScore;
            boolean correct = actualTop.equals(lq.expectedCorrectChunkId());
            return new QueryOutcome(lq.query(), lq.expectedCorrectChunkId(), actualTop, correct, topScore, margin);
        }).toList();
    }

    public List<ThresholdResult> sweep(List<QueryOutcome> outcomes, double[] candidateMargins) {
        return java.util.Arrays.stream(candidateMargins)
                .mapToObj(threshold -> evaluateThreshold(outcomes, threshold))
                .toList();
    }

    private ThresholdResult evaluateThreshold(List<QueryOutcome> outcomes, double marginThreshold) {
        int correctlyAnswered = 0, correctlyBlocked = 0, incorrectlyBlocked = 0, incorrectlyAnswered = 0;
        for (QueryOutcome o : outcomes) {
            boolean wouldPassGuardrail = o.topScore() >= similarityThreshold && o.margin() >= marginThreshold;
            if (o.topChunkCorrect() && wouldPassGuardrail) correctlyAnswered++;
            else if (!o.topChunkCorrect() && !wouldPassGuardrail) correctlyBlocked++;
            else if (o.topChunkCorrect() && !wouldPassGuardrail) incorrectlyBlocked++;
            else incorrectlyAnswered++;
        }
        return new ThresholdResult(marginThreshold, correctlyAnswered, correctlyBlocked, incorrectlyBlocked, incorrectlyAnswered);
    }
}
