const pptxgen = require("pptxgenjs");

const NAVY = "0B2545";
const DEEPTEAL = "134E5E";
const TEAL = "1C7293";
const MINT = "58C4B9";
const ICE = "E7F1F5";
const WHITE = "FFFFFF";
const SLATE = "3C4A5C";
const AMBER = "E0A458";

const pres = new pptxgen();
pres.layout = "LAYOUT_WIDE"; // 13.3 x 7.5
pres.author = "RetailCo Gen-AI Submission";
pres.title = "Final Integrated Banking RAG System";

const FONT_HEAD = "Cambria";
const FONT_BODY = "Calibri";

function bgSlide(dark) {
  const s = pres.addSlide();
  s.background = { color: dark ? NAVY : WHITE };
  return s;
}

function footer(s, label, dark) {
  s.addText(label, {
    x: 0.5, y: 7.12, w: 8, h: 0.3,
    fontFace: FONT_BODY, fontSize: 10, color: dark ? "8FA3BF" : "8A95A5",
    margin: 0,
  });
  s.addText("L2 / UC5", {
    x: 11.8, y: 7.12, w: 1.2, h: 0.3,
    fontFace: FONT_BODY, fontSize: 10, color: dark ? "8FA3BF" : "8A95A5",
    align: "right", margin: 0,
  });
}

// ---------- Slide 1: Title ----------
{
  const s = bgSlide(true);
  s.addShape(pres.ShapeType.ellipse, { x: 9.6, y: -2.2, w: 7, h: 7, fill: { color: DEEPTEAL, transparency: 55 }, line: { type: "none" } });
  s.addShape(pres.ShapeType.ellipse, { x: 11.3, y: 3.6, w: 4.2, h: 4.2, fill: { color: MINT, transparency: 75 }, line: { type: "none" } });

  s.addText("RETAILCO  •  GEN-AI USE CASE SUBMISSION  —  LEVEL 2", {
    x: 0.7, y: 1.15, w: 10, h: 0.4, isTextBox: true,
    fontFace: FONT_BODY, fontSize: 13, color: MINT, charSpacing: 2, bold: true, margin: 0,
  });
  s.addText("Final Integrated Banking\nRAG Support System", {
    x: 0.7, y: 1.75, w: 10.5, h: 2.3, isTextBox: true,
    fontFace: FONT_HEAD, fontSize: 44, color: WHITE, bold: true, margin: 0, lineSpacingMultiple: 1.05,
  });
  s.addText("Five use cases, one composed system: retrieval, guardrails, secure live data, observability, and integration — verified by real, running code, not description alone.", {
    x: 0.7, y: 4.15, w: 8.6, h: 1.0, isTextBox: true,
    fontFace: FONT_BODY, fontSize: 15, color: ICE, margin: 0, lineSpacingMultiple: 1.2,
  });

  const chips = ["UC1 Foundation", "UC2 RAG Assistant", "UC3 Secure Data", "UC4 Observability", "UC5 Integration"];
  let cx = 0.7;
  chips.forEach((c, i) => {
    const w = 0.42 + c.length * 0.092;
    s.addShape(pres.ShapeType.roundRect, { x: cx, y: 5.55, w, h: 0.46, rectRadius: 0.08, fill: { color: i === 4 ? MINT : "1A3A5C" }, line: { type: "none" } });
    s.addText(c, { x: cx, y: 5.55, w, h: 0.46, isTextBox: true, align: "center", valign: "middle", fontFace: FONT_BODY, fontSize: 11.5, bold: true, color: i === 4 ? NAVY : ICE, margin: 0 });
    cx += w + 0.16;
  });

  s.addText("Java 17  +  Spring Boot 3.3.4  +  Spring Security  +  React", {
    x: 0.7, y: 6.55, w: 8, h: 0.35, isTextBox: true,
    fontFace: FONT_BODY, fontSize: 12, color: "7E93AD", margin: 0,
  });
  s.addText("August 2026", { x: 11.6, y: 6.9, w: 1.4, h: 0.3, isTextBox: true, align: "right", fontFace: FONT_BODY, fontSize: 10, color: "5E7392", margin: 0 });
}

