# Testing

## Philosophy

Prefer fast JVM unit tests for ViewModels, mappers, DAOs, and repositories. Keep fake implementations of `GenerationRepository` / preferences in tests so UI logic stays hermetic. Instrumentation / Compose UI tests are optional follow-ups.

## Commands

From the repo root (JDK 17+, Android SDK Platform 36):

```bash
# All debug unit tests
./gradlew testDebugUnitTest

# App lint
./gradlew :app:lintDebug

# Debug APK
./gradlew :app:assembleDebug
```

Windows:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleDebug
```

## What is covered today

| Area | Examples |
|------|----------|
| Splash / onboarding / settings | Routing, DataStore-backed preferences, clear gallery |
| Camera / preview | Capture events, permission permanent denial, continue nav |
| Processing / style | Start job, completed→result, retry, failure copy |
| Result | Load/missing, favourite toggle, share/save/nav effects |
| Gallery | Favourites filter, delete confirmation |
| Room | Result + job DAO behaviours (Robolectric) |
| Network | DTO serialization + status/result mappers |
| Model / common | Style catalog, time-of-day, `Result` helpers |

## Manual checklists

- Camera hardware/emulator: [`CAMERA_TESTING.md`](CAMERA_TESTING.md)
- Remote backend contract: [`BACKEND_API.md`](BACKEND_API.md)

## CI

`.github/workflows/ci.yml` runs on pushes and pull requests to `main`:

1. Unit tests (`testDebugUnitTest`)
2. App lint (`:app:lintDebug`)
3. Assemble debug APK
4. Upload test/lint reports and the APK as artifacts

No secrets are required for the default (fake-generator) path.
