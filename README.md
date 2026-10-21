# Guardião

An Android app that helps older adults in Portugal recognise scams arriving by WhatsApp and email, explains the risk in plain European Portuguese, and suggests a safe next step.

"Guardião" is a working name.

## What it does today

- **Verificar uma mensagem**: paste, type, share or select any text and get a plain-language assessment.
- **Avisos automáticos** (after the user turns on notification access): reads the text visible in WhatsApp and email notifications, analyses it on the phone, and shows a warning when the risk is high. The warning contains no message content.
- **Ajuda**: what to do if you already paid, simple rules, an optional trusted-person phone number, a practice warning.
- No internet permission, no stored messages, no analytics, no backups.

## What it does not do

- It does not read WhatsApp history, photos, audio or calls.
- It does not read full emails, only the notification preview.
- It does not block, delete or reply to messages, and never contacts anyone by itself.
- It does not guarantee that a message is safe. On a holdout set of new synthetic messages it caught 3 out of 9 scams. See [docs/TESTING_AND_EVALUATION.md](docs/TESTING_AND_EVALUATION.md).

## Project layout

| Path | What |
|---|---|
| `core/` | Detection engine in plain Kotlin. No Android dependency. Unit tests and scenario fixtures |
| `app/` | Android app: Jetpack Compose UI, notification listener, warning notifications |
| `docs/` | Feasibility, privacy, threat model, testing, security review |
| `.github/workflows/android.yml` | Builds and tests everything on GitHub, and produces a debug APK |

## For developers

Requirements: JDK 17 or newer. For the app, also the Android SDK with platform 36.

```
./gradlew :core:test                    # engine tests and evaluation reports (no Android SDK needed)
./gradlew :app:assembleDebug            # debug APK in app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest :app:lintDebug
```

If no Android SDK is configured (no `ANDROID_HOME` and no `sdk.dir` in `local.properties`), the build includes only `core`.

### Extending

- **New phrase rule**: add a `PhraseRule` to `core/.../detect/Lexicon.kt` (patterns in folded form: lower case, no accents), add a test in `RuleTest`, and add or update scenario fixtures.
- **New signal type**: add it to `SignalType`, add wording in `PtPtCatalog` and `EnglishCatalog` (a test fails otherwise), and decide how `RiskPolicy` combines it.
- **New message source**: implement `MessageSourceAdapter`, declare its `SourceCapabilities`, and document it in `docs/FEASIBILITY_AND_ARCHITECTURE.md`.
- **Link reputation or AI**: implement `ThreatIntelligenceProvider` or `InferenceProvider` and pass it to `FraudAnalyzer`. Anything that leaves the phone requires internet permission, which fails `ManifestPolicyTest` until the privacy documentation and consent flow are updated.

## Trying the app on a phone (step by step, no technical knowledge needed)

This installs a **test version**. It is not the final app and it is not on Google Play.

**What you need**: an Android phone (Android 8 or newer) and access to the GitHub page of this project.

1. **Download the app file**
   - On a computer, open https://github.com/gpilatti/repo/actions
   - Click the most recent line with a green tick, named "Build and test".
   - Scroll to the bottom, to the section "Artifacts", and click **guardiao-debug-apk**. A zip file downloads.
   - Open the zip. Inside is a file ending in `.apk`.
2. **Copy it to the phone**: send the `.apk` file to yourself by email or put it on Google Drive, then open it on the phone.
3. **Allow the installation**: Android will warn that it cannot install apps from this source. Tap **Definições**, turn on **Permitir desta fonte**, go back and tap **Instalar**. This is expected for test versions.
   - If Play Protect shows a warning, choose **Instalar mesmo assim** only because this is your own test build.
4. **Open Guardião** and follow the introduction.
5. **To test automatic warnings**: when the introduction opens "Acesso a notificações", find Guardião and turn it on.
   - If the switch is greyed out, this is Android's protection for apps installed outside Google Play. Go to **Definições > Aplicações > Guardião**, tap the **⋮** menu at the top right, tap **Permitir definições restritas**, confirm, and try again.
6. **What to expect**: ask someone to send you a WhatsApp message like "Olá mãe, mudei de número, preciso que me faças uma transferência urgente de 300€". A warning should appear within seconds. If nothing appears, note the phone model and Android version and report it: device testing has not been done yet.

**To remove the test app**: hold the Guardião icon, tap **Desinstalar**. Nothing remains on the phone.

## Documentation

- [Feasibility and architecture](docs/FEASIBILITY_AND_ARCHITECTURE.md)
- [Privacy and data flows](docs/PRIVACY_AND_DATA_FLOWS.md)
- [Threat model and scenarios](docs/THREAT_MODEL_AND_SCENARIOS.md)
- [Testing and evaluation](docs/TESTING_AND_EVALUATION.md)
- [Security review and release blockers](docs/SECURITY_REVIEW.md)