// ---------- Slide 2: Executive summary (journey) ----------
{
  const s = bgSlide(false);
  s.addText("The Five-Use-Case Journey", { x: 0.6, y: 0.45, w: 11, h: 0.7, isTextBox: true, fontFace: FONT_HEAD, fontSize: 30, bold: true, color: NAVY, margin: 0 });
  s.addText("Each stage independently verified before the next composed on top of it.", { x: 0.6, y: 1.08, w: 11, h: 0.4, isTextBox: true, fontFace: FONT_BODY, fontSize: 13.5, color: SLATE, margin: 0 });

  const steps = [
    { n: "01", t: "Foundation", d: "Chunking, hashing-trick embeddings, hybrid semantic + keyword search over the policy corpus.", uc: "UC1" },
    { n: "02", t: "RAG Assistant", d: "Prompt template, injection & unsafe-advice guardrails, citation-grounded extractive generation, tracing.", uc: "UC2" },
    { n: "03", t: "Secure Data", d: "Real HMAC-SHA256 JWTs, per-request authorization, PII masking on every live account field.", uc: "UC3" },
    { n: "04", t: "Observability", d: "LRU+TTL caching, latency/cost/token metrics, and a data-driven fix to the retrieval threshold.", uc: "UC4" },
    { n: "05", t: "Integration", d: "One entry point routes intent to RAG or secure tools — the same verified classes, now composed.", uc: "UC5" },
  ];
  const colW = 2.28, gap = 0.14, startX = 0.6, y = 1.75, h = 4.9;
  steps.forEach((st, i) => {
    const x = startX + i * (colW + gap);
    const isLast = i === 4;
    s.addShape(pres.ShapeType.roundRect, { x, y, w: colW, h, rectRadius: 0.09, fill: { color: isLast ? DEEPTEAL : ICE }, line: { type: "none" }, shadow: { type: "outer", color: "1B2A41", opacity: 0.18, blur: 6, offset: 2, angle: 90 } });
    s.addText(st.n, { x: x + 0.18, y: y + 0.22, w: colW - 0.36, h: 0.5, isTextBox: true, fontFace: FONT_HEAD, fontSize: 24, bold: true, color: isLast ? MINT : TEAL, margin: 0 });
    s.addText(st.uc, { x: x + 0.18, y: y + 0.68, w: colW - 0.36, h: 0.3, isTextBox: true, fontFace: FONT_BODY, fontSize: 10.5, bold: true, color: isLast ? "9FD8CE" : "5E7392", charSpacing: 1, margin: 0 });
    s.addText(st.t, { x: x + 0.18, y: y + 1.0, w: colW - 0.36, h: 0.5, isTextBox: true, fontFace: FONT_HEAD, fontSize: 16.5, bold: true, color: isLast ? WHITE : NAVY, margin: 0 });
    s.addText(st.d, { x: x + 0.18, y: y + 1.55, w: colW - 0.36, h: h - 1.75, isTextBox: true, fontFace: FONT_BODY, fontSize: 11, color: isLast ? ICE : SLATE, margin: 0, lineSpacingMultiple: 1.22 });
  });
  footer(s, "RetailCo Gen-AI Submission — L2 Final Integrated System");
}

