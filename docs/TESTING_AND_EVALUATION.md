# Testing and evaluation

## How to run

| What | Command | Needs |
|---|---|---|
| Engine tests and evaluation | `./gradlew :core:test` | JDK 17 or newer |
| App unit tests, lint, debug APK | `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` | JDK 17 and the Android SDK (`ANDROID_HOME`) |
| Everything, automatically | Push to GitHub; see `.github/workflows/android.yml` | Nothing locally |

Reports after `:core:test`:

- `core/build/reports/evaluation.md`: metrics on the tuning corpus.
- `core/build/reports/holdout.md`: metrics on the holdout corpus.
- `core/build/reports/tests/test/index.html`: test results.

In GitHub Actions, both evaluation reports appear in the run summary, and the debug APK is attached to the run as an artifact.

## What is tested

| Area | Test class | Examples |
|---|---|---|
| Every phrase rule | `RuleTest` | urgency, threats, secrecy, new number, family claims, money, unusual payment, credentials (with negation), remote access, lures |
| Links | `LinkTest` | official domains not flagged, lookalikes, `brand@evil` trick, punycode, IP links, shorteners, bare domains, malformed links, link text mismatch |
| Risk policy | `PolicyTest` | single weak tactics stay low; money from a known contact without pressure; classic critical combinations; AI alone cannot raise risk; truncated content lowers confidence; "no warning" never claims safety |
| AI layer | `InferenceTest` | prompt injection cannot downgrade; ungrounded evidence discarded; unknown types discarded; confidence capped; timeout; network failure; redaction and size limit |
| Link reputation | `ThreatIntelTest` | listed host is critical; only host names sent; failures and timeouts degrade |
| Privacy | `PrivacyTest` | `toString` redaction; logger receives codes only; alert payload has no content; malformed payloads rejected; explanations contain no message text; no logging or network calls in engine code |
| Ingestion and alerts | `AlertAndIngestionTest` | de-duplication and its bounds; alert threshold; unsupported apps ignored; group summaries ignored; newest message used; hidden previews marked redacted; email previews marked truncated; shared text bounded |
| Explanations | `ExplanationTest` | every signal and action has text in pt-PT and English; texts are short; no promise of safety; never tells the user to call a number from the message |
| Scenarios | `ScenarioTest` | 57 labelled cases must land in their expected range with expected signals |
| App manifest and text | `ManifestPolicyTest` (app) | no internet permission; no SMS, contacts, accessibility or other broad permissions; backups disabled; no "totally protected" wording; no logging in app code |

Total: 75 engine tests plus 5 app tests.

## Not tested yet

- **On a real phone**: the notification listener with real WhatsApp and email notifications; Android 15 OTP redaction; battery managers; the onboarding flow; screen readers (TalkBack); large font settings. These need a device or emulator. The GitHub workflow builds the APK but does not run an emulator.
- **UI tests**: no Compose UI or instrumentation tests yet. Accessibility semantics (headings, live regions, text labels with every icon) are implemented but not verified with TalkBack.
- **A real crash** with the privacy-safe `toString` in place.

## Decision policy

- **HIGH and CRITICAL** require a combination of tactics, or one tactic that is close to unique to fraud: asking for a code or PIN, asking to install remote-access software, paying with gift cards or crypto, a link that imitates a bank. These produce an automatic notification.
- **CAUTION** covers single notable signs, such as a new-number claim or a suspicious link. Shown only when the user checks a message, never as an automatic notification. Wording recommends confirming, not accusing.
- **NO_WARNING** is shown as "Não encontrámos sinais de alerta. Isto não garante que a mensagem seja verdadeira."
- Urgency, money vocabulary, a bank's name, or a family greeting alone never produce a warning.
- A familiar sender name never lowers risk. A saved contact can lower a plain money request (no urgency, no other tactic) from CAUTION to NO_WARNING.

The policy rules are listed, with identifiers, in `core/src/main/kotlin/pt/guardiao/core/policy/RiskPolicy.kt`. Every assessment records the rules that decided it.

## Results

Positive means "an automatic alert would be shown" (HIGH or CRITICAL). **All data is synthetic.** These numbers say nothing about real-world performance.

### Tuning corpus (57 cases, rules were adjusted using these)

| | Count |
|---|---|
| Scam cases | 40 |
| Benign controls | 14 |
| Ambiguous (excluded from metrics) | 3 |
| True positives | 37 |
| False negatives | 3 (S03-02, D-05, D-07, all by design rated CAUTION) |
| False positives | 0 |

Precision 1.00, recall 0.93, false-positive rate 0.00.

These numbers are inflated: the rules were written and adjusted while looking at these cases. The test suite enforces them only as a guard against regressions (no benign control may alert; recall may not drop below 0.90).

### Holdout corpus (16 cases, written after tuning and not used to adjust rules)

| | Count |
|---|---|
| Scam cases | 9 |
| Benign controls | 7 |
| True positives | 3 |
| False negatives | 6 |
| False positives | 0 |

**Recall 0.33**, false-positive rate 0.00.

Missed cases, and why:

| Case | Pattern | Why it was missed |
|---|---|---|
| H-01 | New number, "preciso de um favor" | Wording "este número é o meu novo" not covered |
| H-02 | Grandchild asks to pay a bill today | "pagues uma fatura" not covered as a money request |
| H-05 | Job scam (paid likes) | No rule for task or job scams |
| H-07 | Fake operator technician asks to read out a code | Verb "leia" not covered |
| H-08 | Netflix lookalike link without `https://` | Bare-domain extraction does not recognise the reserved `.example` ending used in the fixture; a real `.com` would have been caught. Partly a fixture artefact |
| H-09 | English "Mum my phone is broken… can you transfer" | Rated CAUTION; "can you transfer" without an object not covered |

**Interpretation**: phrase rules are precise but brittle. They catch the wording they were written for and miss paraphrases. This is the main technical risk of the product and the strongest argument for adding a semantic layer (an on-device model). It also shows that the false-positive side is in good shape on synthetic data.

Rule for the holdout: if a rule is changed because of a holdout case, that case moves to the tuning corpus and a new holdout case is written.

## Next steps for evaluation

1. Grow the corpus with paraphrases written by several people, not by the rule author. Target: at least 300 cases, balanced across scenarios, with at least as many benign as scam messages.
2. Collect real scam texts from public official alerts (PJ, CTT, CGD, AT pages), anonymised, with their source.
3. Measure an on-device model on the holdout before and after combining it with rules.
4. Run moderated usability sessions with older adults, measuring whether they understand the warning and choose a safe action.
