# Retry Demonstration for Failed Schema — L1 UC5

Deliverable: "Retry demonstration for failed schema." Per the use case's required output shape (`Test name / Scenario / Expected result / Code snippet` — see `edge-case-catalog.md`), AI-generated test-case descriptions were requested as structured JSON first (before being hand-transcribed into the Java test files), so they could be schema-validated automatically. This mirrors the retry-vs-re-prompt pattern already established in L1/UC2.

## Target Schema

```json
{
  "type": "object",
  "required": ["test_name", "scenario", "expected_result", "code_snippet_ref"],
  "properties": {
    "test_name": { "type": "string" },
    "scenario": { "type": "string" },
    "expected_result": { "type": "string" },
    "code_snippet_ref": { "type": "string" }
  }
}
```

## First-Pass Output (failed validation)

Requesting the out-of-stock test case description produced:

```json
{
  "test": "checkout_requestedQuantityExceedsAvailableStock_throwsOutOfStockBeforePayment",
  "scenario": "Cart wants 5 units, only 2 in stock",
  "expected": "OutOfStockException thrown"
}
```

Validator output:
```
ERROR: required property 'test_name' is missing (found 'test' instead)
ERROR: required property 'expected_result' is missing (found 'expected' instead)
ERROR: required property 'code_snippet_ref' is missing
```

## Retry (same prompt, no feedback)

Re-running the identical prompt produced a **different** set of wrong field names on a second attempt (`testName` in camelCase instead of `test_name` in snake_case) — closer, but still failing schema validation. This matches the L1/UC2 finding: blind retry is non-deterministic and doesn't reliably converge on structural correctness.

## Re-Prompt (validator feedback injected)

```
Your previous JSON failed schema validation with these errors:
- required property 'test_name' is missing (found 'test' instead)
- required property 'expected_result' is missing (found 'expected' instead)
- required property 'code_snippet_ref' is missing

Regenerate using EXACTLY these field names: test_name, scenario, expected_result, code_snippet_ref.
```

Result — validated on the first retry with feedback:
```json
{
  "test_name": "checkout_requestedQuantityExceedsAvailableStock_throwsOutOfStockBeforePayment",
  "scenario": "Cart wants 5 units of a product, but only 2 are currently in stock at checkout time.",
  "expected_result": "OutOfStockException is thrown (mapped to HTTP 409); payment gateway is never called; no order is saved.",
  "code_snippet_ref": "OrderServiceImplTest.java : checkout_requestedQuantityExceedsAvailableStock_throwsOutOfStockBeforePayment"
}
```

## Conclusion

Consistent with L1/UC2's finding: for structural/schema-conformance failures, **re-prompting with the exact validator error text** reliably fixes the output in one pass, while blind retry does not reliably converge. This is the process actually used to generate every row of `edge-case-catalog.md`'s tables before they were written into the Java test files.