// ---------- Slide 3: Architecture ----------
{
  const s = bgSlide(false);
  s.addText("System Architecture", { x: 0.6, y: 0.42, w: 8, h: 0.65, isTextBox: true, fontFace: FONT_HEAD, fontSize: 30, bold: true, color: NAVY, margin: 0 });
  s.addText("One controller, one intent classifier, two verified paths.", { x: 0.6, y: 1.02, w: 9, h: 0.4, isTextBox: true, fontFace: FONT_BODY, fontSize: 13.5, color: SLATE, margin: 0 });

  // Customer -> Controller -> Classifier
  function box(x, y, w, h, label, sub, fill, tcolor, subcolor) {
    s.addShape(pres.ShapeType.roundRect, { x, y, w, h, rectRadius: 0.08, fill: { color: fill }, line: { type: "none" }, shadow: { type: "outer", color: "1B2A41", opacity: 0.15, blur: 5, offset: 2, angle: 90 } });
    if (sub) {
      s.addText(label, { x: x + 0.12, y: y + 0.1, w: w - 0.24, h: 0.32, isTextBox: true, align: "center", valign: "bottom", fontFace: FONT_BODY, fontSize: 12.5, bold: true, color: tcolor, margin: 0 });
      s.addText(sub, { x: x + 0.12, y: y + 0.44, w: w - 0.24, h: h - 0.54, isTextBox: true, align: "center", valign: "top", fontFace: FONT_BODY, fontSize: 9, color: subcolor, margin: 0, lineSpacingMultiple: 1.1 });
    } else {
      s.addText(label, { x: x + 0.12, y, w: w - 0.24, h, isTextBox: true, align: "center", valign: "middle", fontFace: FONT_BODY, fontSize: 12.5, bold: true, color: tcolor, margin: 0 });
    }
  }

  box(0.6, 1.75, 1.9, 0.85, "Customer", "", TEAL, WHITE);
  box(2.85, 1.75, 2.7, 0.85, "SupportController", "POST /api/v1/support/ask", NAVY, WHITE, "9FD8CE");
  box(5.9, 1.75, 2.5, 0.85, "IntentClassifier", "UC3 — regex routing", DEEPTEAL, WHITE, "9FD8CE");

  // connectors
  s.addShape(pres.ShapeType.rect, { x: 2.5, y: 2.1, w: 0.35, h: 0.04, fill: { color: "9AA7BC" }, line: { type: "none" } });
  s.addShape(pres.ShapeType.rect, { x: 5.55, y: 2.1, w: 0.35, h: 0.04, fill: { color: "9AA7BC" }, line: { type: "none" } });

  // Two branches
  box(6.1, 3.15, 2.9, 1.0, "Policy Question", "ObservableRagAssistant (UC4) → RagAssistant (UC2) → HybridSearcher (UC1)", MINT, NAVY, NAVY);
  box(9.3, 3.15, 3.1, 1.0, "Live Account Data", "BankingToolService (UC3) → JwtService → PiiMasking → BankingDataStore", AMBER, NAVY, NAVY);

  s.addShape(pres.ShapeType.line, { x: 7.0, y: 2.6, w: 0, h: 0.55, line: { color: "9AA7BC", width: 1.5, dashType: "dash" } });
  s.addShape(pres.ShapeType.line, { x: 9.4, y: 2.6, w: 1.4, h: 0.55, line: { color: "9AA7BC", width: 1.5, dashType: "dash", beginArrowType: "none" } });

  box(6.9, 4.75, 5.5, 0.6, "Unified response — PolicyAnswer / LiveDataAnswer / AccessDenied / Ambiguous", "", ICE, NAVY);

  s.addText([
    { text: "Design principle: ", options: { bold: true, color: NAVY } },
    { text: "IntegratedBankingAssistant contains zero new business rules. It is a router and response-shape unifier over classes already verified in UC1–UC4 — so composing them adds an integration risk to test, not a new implementation to trust.", options: { color: SLATE } },
  ], { x: 0.6, y: 5.55, w: 12.1, h: 1.15, isTextBox: true, fontFace: FONT_BODY, fontSize: 13, margin: 0, lineSpacingMultiple: 1.25 });

  footer(s, "Full sequence + component Mermaid diagrams: diagrams/architecture-diagrams.md");
}

// ---------- Slide 4: Guardrails & Security ----------
{
  const s = bgSlide(true);
  s.addText("Guardrails & Security, Verified Under Integration", { x: 0.6, y: 0.45, w: 11.5, h: 0.7, isTextBox: true, fontFace: FONT_HEAD, fontSize: 27, bold: true, color: WHITE, margin: 0 });
  s.addText("Every protection below fired correctly inside the composed 9-turn session — not just in isolated UC2/UC3 tests.", { x: 0.6, y: 1.12, w: 11.5, h: 0.4, isTextBox: true, fontFace: FONT_BODY, fontSize: 13, color: "9FB4CC", margin: 0 });

  const cards = [
    { t: "Authentication", d: "Real HMAC-SHA256 JWTs (javax.crypto.Mac). Forged and expired tokens cryptographically rejected — verified independently in Python.", n: "UC3" },
    { t: "Authorization", d: "Per-request check: token subject must match requested customer, or hold ADMIN role. Cross-customer and mismatched-account requests denied.", n: "UC3" },
    { t: "PII Masking", d: "Account numbers, mobile numbers, email, and government ID masked before leaving BankingToolService — never seen raw by the controller.", n: "UC3" },
    { t: "Prompt-Injection Guard", d: "Blocks instruction-override / role-hijack attempts before any retrieval cost is spent — confirmed via empty retrieved_chunks in the trace log.", n: "UC2" },
    { t: "Unsafe-Advice Guard", d: "Blocks investment/financial-advice requests regardless of retrieval score — closes a gap UC1 found where such a query slipped past a threshold-only check.", n: "UC2" },
    { t: "Retrieval Confidence Gate", d: "Similarity threshold (0.15) + score-margin guardrail (0.012, UC4-corrected) blocks low-confidence answers instead of guessing.", n: "UC1/UC4" },
  ];
  const cw = 3.85, ch = 1.75, gx = 0.25, gy = 0.25, sx = 0.6, sy = 1.85;
  cards.forEach((c, i) => {
    const col = i % 3, row = Math.floor(i / 3);
    const x = sx + col * (cw + gx), y = sy + row * (ch + gy);
    s.addShape(pres.ShapeType.roundRect, { x, y, w: cw, h: ch, rectRadius: 0.08, fill: { color: "142F4E" }, line: { type: "none" } });
    s.addShape(pres.ShapeType.roundRect, { x: x + 0.2, y: y + 0.2, w: 0.62, h: 0.3, rectRadius: 0.15, fill: { color: MINT }, line: { type: "none" } });
    s.addText(c.n, { x: x + 0.2, y: y + 0.2, w: 0.62, h: 0.3, isTextBox: true, align: "center", valign: "middle", fontFace: FONT_BODY, fontSize: 9, bold: true, color: NAVY, margin: 0 });
    s.addText(c.t, { x: x + 0.2, y: y + 0.58, w: cw - 0.4, h: 0.35, isTextBox: true, fontFace: FONT_HEAD, fontSize: 14.5, bold: true, color: WHITE, margin: 0 });
    s.addText(c.d, { x: x + 0.2, y: y + 0.95, w: cw - 0.4, h: ch - 1.05, isTextBox: true, fontFace: FONT_BODY, fontSize: 10.5, color: "C7D3E3", margin: 0, lineSpacingMultiple: 1.18 });
  });
  footer(s, "Verified in SelfTests.testCrossCustomerLiveDataRequestIsDenied and related integrated tests", true);
}

// ---------- Slide 5: Threshold tuning finding ----------
{
  const s = bgSlide(false);
  s.addText("A Real Finding: Tuning the Retrieval Guardrail", { x: 0.6, y: 0.45, w: 11.5, h: 0.7, isTextBox: true, fontFace: FONT_HEAD, fontSize: 27, bold: true, color: NAVY, margin: 0 });
  s.addText("UC4 ran an evidence-based sweep over 6 labeled corpus queries — not a guess.", { x: 0.6, y: 1.1, w: 11, h: 0.4, isTextBox: true, fontFace: FONT_BODY, fontSize: 13.5, color: SLATE, margin: 0 });

  const chartData = [
    {
      name: "Correctly answered",
      labels: ["0.000", "0.005", "0.010", "0.015", "0.020", "0.030", "0.050", "0.080"],
      values: [4, 4, 4, 4, 3, 3, 2, 1],
    },
    {
      name: "Correctly blocked",
      labels: ["0.000", "0.005", "0.010", "0.015", "0.020", "0.030", "0.050", "0.080"],
      values: [0, 1, 2, 2, 2, 2, 2, 2],
    },
    {
      name: "Errors (blocked or answered wrongly)",
      labels: ["0.000", "0.005", "0.010", "0.015", "0.020", "0.030", "0.050", "0.080"],
      values: [2, 1, 0, 0, 1, 1, 4, 3],
    },
  ];

  s.addChart(pres.ChartType.bar, chartData, {
    x: 0.6, y: 1.7, w: 7.6, h: 4.55,
    barDir: "col",
    chartColors: [MINT, TEAL, "C0392B"],
    showTitle: true, title: "Outcome counts across margin thresholds (n=6 labeled queries)",
    titleFontSize: 12.5, titleColor: NAVY, titleFontFace: FONT_BODY,
    showValue: false,
    catAxisLabelColor: SLATE, catAxisLabelFontSize: 9.5, catAxisTitle: "Score margin",
    showCatAxisTitle: true, catAxisTitleFontSize: 10.5, catAxisTitleColor: SLATE,
    valAxisLabelColor: SLATE, valAxisLabelFontSize: 9.5,
    valGridLine: { color: "E3E8EF", size: 1 },
    catGridLine: { style: "none" },
    showLegend: true, legendPos: "b", legendFontSize: 10, legendColor: SLATE,
    barGapWidthPct: 25,
  });

  s.addShape(pres.ShapeType.roundRect, { x: 8.55, y: 1.7, w: 4.15, h: 4.55, rectRadius: 0.09, fill: { color: NAVY }, line: { type: "none" } });
  s.addText("0.012", { x: 8.75, y: 1.95, w: 3.75, h: 0.9, isTextBox: true, fontFace: FONT_HEAD, fontSize: 46, bold: true, color: MINT, margin: 0 });
  s.addText("new default margin threshold", { x: 8.75, y: 2.7, w: 3.75, h: 0.4, isTextBox: true, fontFace: FONT_BODY, fontSize: 11.5, color: ICE, margin: 0 });
  s.addShape(pres.ShapeType.line, { x: 8.75, y: 3.25, w: 3.55, h: 0, line: { color: "2D4A6B", width: 1 } });
  s.addText([
    { text: "0.010–0.015 ", options: { bold: true, color: MINT } },
    { text: "is the zero-error band on this sample — down from UC2's original ", options: { color: ICE } },
    { text: "0.030", options: { bold: true, color: AMBER } },
    { text: ", which needlessly blocked 1 of 6 valid answers.", options: { color: ICE } },
  ], { x: 8.75, y: 3.4, w: 3.75, h: 1.5, isTextBox: true, fontFace: FONT_BODY, fontSize: 12, margin: 0, lineSpacingMultiple: 1.25 });
  s.addText("2 of the 6 ground-truth labels were themselves wrong at first — caught by manually re-tokenizing the raw corpus, not assumed correct.", {
    x: 8.75, y: 5.1, w: 3.75, h: 1.05, isTextBox: true, fontFace: FONT_BODY, fontSize: 10.5, italic: true, color: "9FB4CC", margin: 0, lineSpacingMultiple: 1.2,
  });

  footer(s, "ThresholdTuningExperiment.java — L2/UC4; carried into UC5's IntegratedBankingAssistant wiring");
}

// ---------- Slide 6: Performance & Cost ----------
{
  const s = bgSlide(false);
  s.addText("Performance & Cost — Real Integrated-Session Numbers", { x: 0.6, y: 0.45, w: 11.8, h: 0.7, isTextBox: true, fontFace: FONT_HEAD, fontSize: 26, bold: true, color: NAVY, margin: 0 });
  s.addText("From the actual 9-turn mixed session run — reports/integrated-demo-run-log.txt", { x: 0.6, y: 1.08, w: 11, h: 0.4, isTextBox: true, fontFace: FONT_BODY, fontSize: 13, color: SLATE, margin: 0 });

  const stats = [
    { v: "0.25", l: "cache hit rate", s: "1 hit / 3 miss, TTL+LRU" },
    { v: "8 ms", l: "avg latency", s: "p95 = 30 ms" },
    { v: "676", l: "total tokens", s: "2 answered queries" },
    { v: "$0.162", l: "session cost", s: "2 answered queries" },
  ];
  const sw = 2.85, sgap = 0.2, sx0 = 0.6, sy0 = 1.75, sh = 1.55;
  stats.forEach((st, i) => {
    const x = sx0 + i * (sw + sgap);
    s.addShape(pres.ShapeType.roundRect, { x, y: sy0, w: sw, h: sh, rectRadius: 0.08, fill: { color: ICE }, line: { type: "none" } });
    s.addText(st.v, { x: x + 0.2, y: sy0 + 0.15, w: sw - 0.4, h: 0.75, isTextBox: true, fontFace: FONT_HEAD, fontSize: 30, bold: true, color: TEAL, margin: 0 });
    s.addText(st.l, { x: x + 0.2, y: sy0 + 0.88, w: sw - 0.4, h: 0.3, isTextBox: true, fontFace: FONT_BODY, fontSize: 12, bold: true, color: NAVY, margin: 0 });
    s.addText(st.s, { x: x + 0.2, y: sy0 + 1.16, w: sw - 0.4, h: 0.3, isTextBox: true, fontFace: FONT_BODY, fontSize: 9.5, color: SLATE, margin: 0 });
  });

  s.addText("Only policy-question turns are metered — by design", { x: 0.6, y: 3.65, w: 11.8, h: 0.4, isTextBox: true, fontFace: FONT_HEAD, fontSize: 15.5, bold: true, color: NAVY, margin: 0 });
  s.addText("The 4 policy-question turns flow through ObservableRagAssistant and are metered. The 5 live-data/ambiguous turns never touch the cache or metrics pipeline — live account data must never be cached or billed as a cacheable RAG answer. Confirmed by SelfTests.testMetricsAccumulateAcrossMixedSessionTraffic.", {
    x: 0.6, y: 4.05, w: 7.3, h: 1.5, isTextBox: true, fontFace: FONT_BODY, fontSize: 12.5, color: SLATE, margin: 0, lineSpacingMultiple: 1.25,
  });

  s.addShape(pres.ShapeType.roundRect, { x: 8.3, y: 3.65, w: 4.4, h: 2.65, rectRadius: 0.09, fill: { color: DEEPTEAL }, line: { type: "none" } });
  s.addText("Illustrative monthly cost projection", { x: 8.55, y: 3.85, w: 3.9, h: 0.4, isTextBox: true, fontFace: FONT_BODY, fontSize: 11.5, bold: true, color: "9FD8CE", margin: 0 });
  s.addText("~$1,215 / month", { x: 8.55, y: 4.25, w: 3.9, h: 0.65, isTextBox: true, fontFace: FONT_HEAD, fontSize: 27, bold: true, color: WHITE, margin: 0 });
  s.addText("500 answered queries/day × 30 days, at this session's $0.081/query average", { x: 8.55, y: 4.9, w: 3.9, h: 0.65, isTextBox: true, fontFace: FONT_BODY, fontSize: 10.5, color: ICE, margin: 0, lineSpacingMultiple: 1.2 });
  s.addText("Lower than UC4's own isolated ~$2,535/mo projection — same code, different sampled traffic. Sample-size sensitivity UC4's own cost document already flagged.", {
    x: 8.55, y: 5.65, w: 3.9, h: 0.6, isTextBox: true, fontFace: FONT_BODY, fontSize: 9.5, italic: true, color: "8FA8C4", margin: 0, lineSpacingMultiple: 1.15,
  });

  footer(s, "performance/performance-observability-summary.md");
}

// ---------- Slide 7: Closing / disclosures / next steps ----------
{
  const s = bgSlide(true);
  s.addText("Honest About What's Verified — and What's Next", { x: 0.6, y: 0.5, w: 11.8, h: 0.7, isTextBox: true, fontFace: FONT_HEAD, fontSize: 28, bold: true, color: WHITE, margin: 0 });

  s.addShape(pres.ShapeType.roundRect, { x: 0.6, y: 1.5, w: 5.9, h: 4.9, rectRadius: 0.09, fill: { color: "142F4E" }, line: { type: "none" } });
  s.addText("Actually run, not just written", { x: 0.85, y: 1.72, w: 5.4, h: 0.4, isTextBox: true, fontFace: FONT_HEAD, fontSize: 16, bold: true, color: MINT, margin: 0 });
  const verified = [
    "banking-support-core compiled and run: 14/14 SelfTests pass, full 9-turn session executed",
    "Pure-JDK design (no Maven-blocked libraries) chosen specifically so this could be real, not asserted",
    "HS256 JWT signing/verification independently re-checked in Python",
    "Threshold tuning is a real sweep over real retrieval, with a self-caught labeling error fixed and disclosed",
  ];
  s.addText(verified.map((t, i) => ({ text: t, options: { bullet: { code: "2713" }, color: "DCE6F2", breakLine: i < verified.length - 1 } })), {
    x: 0.85, y: 2.2, w: 5.4, h: 4.05, isTextBox: true, fontFace: FONT_BODY, fontSize: 12.5, margin: 0, paraSpaceAfter: 10, lineSpacingMultiple: 1.2,
  });

  s.addShape(pres.ShapeType.roundRect, { x: 6.75, y: 1.5, w: 5.9, h: 4.9, rectRadius: 0.09, fill: { color: "142F4E" }, line: { type: "none" } });
  s.addText("Disclosed limitations, tracked not hidden", { x: 7.0, y: 1.72, w: 5.4, h: 0.4, isTextBox: true, fontFace: FONT_HEAD, fontSize: 16, bold: true, color: AMBER, margin: 0 });
  const limits = [
    "Spring Boot service layer not compile-verified — Maven Central blocked in this sandbox",
    "ExtractiveStubLlmClient is not a real generative model — the single most consequential caveat",
    "0.012 margin is small-sample evidence, not production-final",
    "ContextOptimizer built in UC4 but not yet wired into prompt construction",
    "No real login flow — DevTokenController is dev-only",
  ];
  s.addText(limits.map((t, i) => ({ text: t, options: { bullet: { code: "2022" }, color: "DCE6F2", breakLine: i < limits.length - 1 } })), {
    x: 7.0, y: 2.2, w: 5.4, h: 4.05, isTextBox: true, fontFace: FONT_BODY, fontSize: 12.5, margin: 0, paraSpaceAfter: 10, lineSpacingMultiple: 1.2,
  });

  footer(s, "README.md — reproduction steps and full deliverables checklist", true);
}

pres.writeFile({ fileName: "/tmp/uc10_build/presentation/L2-UC5-Final-Presentation.pptx" }).then(() => {
  console.log("done");
});
